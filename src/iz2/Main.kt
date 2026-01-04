package iz2

import iz2.graph.Graph
import iz2.graph.GraphFrame
import javax.swing.SwingUtilities

class Main
fun startJadeMainContainer() {
    try {
        val rt = jade.core.Runtime.instance()
        val p = jade.core.ProfileImpl()
        p.setParameter(jade.core.Profile.MAIN_HOST, "127.0.0.1")
        val container = rt.createMainContainer(p)
// create a trivial agent just to show JADE is running (no GUI integration required here)
        val ac = container.createNewAgent("graphAgent", "jade.core.Agent", arrayOf<Any>())
        ac.start()
    } catch (ex: Exception) {
        println("Не удалось запустить JADE: ${ex.message}")
    }
}

fun main() {
// start JADE in background thread, so Swing EDT isn't blocked
    Thread { startJadeMainContainer() }.start()


    SwingUtilities.invokeLater {
        val graph = Graph()
// add an example graph
//        val v1 = graph.addVertex(x = 100, y = 100)
//        val v2 = graph.addVertex(x = 300, y =100)
//        val v3 = graph.addVertex(x = 200, y = 220)
//        graph.addEdge(v1.id, v2.id, 1.0)
//        graph.addEdge(v2.id, v3.id, 1.0)
//        graph.addEdge(v1.id, v3.id, 2.5)


        GraphFrame(graph)
    }
}