package izg

import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.lang.acl.ACLMessage

class ProgrammerAgent : Agent() {
    var skills: Int = 0
    override fun setup() {
        val args = arguments
        if (args != null && args.isNotEmpty()) {
            skills = (args[0] as Int)
            println("$localName has $skills years skills")
        addBehaviour(object : CyclicBehaviour() {
            override fun action() {
                val msg = myAgent.receive()
                if (msg != null) {
                    if (msg.performative == ACLMessage.INFORM &&
                        msg.content.startsWith("PROJECT|")
                    ) {
                        val parts = msg.content.split("|")
                        val salary = parts[1].toInt()
                        val skill = parts[2].toInt()
                        val vacancies = parts[3].toInt()
                        val spec = ProjectSpect(salary, skill, vacancies)
                        println("$localName got project spec: $spec")
                        if (skills >= skill) {
                            val reply = ACLMessage(ACLMessage.PROPOSE)
                            reply.addReceiver(msg.sender)
                            reply.content = "APPLY|$salary|$skills"
                            send(reply)
                            println("${localName}: applied for project with salary=$salary")
                        }
                    } else if (msg.performative == ACLMessage.ACCEPT_PROPOSAL) {
                        println("${localName}: accepted by ${msg.sender.name} -> ${msg.content}")
                    }
                } else {
                    block()
                }
            }
        })
        }
    }
    override fun takeDown() {
        println("${localName}: terminating")
    }
}