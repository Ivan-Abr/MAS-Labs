package LR2

import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.Behaviour
import jade.core.behaviours.TickerBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate


class BookBuyerAgent : Agent() {
    // The title of the book to buy
    private var targetBookTitle: String? = null

    // The list of known seller agents
    private lateinit var sellerAgents: List<AID?>

    // Put agent initializations here
    override fun setup() {
        // Printout a welcome message
        println("Hallo! Buyer-agent " + aid.name + " is ready.")


        // Get the title of the book to buy as a start-up argument
        val args = arguments
        if (args != null && args.size > 0) {
            targetBookTitle = args[0] as String?
            println("Target book is $targetBookTitle")


            // Add a TickerBehaviour that schedules a request to seller agents every minute
            addBehaviour(object : TickerBehaviour(this, 60000) {
                override fun onTick() {
                    println("Trying to buy $targetBookTitle")
                    // Update the list of seller agents
                    val template = DFAgentDescription()
                    val sd = ServiceDescription()
                    sd.type = "book-selling"
                    template.addServices(sd)
                    try {
                        val result = DFService.search(myAgent, template)
                        println("Found the following seller agents:")
                        sellerAgents = result.map { it.name  as AID}
                    } catch (fe: FIPAException) {
                        fe.printStackTrace()
                    }


                    // Perform the request
                    myAgent.addBehaviour(RequestPerformer())
                }
            })
        } else {
            // Make the agent terminate
            println("No target book title specified")
            doDelete()
        }
    }

    // Put agent clean-up operations here
    override fun takeDown() {
        // Printout a dismissal message
        println("Buyer-agent " + aid.name + " terminating.")
    }

    /**
     * Inner class RequestPerformer.
     * This is the behaviour used by Book-buyer agents to request seller
     * agents the target book.
     */
    private inner class RequestPerformer : Behaviour() {
        private var bestSeller: AID? = null // The agent who provides the best offer
        private var bestPrice = 0 // The best offered price
        private var repliesCnt = 0 // The counter of replies from seller agents
        private var mt: MessageTemplate? = null // The template to receive replies
        private var step = 0

        override fun action() {
            when (step) {
                0 -> {
                    // Send the cfp to all sellers
                    val cfp: ACLMessage = ACLMessage(ACLMessage.CFP)
                    var i = 0
                    while (i < sellerAgents.size) {
                        cfp.addReceiver(sellerAgents[i])
                        ++i
                    }
                    cfp.content = targetBookTitle
                    cfp.conversationId = "book-trade"
                    cfp.replyWith = "cfp" + System.currentTimeMillis() // Unique value
                    myAgent.send(cfp)
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId("book-trade"),
                        MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
                    )
                    step = 1
                }

                1 -> {
                    // Receive all proposals/refusals from seller agents
                    val reply: ACLMessage? = myAgent.receive(mt)
                    if (reply != null) {
                        // Reply received
                        if (reply.performative == ACLMessage.PROPOSE) {
                            // This is an offer
                            val price = reply.content.toInt()
                            if (bestSeller == null || price < bestPrice) {
                                // This is the best offer at present
                                bestPrice = price
                                bestSeller = reply.sender
                            }
                        }
                        repliesCnt++
                        if (repliesCnt >= sellerAgents.size) {
                            // We received all replies
                            step = 2
                        }
                    } else {
                        block()
                    }
                }

                2 -> {
                    // Send the purchase order to the seller that provided the best offer
                    val order: ACLMessage = ACLMessage(ACLMessage.ACCEPT_PROPOSAL)
                    order.addReceiver(bestSeller)
                    order.content = targetBookTitle
                    order.conversationId = "book-trade"
                    order.replyWith = "order" + System.currentTimeMillis()
                    myAgent.send(order)
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId("book-trade"),
                        MessageTemplate.MatchInReplyTo(order.replyWith)
                    )
                    step = 3
                }

                3 -> {
                    // Receive the purchase order reply
                    val reply = myAgent.receive(mt)
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.performative == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            println(
                                targetBookTitle + " successfully purchased from agent " + reply.sender.name
                            )
                            println("Price = $bestPrice")
                            myAgent.doDelete()
                        } else {
                            println("Attempt failed: requested book already sold.")
                        }

                        step = 4
                    } else {
                        block()
                    }
                }
            }
        }

        override fun done(): Boolean {
            if (step == 2 && bestSeller == null) {
                println("Attempt failed: $targetBookTitle not available for sale")
            }
            return ((step == 2 && bestSeller == null) || step == 4)
        }
    } // End of inner class RequestPerformer
}