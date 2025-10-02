package LR1P1

import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.domain.AMSService
import jade.domain.FIPAAgentManagement.AMSAgentDescription
import jade.domain.FIPAAgentManagement.SearchConstraints
import jade.lang.acl.ACLMessage

class AMain: Agent() {
    override fun setup() {
        println("Hello, Mr. ${aid.localName}")
        addBehaviour(object : CyclicBehaviour(this) // Поведение агента исполняемое в цикле
        {
            override fun action() {
                val msg = receive()
                if (msg != null) {
                    println(" – " + myAgent.localName + " received: " + msg.content)
                } //Вывод на экран локального имени агента и полученного сообщения
                block()
                //Блокируем поведение, пока в очереди сообщений агента не появится хотя бы одно сообщение
            }
        })
        var agents: Array<AMSAgentDescription?> = emptyArray()
        try {
            val constraints = SearchConstraints()
            constraints.maxResults = -1
            agents = AMSService.search(this, AMSAgentDescription(), constraints)
        } catch (e: Exception) {
            println("problem searching agent ${e.message}")
            e.printStackTrace()
        }

        for (i in 0..4) {
            val agentId = agents[i]!!.name
            val msg = ACLMessage(ACLMessage.INFORM)
            msg.addReceiver(agentId)
            msg.language = "English"
            msg.content = "Ping"
            send(msg)
        }
    }
}