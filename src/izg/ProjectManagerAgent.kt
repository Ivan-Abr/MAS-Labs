package izg

import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.lang.acl.ACLMessage
import java.util.concurrent.atomic.AtomicInteger

class ProjectManagerAgent : Agent() {
    private val id = AtomicInteger(0)

    override fun setup() {
        println("$localName setup")
        addBehaviour(object: CyclicBehaviour() {
            override fun action() {
                val msg = myAgent.receive()
                if (msg != null) {
                    when (msg.performative) {
                        ACLMessage.INFORM -> {
                            val content = msg.content
                            if (content.startsWith("ANNOUNCE|")) {
                                val parts = content.split("|")
                                if (parts.size >= 4) {
                                    val salary = parts[1].toInt()
                                    val skill = parts[2].toInt()
                                    val vacancies = parts[3].toInt()
                                    val spec = ProjectSpect(salary, skill, vacancies)
                                    println("$localName announcing spec: $spec")
                                    broadcastProject(spec)
                                }
                            }
                        }
                        else -> {
                            if (msg.protocol == "APPLY") {
                                println("${localName}: received APPLY from ${msg.sender.name} - ${msg.content}")
                                val reply = msg.createReply()
                                reply.performative = ACLMessage.ACCEPT_PROPOSAL
                                reply.content = "ACCEPTED"
                                myAgent.send(reply)
                            }
                        }
                    }
                } else {
                    block()
                }
            }
        })
    }

    override fun takeDown() {
        super.takeDown()
    }

    private fun broadcastProject(spec: ProjectSpect) {
        val msg = ACLMessage(ACLMessage.INFORM)
        msg.content = "PROJECT|${spec.salary}|${spec.skillDemand}|${spec.vacancies}"
    }
}