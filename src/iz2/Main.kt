package iz2

import iz2.agents.ClientAgent
import iz2.agents.CoordinatorAgent
import iz2.agents.TaxiAgent
import iz2.graph.Graph
import iz2.graph.GraphFrame
import javax.swing.SwingUtilities

fun startJadeMainContainer(graph: Graph) {
    try {
        val rt = jade.core.Runtime.instance()
        val p = jade.core.ProfileImpl()
        p.setParameter(jade.core.Profile.MAIN_HOST, "127.0.0.1")
        val container = rt.createMainContainer(p)
        val ac = container.createNewAgent("graphAgent", "jade.core.Agent", arrayOf<Any>())
        val coordinator = container.createNewAgent(
            "coordinator",
            CoordinatorAgent::class.java.name,
            arrayOf<Any?>(graph)
            )
        val taxi1 = container.createNewAgent(
            "taxi1",
            TaxiAgent::class.java.name,
            arrayOf<Any?>(graph, 1, 3000.0)
        )
        val client1 = container.createNewAgent("client1",
            ClientAgent::class.java.name,
            arrayOf<Any?>(graph, 2, 4)
        )
        ac.start()
        coordinator.start()
        taxi1.start()
        client1.start()
    } catch (ex: Exception) {
        println("Не удалось запустить JADE: ${ex.message}")
    }
}


fun main() {
    val graph = Graph()
    SwingUtilities.invokeLater {

        val v1 = graph.addVertex(x = 100, y = 100)
        val v2 = graph.addVertex(x = 300, y =100)
        val v3 = graph.addVertex(x = 200, y = 220)
        val v4 = graph.addVertex(x = 330, y = 250)
        graph.addEdge(v1.id, v2.id, 1.0)
        graph.addEdge(v2.id, v3.id, 1.0)
        graph.addEdge(v1.id, v3.id, 2.5)
        graph.addEdge(v3.id, v4.id, 1.5)
        GraphFrame(graph)
    }
    Thread { startJadeMainContainer(graph) }.start()
}