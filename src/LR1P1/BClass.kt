package LR1P1

import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.lang.acl.ACLMessage

class BClass : Agent() {
    override fun setup() {
        println("Привет! агент " + aid.name + " готов.")
        addBehaviour(object : CyclicBehaviour(this) {
            override fun action() {
                val msg = receive()
                if (msg != null) {
                    println(
                        (" – " +
                            myAgent.localName +
                            " received: "
                            + msg.content)
                    )
                    //Вывод на экран локального имени агента и полученного сообщения
                    val reply = msg.createReply()
                    reply.performative = ACLMessage.INFORM
                    reply.content = "Pong"
                    //Содержимое сообщения
                    send(reply) //отправляем сообщения
                }
                block()
            }
        })
    }
}