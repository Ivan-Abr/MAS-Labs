package LR2

import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.core.behaviours.OneShotBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import java.util.Hashtable


class BookSellerAgent : Agent() {
    // The catalogue of books for sale (maps the title of a book to its price)
    private var catalogue: Hashtable<Any?, Any?>? = null

    // The GUI by means of which the user can add books in the catalogue
    private var myGui: BookSellerGui? = null

    // Put agent initializations here
    override fun setup() {
        // Create the catalogue
        catalogue = Hashtable<Any?, Any?>()

        // Create and show the GUI
        myGui = BookSellerGui(this)
        myGui!!.show()

        // Register the book-selling service in the yellow pages
        val dfd = DFAgentDescription()
        dfd.name = aid
        val sd = ServiceDescription()
        sd.type = "book-selling"
        sd.name = "JADE-book-trading"
        dfd.addServices(sd)
        try {
            DFService.register(this, dfd)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }


        // Add the behaviour serving queries from buyer agents
        addBehaviour(OfferRequestsServer())

        // Add the behaviour serving purchase orders from buyer agents
        addBehaviour(PurchaseOrdersServer())
    }

    // Put agent clean-up operations here
    override fun takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
        // Close the GUI
        myGui!!.dispose()
        // Printout a dismissal message
        println("Seller-agent " + aid.name + " terminating.")
    }

    /**
     * This is invoked by the GUI when the user adds a new book for sale
     */
    fun updateCatalogue(title: String?, price: Int) {
        addBehaviour(object : OneShotBehaviour() {
            override fun action() {
                catalogue!![title] = price
                println("$title inserted into catalogue. Price = $price")
            }
        })
    }

    /**
     * Inner class OfferRequestsServer.
     * This is the behaviour used by Book-seller agents to serve incoming requests
     * for offer from buyer agents.
     * If the requested book is in the local catalogue the seller agent replies
     * with a PROPOSE message specifying the price. Otherwise a REFUSE message is
     * sent back.
     */
    private inner class OfferRequestsServer : CyclicBehaviour() {
        override fun action() {
            val mt = MessageTemplate.MatchPerformative(ACLMessage.CFP)
            val msg = myAgent.receive(mt)
            if (msg != null) {
                // CFP Message received. Process it
                val title = msg.content
                val reply = msg.createReply()

                val price = catalogue!!.get(title) as Int?
                if (price != null) {
                    // The requested book is available for sale. Reply with the price
                    reply.performative = ACLMessage.PROPOSE
                    reply.content = price.toString()
                } else {
                    // The requested book is NOT available for sale.
                    reply.performative = ACLMessage.REFUSE
                    reply.content = "not-available"
                }
                myAgent.send(reply)
            } else {
                block()
            }
        }
    } // End of inner class OfferRequestsServer


    /**
     * Inner class PurchaseOrdersServer.
     * This is the behaviour used by Book-seller agents to serve incoming
     * offer acceptances (i.e. purchase orders) from buyer agents.
     * The seller agent removes the purchased book from its catalogue
     * and replies with an INFORM message to notify the buyer that the
     * purchase has been sucesfully completed.
     */
    private inner class PurchaseOrdersServer : CyclicBehaviour() {
        override fun action() {
            val mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            val msg = myAgent.receive(mt)
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                val title = msg.content
                val reply = msg.createReply()

                val price = catalogue!!.remove(title) as Int?
                if (price != null) {
                    reply.performative = ACLMessage.INFORM
                    println(title + " sold to agent " + msg.sender.name)
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.performative = ACLMessage.FAILURE
                    reply.content = "not-available"
                }
                myAgent.send(reply)
            } else {
                block()
            }
        }
    } // End of inner class OfferRequestsServer
}