package LR2KT.seller


import LR2KT.ontology.BookTradingOntology
import LR2KT.ontology.Costs
import LR2KT.ontology.Sell
import jade.content.ContentElementList
import jade.content.lang.Codec
import jade.content.lang.sl.SLCodec
import jade.content.onto.Ontology
import jade.content.onto.OntologyException
import jade.content.onto.basic.Action
import jade.core.Agent
import jade.core.behaviours.TickerBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.FailureException
import jade.domain.FIPAAgentManagement.NotUnderstoodException
import jade.domain.FIPAAgentManagement.RefuseException
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import jade.proto.ContractNetResponder
import java.util.Date
import kotlin.math.roundToInt

class BookSellerAgent : Agent() {
    // The catalogue of books available for sale
    private val catalogue = HashMap<Any?, Any?>()

    // The GUI to interact with the user
    private var myGui: BookSellerGui? = null

    /** The following parts, where the SLCodec and BookTradingOntology are
     * registered, are explained in section 5.1.3.4 page 88 of the book.
     */
    private val codec: Codec = SLCodec()
    private val ontology: Ontology = BookTradingOntology.instance

    /**
     * Agent initializations
     */
    override fun setup() {
// Printout a welcome message
        println("Seller-agent " + aid.name + " is ready.")

        contentManager.registerLanguage(codec)
        contentManager.registerOntology(ontology)

        // Create and show the GUI
        myGui = BookSellerGuiImpl()
        myGui!!.setAgent(this)
        myGui!!.show()

        // Add the behaviour serving calls for price from buyer agents
        addBehaviour(CallForOfferServer())

        // Add the behaviour serving purchase requests from buyer agents
//addBehaviour(new PurchaseOrderServer());
        /** This piece of code, to register services with the DF, is explained
         * in the book in section 4.4.2.1, page 73
         */
// Register the book-selling service in the yellow pages
        val dfd = DFAgentDescription()
        dfd.name = aid
        val sd = ServiceDescription()
        sd.type = "Book-selling"
        sd.name = "$localName-Book-selling"
        dfd.addServices(sd)
        try {
            DFService.register(this, dfd)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
    }

    /**
     * Agent clean-up
     */
    override fun takeDown() {
// Dispose the GUI if it is there
        if (myGui != null) {
            myGui!!.dispose()
        }

        // Printout a dismissal message
        println("Seller-agent " + aid.name + "terminating.")

        /** This piece of code, to deregister with the DF, is explained
         * in the book in section 4.4.2.1, page 73
         */
// Deregister from the yellow pages
        try {
            DFService.deregister(this)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
    }

    /**
     * This method is called by the GUI when the user inserts a new
     * book for sale
     * @param title The title of the book for sale
     * @param initPrice The initial price
     * @param minPrice The minimum price
     * @param deadline The deadline by which to sell the book
     */
    fun putForSale(title: String?, initPrice: Int, minPrice: Int, deadline: Date) {
        addBehaviour(PriceManager(this, title, initPrice, minPrice, deadline))
    }

    private inner class PriceManager(
        a: Agent?,
        private val title: String?,
        private val initPrice: Int,
        mp: Int,
        d: Date
    ) : TickerBehaviour(a, 5000) {
        private val minPrice = 0
        var currentPrice: Int
            private set
        private val deltaP: Int
        private val initTime: Long
        private val deadline: Long
        private val deltaT: Long

        init {
            currentPrice = initPrice
            deltaP = initPrice - mp
            deadline = d.time
            initTime = System.currentTimeMillis()
            deltaT = (if ((deadline - initTime) > 0) (deadline - initTime) else 60000)
        }

        override fun onStart() {
// Insert the book in the catalogue of books available for sale
            catalogue.put(title, this)
            super.onStart()
        }

        public override fun onTick() {
            val currentTime = System.currentTimeMillis()
            if (currentTime > deadline) {
// Deadline expired
                myGui!!.notifyUser("Cannot sell book $title")
                catalogue.remove(title)
                stop()
            } else {
// Compute the current price
                val elapsedTime = currentTime - initTime
                // System.out.println("initPrice"+initPrice+"deltaP"+deltaP+"elapsedTime"+elapsedTime+"deltaT"+deltaT+"currentPrice"+currentPrice+"");
                currentPrice = (initPrice - 1.0 * deltaP * (1.0 * elapsedTime / deltaT)).roundToInt().toInt()
            }
        }
    }

    private inner class CallForOfferServer : ContractNetResponder(
        this@BookSellerAgent, MessageTemplate.and(
            MessageTemplate.MatchOntology(ontology.name), MessageTemplate.MatchPerformative(
                ACLMessage.CFP
            )
        )
    ) {
        var price: Int = 0

        @Throws(RefuseException::class, FailureException::class, NotUnderstoodException::class)
        override fun handleCfp(cfp: ACLMessage): ACLMessage {
// CFP Message received. Process it
            val reply = cfp.createReply()
            //System.out.println(cfp);
            /* if (cfp.getPerformative() != ACLMessage.CFP) {
reply.setPerformative(ACLMessage.FAILURE);
System.out.println(myAgent.getLocalName()+"REINIT"+cfp);
reinit();
}
else*/
            run {
                try {
                    val cm = myAgent.contentManager
                    val act = cm.extractContent(cfp) as Action
                    val sellAction = act.action as Sell
                    val book = sellAction.book
                    myGui!!.notifyUser("Received Proposal to buy " + book!!.title)
                    val pm = catalogue[book.title] as PriceManager?
                    if (pm != null) {
// The requested book is available for sale
                        reply.performative = ACLMessage.PROPOSE
                        val cel = ContentElementList()
                        cel.add(act)
                        val costs = Costs(null, null)
                        costs.book = book
                        price = pm.currentPrice
                        costs.price = price
                        cel.add(costs)
                        cm.fillContent(reply, cel)
                    } else {
// The requested book is NOT available for sale.
                        reply.performative = ACLMessage.REFUSE
                    }
                } catch (oe: OntologyException) {
                    oe.printStackTrace()
                    reply.performative = ACLMessage.NOT_UNDERSTOOD
                } catch (ce: Codec.CodecException) {
                    ce.printStackTrace()
                    reply.performative = ACLMessage.NOT_UNDERSTOOD
                } catch (e: Exception) {
                    e.printStackTrace()
                    reply.performative = ACLMessage.NOT_UNDERSTOOD
                }
            }
            //System.out.println(myAgent.getLocalName()+"RX"+cfp+"\nTX"+reply+"\n\n");
            myGui!!.notifyUser(if (reply.performative == ACLMessage.PROPOSE) "Sent Proposal to sell at $price" else "Refused Proposal as the book is not for sale")
            return reply
        }

        @Throws(FailureException::class)
        override fun handleAcceptProposal(cfp: ACLMessage?, propose: ACLMessage?, accept: ACLMessage): ACLMessage {
            val inform = accept.createReply()
            inform.performative = ACLMessage.INFORM
            inform.content = price.toString()
            myGui!!.notifyUser("Sent Inform at price $price")
            return inform
        }
    }
}