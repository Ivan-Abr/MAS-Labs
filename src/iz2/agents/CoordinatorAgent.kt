package iz2.agents

import iz2.graph.Graph
import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.lang.acl.ACLMessage

class CoordinatorAgent : Agent() {
    private val taxis = mutableMapOf<AID, TaxiInfo>()
    private var graph: Graph? = null

    override fun setup() {
        val args = arguments
        if (args != null && args.isNotEmpty() && args[0] is Graph) {
            graph = args[0] as Graph
        }

        addBehaviour(object: CyclicBehaviour(this@CoordinatorAgent) {
            override fun action() {
                val msg = myAgent.receive()
                if (msg == null) {
                    block()
                    return
                }
                when (msg.performative) {
                    ACLMessage.INFORM -> handleInform(msg)
                    ACLMessage.REQUEST -> handleRequest(msg)
                    else -> return
                }
            }

            private fun handleInform(msg: ACLMessage) {
                val parts = msg.content.split(";")
                val sender = msg.sender
                when (parts[0]) {
                    "register" -> {
                        val loc = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val time = parts.getOrNull(2)?.toDoubleOrNull() ?: 1.0
                        taxis[sender] = TaxiInfo(loc, true, time)
                        println("Coordinator: registered taxi ${sender.name} at $loc t = $time")
                    }
                    "update" -> {
                        val loc = parts.getOrNull(1)?.toIntOrNull()
                        val avail = parts.getOrNull(2)?.toBoolean()
                        taxis[sender]?.let {
                            if (loc != null) it.location = loc
                            if (avail != null) it.available = avail
                        }
                    }
                    "done" -> {
                        val loc = parts.getOrNull(1)?.toIntOrNull()
                        taxis[sender]?.let {
                            if (loc != null) it.location = loc
                            it.available = true
                        }
                        println("Coordinator: taxi ${sender.name} done, now at ${taxis[sender]}")
                    }
                }
            }

            private fun handleRequest(msg: jade.lang.acl.ACLMessage) {
// Клиент отправляет REQUEST с содержимым: "request;clientAID;start;dest"
                val parts = msg.content.split(";")
                if (parts.size < 4) return
                val clientAID = parts[1]
                val start = parts[2].toIntOrNull() ?: return
                val dest = parts[3].toIntOrNull() ?: return


// Находим ближайшее доступное такси (по суммарному весу пути до клиента)
                var bestTaxi: jade.core.AID? = null
                var bestDist = Double.POSITIVE_INFINITY
                var bestPath: List<Int> = emptyList()


                val g = graph ?: return


                synchronized(g) {
                    for ((aid, info) in taxis) {
                        if (!info.available) continue
// compute shortest path from taxi location to client start
                        val (path, total) = g.shortestPathDijkstra(info.location, start)
                        if (path.isNotEmpty() && total < bestDist) {
                            bestDist = total
                            bestTaxi = aid
                            bestPath = path
                        }
                    }
                }


                if (bestTaxi == null) {
// никто не свободен — ответ клиенту отказом
                    val reply = msg.createReply()
                    reply.performative = jade.lang.acl.ACLMessage.REFUSE
                    reply.content = "no_taxi"
                    myAgent.send(reply)
                    return
                }


// Назначаем задачу такси: отправляем ASSIGN сообщение
                val assign = jade.lang.acl.ACLMessage(jade.lang.acl.ACLMessage.REQUEST)
                assign.addReceiver(bestTaxi)
                assign.content = "assign;${msg.sender.name};$start;$dest"
                myAgent.send(assign)


// отмечаем такси как занятое
                taxis[bestTaxi]?.available = false


// информируем клиента, кому назначено
                val reply = msg.createReply()
                reply.performative = jade.lang.acl.ACLMessage.INFORM
                reply.content = "assigned;${bestTaxi.name};distance;$bestDist"
                myAgent.send(reply)


                println("Coordinator: assigned ${bestTaxi.name} to client ${msg.sender.name}, dist=$bestDist")
            }
        }
        )
    }
}