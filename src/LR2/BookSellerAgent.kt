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
    private var catalogue: Hashtable<Any?, Any?>? = null
    private var myGui: BookSellerGui? = null
    override fun setup() {
        catalogue = Hashtable<Any?, Any?>()
        myGui = BookSellerGui(this)
        myGui!!.show()
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
        addBehaviour(OfferRequestsServer())
        addBehaviour(PurchaseOrdersServer())
    }

    override fun takeDown() {
        try {
            DFService.deregister(this)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
        myGui!!.dispose()
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
                val title = msg.content
                val reply = msg.createReply()

                val price = catalogue!!.get(title) as Int?
                if (price != null) {
                    reply.performative = ACLMessage.PROPOSE
                    reply.content = price.toString()
                } else {
                    reply.performative = ACLMessage.REFUSE
                    reply.content = "not-available"
                }
                myAgent.send(reply)
            } else {
                block()
            }
        }
    }


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
    }
}