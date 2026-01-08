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
                        graph?.let { g ->
                            synchronized(g) {
                                g.taxiStates[sender.name] = Graph.TaxiState(loc, true, false, time)
                                g.notifyChange()
                            }
                        }
                        println("Coordinator: registered taxi ${sender.name} at $loc t = $time")
                    }
                    "update" -> {
                        val loc = parts.getOrNull(1)?.toIntOrNull()
                        val avail = parts.getOrNull(2)?.toBoolean()
                        taxis[sender]?.let {
                            if (loc != null) it.location = loc
                            if (avail != null) it.available = avail
                        }
                        graph?.let { g ->
                            synchronized(g) {
                                val st = g.taxiStates.getOrPut(sender.name) { Graph.TaxiState(0, true, false, 1.0) }
                                if (loc != null) st.location = loc
                                if (avail != null) st.available = avail
                                g.notifyChange()
                            }
                        }
                    }
                    "done" -> {
                        val loc = parts.getOrNull(1)?.toIntOrNull()
                        taxis[sender]?.let {
                            if (loc != null) it.location = loc
                            it.available = true
                        }
                        graph?.let { g ->
                            synchronized(g) {
                                val st = g.taxiStates.getOrPut(sender.name) { Graph.TaxiState(0, true, false, 1.0) }
                                if (loc != null) st.location = loc
                                st.available = true
                                st.withClient = false
                                g.notifyChange()
                            }
                        }
                        println("Coordinator: taxi ${sender.name} done, now at ${taxis[sender]}")
                    }
                }
            }

            private fun handleRequest(msg: ACLMessage) {
                val parts = msg.content.split(";")
                if (parts.size < 4) return
                val clientAID = parts[1]
                val start = parts[2].toIntOrNull() ?: return
                val dest = parts[3].toIntOrNull() ?: return

                graph?.let { g ->
                    synchronized(g) {
                        g.clientStates[clientAID] = Graph.ClientState(start, false)
                        g.notifyChange()
                    }
                }
                var bestTaxi: AID? = null
                var bestDist = Double.POSITIVE_INFINITY
                var bestPath: List<Int> = emptyList()

                val g = graph ?: return

                synchronized(g) {
                    for ((aid, info) in taxis) {
                        if (!info.available) continue
                        val (path, total) = g.shortestPathDijkstra(info.location, start)
                        if (path.isNotEmpty() && total < bestDist) {
                            bestDist = total
                            bestTaxi = aid
                            bestPath = path
                        }
                    }
                }


                if (bestTaxi == null) {
                    val reply = msg.createReply()
                    reply.performative = ACLMessage.REFUSE
                    reply.content = "no_taxi"
                    myAgent.send(reply)
                    return
                }

                val assign = ACLMessage(ACLMessage.REQUEST)
                assign.addReceiver(bestTaxi)
                assign.content = "assign;${msg.sender.name};$start;$dest"
                myAgent.send(assign)

                taxis[bestTaxi]?.available = false
                graph?.let { gg ->
                    synchronized(gg) {
                        gg.taxiStates[bestTaxi.name]?.available = false
                        gg.notifyChange()
                    }
                }
                val reply = msg.createReply()
                reply.performative = ACLMessage.INFORM
                reply.content = "assigned;${bestTaxi.name};distance;$bestDist"
                myAgent.send(reply)


                println("Coordinator: assigned ${bestTaxi.name} to client ${msg.sender.name}, dist=$bestDist")
            }
        }
        )
    }
}