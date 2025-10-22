package LR2T.buyer;

import LR2T.ontology.Book;
import LR2T.ontology.Costs;
import LR2T.ontology.Sell;
import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;

import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;

import jade.proto.ContractNetInitiator;

import java.util.Vector;
import java.util.Date;

import jade.content.*;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.*;
import jade.content.onto.basic.*;
import LR2T.ontology.*;

public class BookBuyerAgent extends Agent {
    // The list of known seller agents
    private Vector sellerAgents = new Vector();

    // The GUI to interact with the user
    private BookBuyerGui myGui;

    /** The following parts, where the SLCodec and BookTradingOntology are
     * registered, are explained in section 5.1.3.4 page 88 of the book.
     **/
    private Codec codec = new SLCodec();
    private Ontology ontology = BookTradingOntology.getInstance();

    /**
     * Agent initializations
     **/
    protected void setup() {

/** The following piece of code is explained in section 5.6.1 pag. 113 of the book.
 * It processes notifications from the external buying system (other modifications
 * also need to be introduced to handle the successful purchase or deadline expiration).
 **/
// Enable O2A Communication
        setEnabledO2ACommunication(true, 0);
// Add the behaviour serving notifications from the external system
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                BookInfo info = (BookInfo) myAgent.getO2AObject();
                if (info != null) {
                    purchase(info.getTitle(), info.getMaxPrice(), info.getDeadline());
                }
                else {
                    block();
                }
            }
        } );

// Printout a welcome message
        System.out.println("Buyer-agent "+getAID().getName()+" is ready.");

        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

// Get names of seller agents as arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; ++i) {
                AID seller = new AID((String) args[i], AID.ISLOCALNAME);
                sellerAgents.addElement(seller);
            }
        }

// Show the GUI to interact with the user
        myGui = new BookBuyerGuiImpl();
        myGui.setAgent(this);
        myGui.show();

/** This piece of code, to search services with the DF, is explained
 * in the book in section 4.4.3, page 74
 **/
// Update the list of seller agents every minute
        addBehaviour(new TickerBehaviour(this, 60000) {
            protected void onTick() {
// Update the list of seller agents
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Book-selling");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    sellerAgents.clear();
                    for (int i = 0; i < result.length; ++i) {
                        sellerAgents.addElement(result[i].getName());
                    }
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        } );
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
        System.out.println("Buyer-agent "+getAID().getName()+"terminated.");
    }

    /**
     * This method is called by the GUI when the user inserts a new
     * book to buy
     * @param title The title of the book to buy
     * @param maxPrice The maximum acceptable price to buy the book
     * @param deadline The deadline by which to buy the book
     **/
    public void purchase(String title, int maxPrice, Date deadline) {
// the following line is in the book at page 62
        addBehaviour(new PurchaseManager(this, title, maxPrice, deadline));
    }

    /**
     * This method is called by the GUI. At the moment it is not implemented.
     **/
    public void setCreditCard(String creditCarNumber) {
    }

    /**
     * Section 4.2.4, Page 62
     **/
    private class PurchaseManager extends TickerBehaviour {
        private String title;
        private int maxPrice;
        private long deadline, initTime, deltaT;

        private PurchaseManager(Agent a, String t, int mp, Date d) {
            super(a, 60000); // tick every minute
            title = t;
            maxPrice = mp;
            deadline = d.getTime();
            initTime = System.currentTimeMillis();
            deltaT = deadline - initTime;
        }

        public void onTick() {
            long currentTime = System.currentTimeMillis();
            if (currentTime > deadline) {
// Deadline expired
                myGui.notifyUser("Cannot buy book "+title);
                stop();
            }
            else {
// Compute the currently acceptable price and start a negotiation
                long elapsedTime = currentTime - initTime;
                int acceptablePrice = (int)Math.round(1.0 * maxPrice * (1.0 * elapsedTime / deltaT));
                myAgent.addBehaviour(new BookNegotiator(title, acceptablePrice, this));
            }
        }
    }

    public ACLMessage cfp = new ACLMessage(ACLMessage.CFP); // variable needed to the ContractNetInitiator constructor

    /**
     * Section 5.4.2 of the book, page 104
     * Inner class BookNegotiator.
     * This is the behaviour reimplemented by using the ContractNetInitiator
     **/
    public class BookNegotiator extends ContractNetInitiator {
        private String title;
        private int maxPrice;
        private PurchaseManager manager;

        public BookNegotiator(String t, int p, PurchaseManager m) {
            super(BookBuyerAgent.this, cfp);
            title = t;
            maxPrice = p;
            manager = m;
            Book book = new Book();
            book.setTitle(title);
            Sell sellAction = new Sell();
            sellAction.setItem(book);
            Action act = new Action(BookBuyerAgent.this.getAID(), sellAction);
            try {
                cfp.setLanguage(codec.getName());
                cfp.setOntology(ontology.getName());
                BookBuyerAgent.this.getContentManager().fillContent(cfp, act);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected Vector prepareCfps(ACLMessage cfp) {
            cfp.clearAllReceiver();
            for (int i = 0; i < sellerAgents.size(); ++i) {
                cfp.addReceiver((AID) sellerAgents.get(i));
            }
            Vector v = new Vector();
            v.add(cfp);
            if (sellerAgents.size() > 0)
                myGui.notifyUser("Sent Call for Proposal to "+sellerAgents.size()+" sellers.");
            return v;
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            ACLMessage bestOffer = null;
            int bestPrice = -1;
            for (int i = 0; i < responses.size(); i++) {
                ACLMessage rsp = (ACLMessage) responses.get(i);
                if (rsp.getPerformative() == ACLMessage.PROPOSE) {
                    try {
                        ContentElementList cel = (ContentElementList)myAgent.getContentManager().extractContent(rsp);
                        int price = ((Costs)cel.get(1)).getPrice();
                        myGui.notifyUser("Received Proposal at "+price+" when maximum acceptable price was "+maxPrice);
                        if (bestOffer == null || price < bestPrice) {
                            bestOffer = rsp;
                            bestPrice = price;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            for (int i = 0; i < responses.size(); i++) {
                ACLMessage rsp = (ACLMessage) responses.get(i);
                ACLMessage accept = rsp.createReply();
                if (rsp == bestOffer) {
                    boolean acceptedProposal = (bestPrice <= maxPrice);
                    accept.setPerformative(acceptedProposal ? ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL);
                    accept.setContent(title);
                    myGui.notifyUser(acceptedProposal ? "sent Accept Proposal" : "sent Reject Proposal");
                } else {
                    accept.setPerformative(ACLMessage.REJECT_PROPOSAL);
                }
//System.out.println(myAgent.getLocalName()+" handleAllResponses.acceptances.add "+accept);
                acceptances.add(accept);
            }
        }

        protected void handleInform(ACLMessage inform) {
// Book successfully purchased
            int price = Integer.parseInt(inform.getContent());
            myGui.notifyUser("Book "+title+" successfully purchased. Price =" + price);
            manager.stop();
        }

    } // End of inner class BookNegotiator
}