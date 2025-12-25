package iz

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

class ProgrammerAgent : Agent() {
    private var experience: Int = 0
    private lateinit var projectAgents: List<AID>

    override fun setup() {
        println("Programmer Agent ${aid.name} starting")
        val args = arguments
        if (args != null && args.size > 0) {
            try {
                experience = (args[0] as String).toInt()
            } catch (e: Exception) {
                println("Invalid startup args for $localName using defaults")
            }
        }else println("Not all args are present for $localName, using default args")
        println("Programmer $localName experience = $experience")

        addBehaviour(object : TickerBehaviour(this, 10000) {
            override fun onTick() {
                println("$localName looking for projects")
                val template = DFAgentDescription()
                val sd = ServiceDescription()
                sd.type = "project-offering"
                template.addServices(sd)
                try {
                    val result = DFService.search(myAgent, template)
                    projectAgents = result.map { it.name as AID }
                    println("$localName found ${projectAgents.size} projects")
                } catch (fe: FIPAException) {
                    fe.printStackTrace()
                    projectAgents = emptyList()
                }
                if (projectAgents.isNotEmpty()) {
                    myAgent.addBehaviour(RequestPerformer())
                }
            }
        })
    }

    override fun takeDown() {
        println("Programmer-agent ${aid.name} terminating.")
    }

    private inner class RequestPerformer : Behaviour() {
        private var bestProject: AID? = null
        private var bestCost = -1
        private var repliesCount = 0
        private var mt: MessageTemplate? =null
        private var step = 0

        override fun action() {
            when (step) {
                0 -> {
                    val cfp = ACLMessage(ACLMessage.CFP)
                    for (proj in projectAgents) {
                        cfp.addReceiver(proj)
                    }
                    cfp.content = experience.toString()
                    cfp.conversationId = "project-allocation"
                    cfp.replyWith = "cfp" + System.currentTimeMillis()
                    myAgent.send(cfp)

                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId("project-allocation"),
                        MessageTemplate.MatchInReplyTo(cfp.replyWith)
                    )
                    step = 1
                }

                1 -> {
                    val reply = myAgent.receive(mt)
                    if (reply != null) {
                        if (reply.performative == ACLMessage.PROPOSE) {
                            try {
                                val cost = reply.content.toInt()
                                if (cost > bestCost) {
                                    bestCost = cost
                                    bestProject = reply.sender
                                }
                            } catch (e: Exception) {}
                        }
                        repliesCount++
                        if (repliesCount >= projectAgents.size) step = 2
                        else block()
                    }
                }

                2 -> {
                    // send ACCEPT_PROPOSAL to best project (if any)
                    if (bestProject != null) {
                        val order = ACLMessage(ACLMessage.ACCEPT_PROPOSAL)
                        order.addReceiver(bestProject)
                        // optionally include programmer info (name/experience)
                        order.content = experience.toString()
                        order.conversationId = "project-allocation"
                        order.replyWith = "order" + System.currentTimeMillis()
                        myAgent.send(order)

                        mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("project-allocation"),
                            MessageTemplate.MatchInReplyTo(order.replyWith)
                        )
                        step = 3
                    } else {
                        println("${localName}: no suitable project proposals received.")
                        step = 4
                    }
                }

                3 -> {
                    // wait for INFORM / FAILURE
                    val reply = myAgent.receive(mt)
                    if (reply != null) {
                        if (reply.performative == ACLMessage.INFORM) {
                            println("${localName}: successfully assigned to project ${reply.sender.name}. Project cost = $bestCost")
                            // programmer can stop (one project only)
                            myAgent.doDelete()
                        } else {
                            println("${localName}: assignment failed by ${reply.sender.name}: ${reply.content}")
                        }
                        step = 4
                    } else {
                        block()
                    }
                }
            }
        }
        override fun done(): Boolean {
            return step == 4
        }
    }
}