package iz2

import iz2.agents.ClientAgent
import iz2.agents.CoordinatorAgent
import iz2.agents.TaxiAgent
import iz2.graph.Graph
import iz2.graph.GraphFrame
import java.util.Collections
import javax.swing.SwingUtilities

@Volatile
var jadeContainer: jade.wrapper.AgentContainer? = null
val createdAgentNames: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

fun startJadeMainContainer(graph: Graph) {
    try {
        val rt = jade.core.Runtime.instance()
        val p = jade.core.ProfileImpl()
        p.setParameter(jade.core.Profile.MAIN_HOST, "127.0.0.1")
        val container = rt.createMainContainer(p)
        jadeContainer = container
        val ac = container.createNewAgent("graphAgent", "jade.core.Agent", arrayOf<Any>())
        val coordinator = container.createNewAgent(
            "coordinator",
            CoordinatorAgent::class.java.name,
            arrayOf<Any?>(graph)
            )
        val taxi1 = container.createNewAgent(
            "taxi1",
            TaxiAgent::class.java.name,
            arrayOf<Any?>(graph, 1, 2000.0)
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




fun createAgentInContainer(name: String, className: String, args: Array<Any?> = arrayOf()) {
    try {
        val container = jadeContainer ?: throw IllegalStateException("JADE container not initialized")
        val ac = container.createNewAgent(name, className, args)
        ac.start()
        createdAgentNames.add(name)
    } catch (ex: Exception) {
        println("Failed to create agent $name: ${ex.message}")
    }
}


fun killAgentInContainer(name: String) {
    try {
        val container = jadeContainer ?: throw IllegalStateException("JADE container not initialized")
        val controller = container.getAgent(name)
        controller.kill()
        createdAgentNames.remove(name)
    } catch (ex: Exception) {
        println("Failed to kill agent $name: ${ex.message}")
    }
}