package iz2.agents

import iz2.graph.Graph
import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.lang.acl.ACLMessage

class TaxiAgent : Agent() {
    private var graph: Graph? = null
    private var location: Int = 0
    private var timePerWeight: Double = 1000.0
    private var coordinatorAID: AID? = null

    override fun setup() {
        val args = arguments
        if (args != null && args.isNotEmpty()) {
            try {
                graph = args[0] as Graph
                location = args[1] as Int
                timePerWeight = (args[2] as? Double)?: timePerWeight
            } catch (e: Exception) {e.printStackTrace()}
            coordinatorAID = AID("coordinator", AID.ISLOCALNAME)
            val reg = ACLMessage(ACLMessage.INFORM)
            reg.addReceiver(coordinatorAID)
            reg.addReceiver(coordinatorAID)
            reg.content = "register;$location;$timePerWeight"
            send(reg)

            addBehaviour(object : CyclicBehaviour(this@TaxiAgent) {
                override fun action() {
                    val msg = myAgent.receive()
                    if (msg == null) { block(); return }
                    if (msg.performative == ACLMessage.REQUEST && msg.content.startsWith("assign;")) {
                        handleAssign(msg)
                    }
                }

                private fun handleAssign(msg: ACLMessage) {
// Формат assign;clientAID;start;dest
                    val parts = msg.content.split(";")
                    if (parts.size < 4) return
                    val clientAIDname = parts[1]
                    val start = parts[2].toIntOrNull() ?: return
                    val dest = parts[3].toIntOrNull() ?: return


                    val g = graph ?: return


// 1) едем к клиенту
                    val (pathToClient, distToClient) = synchronized(g) { g.shortestPathDijkstra(location, start) }
                    val travelMs1 = (distToClient * timePerWeight).toLong()
                    println("${myAgent.name}: driving to client $clientAIDname via ${pathToClient.joinToString("->")}, time=${travelMs1}ms")
                    try { Thread.sleep(travelMs1) } catch (_: InterruptedException) {}
                    location = start


// сообщаем координатору, что сейчас заняты
                    val upd = ACLMessage(ACLMessage.INFORM)
                    upd.addReceiver(coordinatorAID)
                    upd.content = "update;$location;false"
                    send(upd)


// 2) везём клиента до пункта назначения
                    val (pathToDest, distToDest) = synchronized(g) { g.shortestPathDijkstra(start, dest) }
                    val travelMs2 = (distToDest * timePerWeight).toLong()
                    println("${myAgent.name}: transporting client $clientAIDname via ${pathToDest.joinToString("->")}, time=${travelMs2}ms")
                    try { Thread.sleep(travelMs2) } catch (_: InterruptedException) {}
                    location = dest


// сообщаем координатору о завершении
                    val done = ACLMessage(ACLMessage.INFORM)
                    done.addReceiver(coordinatorAID)
                    done.content = "done;$location"
                    send(done)


// сообщаем клиенту о завершении
                    val clientAID = AID(clientAIDname, AID.ISLOCALNAME)
                    val fin = ACLMessage(ACLMessage.INFORM)
                    fin.addReceiver(clientAID)
                    fin.content = "arrived;${myAgent.name}"
                    send(fin)
                }
            })
        }
    }
}