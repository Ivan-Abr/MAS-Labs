package iz

import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import java.util.concurrent.atomic.AtomicInteger

class ProjectAgent : Agent() {
    private var requiredProgrammers: AtomicInteger = AtomicInteger(1)
    private var requiredExp: Int = 0
    private var cost = 0

    override fun setup() {
        println("Project Agent ${aid.name} starting")
        val args = arguments
        if (args!=null && args.size >=3 ) {
            try {
                requiredProgrammers = AtomicInteger((args[0] as String).toInt())
                requiredExp = (args[1] as String).toInt()
                cost = (args[2] as String).toInt()
            } catch (e: Exception) {
                println("Invalid startup args for $localName using defaults")
            }
        } else {
            println("Not all args are present for $localName, using default args")
        }

        val dfd = DFAgentDescription()
        dfd.name = aid
        val sd = ServiceDescription()
        sd.type = "project-offering"
        sd.name = localName
        dfd.addServices(sd)
        try {
            DFService.register(this, dfd)
            println("Project ${localName} registered in DF: required=${requiredProgrammers.get()},requiredExp=$requiredExp, cost=$cost")
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }

        addBehaviour(OfferRequestServer())

        addBehaviour(AllocationServer())
    }

    override fun takeDown() {
        try {
            DFService.deregister(this)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
        println("Project-agent ${aid.name} terminating.")
    }


    private inner class OfferRequestServer : CyclicBehaviour() {
        override fun action() {
            val mt = MessageTemplate.MatchPerformative(ACLMessage.CFP)
            val msg = myAgent.receive(mt)
            println("Project receiving message:$msg")
            if (msg!=null) {
                val reply = msg.createReply()
                try {
                    val progExp = msg.content.toInt()
                    val slots = requiredProgrammers.get()
                    if (progExp >= requiredExp && slots > 0) {
                        reply.performative = ACLMessage.PROPOSE
                        reply.content = cost.toString()
                    } else {
                        reply.performative = ACLMessage.REFUSE
                        reply.content = if (slots <= 0) "No slots" else "Insufficient experience"
                    }
                } catch (e: Exception) {
                    reply.performative = ACLMessage.REFUSE
                    reply.content = "Bad request"
                }
                println("Sending reply: $reply")
                myAgent.send(reply)
            } else block()
        }
    }

    private inner class AllocationServer : CyclicBehaviour() {
        override fun action() {
            val mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            val msg = myAgent.receive(mt)
            if (msg != null) {
                val reply = msg.createReply()
                val slotsNow = requiredProgrammers.get()
                if (slotsNow > 0) {
                    val newSlots =requiredProgrammers.decrementAndGet()
                    reply.performative = ACLMessage.INFORM
                    println("Project $localName: allocated slot to ${msg.sender.name}. Remaining slots = $newSlots")
                    myAgent.send(reply)
                    if (newSlots == 0) {
                        println("Project $localName is fully staffed. Deregister from DF.")
                        try {
                            DFService.deregister(myAgent)
                        } catch (fe: FIPAException) {
                            fe.printStackTrace()
                        }
                    }
                } else {
                    reply.performative = ACLMessage.FAILURE
                    reply.content = "No slots"
                    myAgent.send(reply)
                }
            } else block()
        }
    }
}
