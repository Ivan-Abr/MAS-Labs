package iz2

import iz2.agents.ClientAgent
import iz2.agents.CoordinatorAgent
import iz2.agents.TaxiAgent
import iz2.graph.Graph
import iz2.graph.GraphFrame
import javax.swing.SwingUtilities

class Main
fun startJadeMainContainer(graph: Graph) {
    try {
        val rt = jade.core.Runtime.instance()
        val p = jade.core.ProfileImpl()
        p.setParameter(jade.core.Profile.MAIN_HOST, "127.0.0.1")
        val container = rt.createMainContainer(p)
// create a trivial agent just to show JADE is running (no GUI integration required here)
        val ac = container.createNewAgent("graphAgent", "jade.core.Agent", arrayOf<Any>())
        val coordinator = container.createNewAgent(
            "coordinator",
            CoordinatorAgent::class.java.name,
            arrayOf<Any?>(graph)
            )
        val taxi1 = container.createNewAgent(
            "taxi1",
            TaxiAgent::class.java.name,
            arrayOf<Any?>(graph, 1, 1000.0)
        )
        val client1 = container.createNewAgent("client1",
            ClientAgent::class.java.name,
            arrayOf<Any?>(graph, 2, 3)
        )
        ac.start()
        coordinator.start()
        taxi1.start()
        client1.start()
    } catch (ex: Exception) {
        println("Не удалось запустить JADE: ${ex.message}")
    }
}


// Entry point
fun main() {
// start JADE in background thread, so Swing EDT isn't blocked
    val graph = Graph()
    SwingUtilities.invokeLater {

        val v1 = graph.addVertex(x = 100, y = 100)
        val v2 = graph.addVertex(x = 300, y =100)
        val v3 = graph.addVertex(x = 200, y = 220)
        graph.addEdge(v1.id, v2.id, 1.0)
        graph.addEdge(v2.id, v3.id, 1.0)
        graph.addEdge(v1.id, v3.id, 2.5)
        GraphFrame(graph)
    }
    Thread { startJadeMainContainer(graph) }.start()
}