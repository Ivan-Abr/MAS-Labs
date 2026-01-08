package iz2.agents

import iz2.graph.Graph
import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.Behaviour
import jade.lang.acl.ACLMessage

class ClientAgent : Agent() {
    private var coordinatorAID: AID? = null
    private var graph: Graph? = null
    private var start: Int = 0
    private var dest: Int = 0


    override fun setup() {
        val args = arguments
        if (args != null && args.isNotEmpty()) {
            if (args[0] is Graph) graph = args[0] as Graph
        }
        coordinatorAID = AID("coordinator", AID.ISLOCALNAME)


        addBehaviour(object : Behaviour() {
            private var finished = false


            override fun onStart() {
                val a = arguments ?: run { finished = true; return }
                if (a.size < 3) { finished = true; return }
                start = (a[1] as? Int) ?: run { finished = true; return }
                dest = (a[2] as? Int) ?: run { finished = true; return }


// register client position in shared graph for GUI
                graph?.let { g ->
                    synchronized(g) {
                        g.clientStates[this@ClientAgent.name] = Graph.ClientState(start, false)
                        g.notifyChange()
                    }
                }


                val req = ACLMessage(ACLMessage.REQUEST)
                req.addReceiver(coordinatorAID)
                req.content = "request;${this@ClientAgent.name};$start;$dest"
                this@ClientAgent.send(req)
                agentLog("${this@ClientAgent.name}: request sent (start=$start dest=$dest)")
            }


            override fun action() {
                if (finished) { block(); return }
                val msg = this@ClientAgent.receive()
                if (msg == null) { block(); return }


                when {
                    msg.performative == ACLMessage.REFUSE -> {
                        agentLog("${this@ClientAgent.name}: coordinator refused - no taxi available")
                        try { Thread.sleep(1000) }
                        catch (e: InterruptedException) {
                            e.printStackTrace()
                            finished = true
                            this@ClientAgent.doDelete()
                        }
                        val retry = ACLMessage(ACLMessage.REQUEST)
                        retry.addReceiver(coordinatorAID)
                        retry.content = "request;${this@ClientAgent.name};$start;$dest"
                        this@ClientAgent.send(retry)
                    }


                    msg.performative == ACLMessage.INFORM && msg.content.startsWith("assigned;") -> {
                        val parts = msg.content.split(";")
                        val assignedTaxi = parts.getOrNull(1)
                        val dist = parts.getOrNull(3)
                        agentLog("${this@ClientAgent.name}: assigned taxi $assignedTaxi (dist=$dist)")
                    }


                    msg.performative == ACLMessage.INFORM && msg.content.startsWith("arrived;") -> {
                        agentLog("${this@ClientAgent.name}: taxi arrived -> terminating client")
                        finished = true
                        this@ClientAgent.doDelete()
                    }
                }
            }


            override fun done(): Boolean = finished
        })
    }


    override fun takeDown() {
        agentLog("${'$'}{myAgent.name}: takeDown() called, cleaning up client agent.")
        graph?.let { g ->
            synchronized(g) {
                g.clientStates.remove(this@ClientAgent.name)
                g.notifyChange()
            }
        }
        coordinatorAID?.let {
            val msg = ACLMessage(ACLMessage.INFORM)
            msg.addReceiver(it)
            msg.content = "client_done;${this@ClientAgent.name}"
            this@ClientAgent.send(msg)
        }
    }

    private fun agentLog(msg: String) {
        graph?.addLog(msg)
        println(msg)
    }
}