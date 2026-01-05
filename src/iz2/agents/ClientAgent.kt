package iz2.agents

import iz2.graph.Graph
import jade.core.Agent
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.domain.df

class ClientAgent : jade.core.Agent() {
    private var coordinatorAID: jade.core.AID? = null
    private var graph: Graph? = null
    override fun setup() {
        val args = arguments
        if (args != null && args.isNotEmpty()) {
            if (args[0] is Graph) graph = args[0] as Graph
        }
        coordinatorAID = jade.core.AID("coordinator", jade.core.AID.ISLOCALNAME)


        addBehaviour(object : jade.core.behaviours.OneShotBehaviour(this@ClientAgent) {
            override fun action() {
// Аргументы: start, dest
                val a = arguments ?: return
                if (a.size < 3) return
                val start = (a[1] as? Int) ?: return
                val dest = (a[2] as? Int) ?: return


// Отправляем запрос координатору
                val req = jade.lang.acl.ACLMessage(jade.lang.acl.ACLMessage.REQUEST)
                req.addReceiver(coordinatorAID)
                req.content = "request;${myAgent.name};$start;$dest"
                send(req)


// ждём ответа от координатора об назначении
                var assignedTaxi: String? = null
                while (true) {
                    val msg = blockingReceive(10000)
                    if (msg == null) {
                        println("${myAgent.name}: timeout waiting for assignment")
                        break
                    }
                    if (msg.performative == jade.lang.acl.ACLMessage.INFORM && msg.content.startsWith("assigned;")) {
                        val parts = msg.content.split(";")
                        assignedTaxi = parts.getOrNull(1)
                        println("${myAgent.name}: assigned taxi $assignedTaxi")
// теперь ждём сообщение от такси о прибытии
                    } else if (msg.performative == jade.lang.acl.ACLMessage.INFORM && msg.content.startsWith("arrived;")) {
                        println("${myAgent.name}: my taxi ${msg.content} arrived. terminating.")
                        break
                    } else if (msg.performative == jade.lang.acl.ACLMessage.REFUSE) {
                        println("${myAgent.name}: coordinator refused - no taxi available")
                        break
                    }
                }


                doDelete() // завершение клиента
            }
        })
    }
}