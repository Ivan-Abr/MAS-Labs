package LR2KT.buyer


import LR2KT.ontology.Book
import LR2KT.ontology.Costs
import LR2KT.ontology.Sell
import jade.content.ContentElementList
import jade.content.lang.Codec
import jade.content.lang.sl.SLCodec
import jade.content.onto.Ontology
import jade.content.onto.basic.Action
import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.core.behaviours.TickerBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.proto.ContractNetInitiator
import java.util.Date
import java.util.Vector
import kotlin.math.roundToInt

class BookBuyerAgent : Agent() {
    // The list of known seller agents
    private val sellerAgents = Vector<Any?>()

    // The GUI to interact with the user
    private var myGui: BookBuyerGui? = null

    /** The following parts, where the SLCodec and BookTradingOntology are
     * registered, are explained in section 5.1.3.4 page 88 of the book.
     */
    private val codec: Codec = SLCodec()
    private val ontology: Ontology = LR2T.ontology.BookTradingOntology.getInstance()
    private lateinit var manager: PurchaseManager
    /**
     * Agent initializations
     */
    override fun setup() {
        /** The following piece of code is explained in section 5.6.1 pag. 113 of the book.
         * It processes notifications from the external buying system (other modifications
         * also need to be introduced to handle the successful purchase or deadline expiration).
         */
// Enable O2A Communication

        setEnabledO2ACommunication(true, 0)
        // Add the behaviour serving notifications from the external system
        addBehaviour(object : CyclicBehaviour(this) {
            override fun action() {
                val info = myAgent.o2AObject as BookInfo?
                if (info != null) {
                    purchase(info.title, info.maxPrice, info.date)
                } else {
                    block()
                }
            }
        })

        // Printout a welcome message
        println("Buyer-agent " + aid.name + " is ready.")

        contentManager.registerLanguage(codec)
        contentManager.registerOntology(ontology)

        // Get names of seller agents as arguments
        val args = arguments
        if (args != null && args.size > 0) {
            for (i in args.indices) {
                val seller = AID(args[i] as String?, false)
                sellerAgents.addElement(seller)
            }
        }

        // Show the GUI to interact with the user
        myGui = BookBuyerGuiImpl()
        myGui!!.setAgent(this)
        myGui!!.show()

        /** This piece of code, to search services with the DF, is explained
         * in the book in section 4.4.3, page 74
         */
// Update the list of seller agents every minute
        addBehaviour(object : TickerBehaviour(this, 10000) {
            override fun onTick() {
// Update the list of seller agents
                val template = DFAgentDescription()
                val sd = ServiceDescription()
                sd.type = "Book-selling"
                template.addServices(sd)
                try {
                    val result = DFService.search(myAgent, template)
                    sellerAgents.clear()
                    for (i in result.indices) {
                        sellerAgents.addElement(result[i]!!.name)
                    }
                } catch (fe: FIPAException) {
                    fe.printStackTrace()
                }
            }
        })
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
        println("Buyer-agent " + aid.name + "terminated.")
    }

    /**
     * This method is called by the GUI when the user inserts a new
     * book to buy
     * @param title The title of the book to buy
     * @param maxPrice The maximum acceptable price to buy the book
     * @param deadline The deadline by which to buy the book
     */
    fun purchase(title: String?, maxPrice: Int, deadline: Date) {
// the following line is in the book at page 62
        addBehaviour(PurchaseManager(this, title, maxPrice, deadline))
    }

    /**
     * This method is called by the GUI. At the moment it is not implemented.
     */
    fun setCreditCard(creditCarNumber: String?) {
    }

    /**
     * Section 4.2.4, Page 62
     */
    private inner class PurchaseManager(a: Agent?, private val title: String?, private val maxPrice: Int, d: Date) :
        TickerBehaviour(a, 60000) {
        private val deadline: Long = d.time
        private val initTime: Long = System.currentTimeMillis()
        private val deltaT: Long = deadline - initTime

        public override fun onTick() {
            val currentTime = System.currentTimeMillis()
            if (currentTime > deadline) {
// Deadline expired
                myGui!!.notifyUser("Cannot buy book $title")
                stop()
            } else {
// Compute the currently acceptable price and start a negotiation
                val elapsedTime = currentTime - initTime
                val acceptablePrice = (1.0 * maxPrice * (1.0 * elapsedTime / deltaT)).roundToInt().toInt()
                myAgent.addBehaviour(BookNegotiator(title, acceptablePrice))
            }
        }
    }

    var cfp: ACLMessage = ACLMessage(ACLMessage.CFP) // variable needed to the ContractNetInitiator constructor

    /**
     * Section 5.4.2 of the book, page 104
     * Inner class BookNegotiator.
     * This is the behaviour reimplemented by using the ContractNetInitiator
     */
    inner class BookNegotiator(
        private val title: String?,
        private val maxPrice: Int,
    ) : ContractNetInitiator(this@BookBuyerAgent, cfp) {
        init {
            val book = Book()
            book.title = title
            val sellAction = Sell()
            sellAction.book = book
            val act = Action(this@BookBuyerAgent.aid, sellAction)
            try {
                cfp.language = codec.name
                cfp.ontology = ontology.name
                this@BookBuyerAgent.contentManager.fillContent(cfp, act)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun prepareCfps(cfp: ACLMessage): Vector<*> {
            cfp.clearAllReceiver()
            for (i in sellerAgents.indices) {
                cfp.addReceiver(sellerAgents[i] as AID?)
            }
            val v = Vector<Any?>()
            v.add(cfp)
            if (sellerAgents.isNotEmpty()) myGui!!.notifyUser("Sent Call for Proposal to " + sellerAgents.size + " sellers.")
            return v
        }

        override fun handleAllResponses(responses: Vector<Any?>, acceptances: Vector<Any?>) {
            var bestOffer: ACLMessage? = null
            var bestPrice = -1
            for (i in responses.indices) {
                val rsp = responses[i] as ACLMessage
                if (rsp.performative == ACLMessage.PROPOSE) {
                    try {
                        val cel = myAgent.contentManager.extractContent(rsp) as ContentElementList
                        val price = (cel.get(1) as Costs).price!!
                        myGui!!.notifyUser("Received Proposal at $price when maximum acceptable price was $maxPrice")
                        if (bestOffer == null || price < bestPrice) {
                            bestOffer = rsp
                            bestPrice = price
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            for (i in responses.indices) {
                val rsp = responses[i] as ACLMessage
                val accept = rsp.createReply()
                if (rsp === bestOffer) {
                    val acceptedProposal = (bestPrice <= maxPrice)
                    accept.performative = if (acceptedProposal) ACLMessage.ACCEPT_PROPOSAL else ACLMessage.REJECT_PROPOSAL
                    accept.content = title
                    myGui!!.notifyUser(if (acceptedProposal) "sent Accept Proposal" else "sent Reject Proposal")
                } else {
                    accept.performative = ACLMessage.REJECT_PROPOSAL
                }
                //System.out.println(myAgent.getLocalName()+" handleAllResponses.acceptances.add "+accept);
                acceptances.add(accept)
            }
        }

        override fun handleInform(inform: ACLMessage) {
// Book successfully purchased
            val price = inform.content.toInt()
            myGui!!.notifyUser("Book $title successfully purchased. Price =$price")
            manager.stop()
        }
    } // End of inner class BookNegotiator
}