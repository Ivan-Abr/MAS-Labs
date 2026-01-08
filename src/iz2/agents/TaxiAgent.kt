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
            graph?.let { g ->
                synchronized(g) {
                    g.taxiStates[this@TaxiAgent.name] = Graph.TaxiState(location, true, false, timePerWeight)
                    g.notifyChange()
                }
            }
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
                    val parts = msg.content.split(";")
                    if (parts.size < 4) return
                    val clientAIDname = parts[1]
                    val start = parts[2].toIntOrNull() ?: return
                    val dest = parts[3].toIntOrNull() ?: return
                    val g = graph ?: return

                    // 1) drive to client along shortest path, updating location step-by-step
                    val (pathToClient, _) = synchronized(g) { g.shortestPathDijkstra(location, start) }
                    for (i in 0 until pathToClient.size - 1) {
                        val a = pathToClient[i]
                        val b = pathToClient[i + 1]
                        val w = synchronized(g) { g.edgeWeight(a, b) ?: 1.0 }
                        val travelMs = (w * timePerWeight).toLong()
                        try { Thread.sleep(travelMs) } catch (_: InterruptedException) {}
                        synchronized(g) {
                            location = b
                            val st = g.taxiStates.getOrPut(this@TaxiAgent.name) { Graph.TaxiState(location, false, false, timePerWeight) }
                            st.location = location
                            st.available = false
                            g.notifyChange()
                        }
                        val upd = jade.lang.acl.ACLMessage(jade.lang.acl.ACLMessage.INFORM)
                        upd.addReceiver(coordinatorAID)
                        upd.content = "update;$location;false"
                        this@TaxiAgent.send(upd)
                    }

                    // arrived to client
                    synchronized(g) {
                        val st = g.taxiStates.getOrPut(this@TaxiAgent.name) { Graph.TaxiState(location, false, false, timePerWeight) }
                        st.withClient = true
                        st.available = false
                        g.clientStates[clientAIDname]?.inTaxi = true
                        g.notifyChange()
                    }

                    // 2) drive to destination with client
                    val (pathToDest, _) = synchronized(g) { g.shortestPathDijkstra(start, dest) }
                    for (i in 0 until pathToDest.size - 1) {
                        val a = pathToDest[i]
                        val b = pathToDest[i + 1]
                        val w = synchronized(g) { g.edgeWeight(a, b) ?: 1.0 }
                        val travelMs = (w * timePerWeight).toLong()
                        try { Thread.sleep(travelMs) } catch (_: InterruptedException) {}
                        synchronized(g) {
                            location = b
                            val st = g.taxiStates.getOrPut(this@TaxiAgent.name) { Graph.TaxiState(location, false, true, timePerWeight) }
                            st.location = location
                            st.withClient = true
                            st.available = false
                            g.clientStates[clientAIDname]?.location = location
                            g.notifyChange()
                        }
                        val upd2 = jade.lang.acl.ACLMessage(jade.lang.acl.ACLMessage.INFORM)
                        upd2.addReceiver(coordinatorAID)
                        upd2.content = "update;$location;false"
                        this@TaxiAgent.send(upd2)
                    }

                    // finished transport
                    synchronized(g) {
                        val st = g.taxiStates.getOrPut(this@TaxiAgent.name) { Graph.TaxiState(location, true, false, timePerWeight) }
                        st.location = location
                        st.available = true
                        st.withClient = false
                        g.clientStates.remove(clientAIDname)
                        g.notifyChange()
                    }


                    val done = jade.lang.acl.ACLMessage(jade.lang.acl.ACLMessage.INFORM)
                    done.addReceiver(coordinatorAID)
                    done.content = "done;$location"
                    this@TaxiAgent.send(done)


                    val clientAID = jade.core.AID(clientAIDname, jade.core.AID.ISLOCALNAME)
                    val fin = jade.lang.acl.ACLMessage(jade.lang.acl.ACLMessage.INFORM)
                    fin.addReceiver(clientAID)
                    fin.content = "arrived;${this@TaxiAgent.name}"
                    this@TaxiAgent.send(fin)
                }
            })
        }
    }
}