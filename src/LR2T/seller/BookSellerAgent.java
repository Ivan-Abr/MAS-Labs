package LR2T.seller;

import LR2T.ontology.Book;
import LR2T.ontology.BookTradingOntology;
import LR2T.ontology.Costs;
import LR2T.ontology.Sell;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.proto.ContractNetResponder;
import jade.content.*;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.*;
import jade.content.onto.basic.*;

import java.util.*;

public class BookSellerAgent extends Agent {
    // The catalogue of books available for sale
    private Map catalogue = new HashMap();

    // The GUI to interact with the user
    private BookSellerGui myGui;

    /** The following parts, where the SLCodec and BookTradingOntology are
     * registered, are explained in section 5.1.3.4 page 88 of the book.
     **/
    private Codec codec = new SLCodec();
    private Ontology ontology = BookTradingOntology.getInstance();

    /**
     * Agent initializations
     **/
    protected void setup() {
// Printout a welcome message
        System.out.println("Seller-agent "+getAID().getName()+" is ready.");

        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

// Create and show the GUI
        myGui = new BookSellerGuiImpl();
        myGui.setAgent(this);
        myGui.show();

// Add the behaviour serving calls for price from buyer agents
        addBehaviour(new CallForOfferServer());

// Add the behaviour serving purchase requests from buyer agents
//addBehaviour(new PurchaseOrderServer());

/** This piece of code, to register services with the DF, is explained
 * in the book in section 4.4.2.1, page 73
 **/
// Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Book-selling");
        sd.setName(getLocalName()+"-Book-selling");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    /**
     * Agent clean-up
     **/
    protected void takeDown() {
// Dispose the GUI if it is there
        if (myGui != null) {
            myGui.dispose();
        }

// Printout a dismissal message
        System.out.println("Seller-agent "+getAID().getName()+"terminating.");

/** This piece of code, to deregister with the DF, is explained
 * in the book in section 4.4.2.1, page 73
 **/
// Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    /**
     * This method is called by the GUI when the user inserts a new
     * book for sale
     * @param title The title of the book for sale
     * @param initPrice The initial price
     * @param minPrice The minimum price
     * @param deadline The deadline by which to sell the book
     **/
    public void putForSale(String title, int initPrice, int minPrice, Date deadline) {
        addBehaviour(new PriceManager(this, title, initPrice, minPrice, deadline));
    }

    private class PriceManager extends TickerBehaviour {
        private String title;
        private int minPrice, currentPrice, initPrice, deltaP;
        private long initTime, deadline, deltaT;

        private PriceManager(Agent a, String t, int ip, int mp, Date d) {
            super(a, 30000); // tick every minute
            title = t;
            initPrice = ip;
            currentPrice = initPrice;
            deltaP = initPrice - mp;
            deadline = d.getTime();
            initTime = System.currentTimeMillis();
            deltaT = ((deadline - initTime) > 0 ? (deadline - initTime) : 60000);
        }

        public void onStart() {
// Insert the book in the catalogue of books available for sale
            catalogue.put(title, this);
            super.onStart();
        }

        public void onTick() {
            long currentTime = System.currentTimeMillis();
            if (currentTime > deadline) {
// Deadline expired
                myGui.notifyUser("Cannot sell book "+title);
                catalogue.remove(title);
                stop();
            }
            else {
// Compute the current price
                long elapsedTime = currentTime - initTime;
// System.out.println("initPrice"+initPrice+"deltaP"+deltaP+"elapsedTime"+elapsedTime+"deltaT"+deltaT+"currentPrice"+currentPrice+"");
                currentPrice = (int)Math.round(initPrice - 1.0 * deltaP * (1.0 * elapsedTime / deltaT));
            }
        }

        public int getCurrentPrice() {
            return currentPrice;
        }
    }

    private class CallForOfferServer extends ContractNetResponder {

        int price;

        CallForOfferServer() {
            super(BookSellerAgent.this, MessageTemplate.and(MessageTemplate.MatchOntology(ontology.getName()),MessageTemplate.MatchPerformative(ACLMessage.CFP)));
        }

        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
// CFP Message received. Process it
            ACLMessage reply = cfp.createReply();
//System.out.println(cfp);
/* if (cfp.getPerformative() != ACLMessage.CFP) {
reply.setPerformative(ACLMessage.FAILURE);
System.out.println(myAgent.getLocalName()+"REINIT"+cfp);
reinit();
}
else*/ {
                try {
                    ContentManager cm = myAgent.getContentManager();
                    Action act = (Action) cm.extractContent(cfp);
                    Sell sellAction = (Sell) act.getAction();
                    Book book = sellAction.getItem();
                    myGui.notifyUser("Received Proposal to buy "+book.getTitle());
                    PriceManager pm = (PriceManager)catalogue.get(book.getTitle());
                    if (pm != null) {
// The requested book is available for sale
                        reply.setPerformative(ACLMessage.PROPOSE);
                        ContentElementList cel = new ContentElementList();
                        cel.add(act);
                        Costs costs = new Costs();
                        costs.setItem(book);
                        price = pm.getCurrentPrice();
                        costs.setPrice(price);
                        cel.add(costs);
                        cm.fillContent(reply, cel);
                    }
                    else {
// The requested book is NOT available for sale.
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                }
                catch (OntologyException oe) {
                    oe.printStackTrace();
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                }
                catch (CodecException ce) {
                    ce.printStackTrace();
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                }
            }
//System.out.println(myAgent.getLocalName()+"RX"+cfp+"\nTX"+reply+"\n\n");
            myGui.notifyUser(reply.getPerformative() == ACLMessage.PROPOSE ? "Sent Proposal to sell at "+ price : "Refused Proposal as the book is not for sale");
            return reply;
        }

        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            ACLMessage inform = accept.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent(Integer.toString(price));
            myGui.notifyUser("Sent Inform at price "+price);
            return inform;
        }

    }

}