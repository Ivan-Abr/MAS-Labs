package iz2.graph

import java.util.LinkedList
import java.util.PriorityQueue
import java.util.Queue
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

class Graph {
    private val idCounter = AtomicInteger(1)
    val vertices = mutableMapOf<Int, Vertex>()
    val edges = mutableListOf<Edge>()
    private val adj = mutableMapOf<Int, MutableList<Pair<Int, Double>>>()


    data class TaxiState(var location: Int, var available: Boolean, var withClient: Boolean = false, var timePerWeight: Double = 1.0)
    data class ClientState(var location: Int, var inTaxi: Boolean = false)

    val taxiStates = mutableMapOf<String, TaxiState>()
    val clientStates = mutableMapOf<String, ClientState>()

    private val listeners = mutableListOf<()-> Unit>()
    fun addChangeListener(listener: () -> Unit) { listeners.add(listener) }
    fun notifyChange() {
        val r = Runnable {
            for (l in listeners) {
                try { l() } catch (_: Exception) { /* ignore */ }
            }
        }
        if (SwingUtilities.isEventDispatchThread()) {
            r.run()
        } else {
            SwingUtilities.invokeLater(r)
        }
    }

    fun addVertex(customId: Int? = null,
                  x: Int,
                  y: Int
    ): Vertex {
        val id = customId?: idCounter.andIncrement
        val name = "V$id"
        println("$id $name $x $y")
        val v = Vertex(id, name, x, y)
        vertices[id] = v
        adj[id] = mutableListOf()
        return v
    }

    fun removeVertex(id: Int) {
        vertices.remove(id)
        adj.remove(id)
        val edgeIterator = edges.iterator()
        while (edgeIterator.hasNext()) {
            val e = edgeIterator.next()
            if (id == e.from || id == e.to) edgeIterator.remove()
        }
        for (list in adj.values) {
            val it = list.iterator()
            while (it.hasNext()) {
                if (it.next().first == id) it.remove()
            }
        }
        taxiStates.entries.removeIf { it.value.location == id }
        clientStates.entries.removeIf { it.value.location == id }
        notifyChange()
    }

    fun addEdge(a: Int, b: Int, weight: Double = 1.0) {
        if (!vertices.contains(a) || !vertices.containsKey(b)) return
        edges.add(Edge(a, b, weight))
        adj.getOrPut(a) { mutableListOf() }.add(b to weight)
        adj.getOrPut(b) { mutableListOf() }.add(a to weight)
    }

    fun removeEdge(a: Int, b: Int) {
        val edgeIter = edges.iterator()
        while (edgeIter.hasNext()) {
            val e = edgeIter.next()
            if ((e.from == a && e.to == b) || (e.from == b && e.to == a)) edgeIter.remove()
        }
        adj[a]?.let { list ->
            val it = list.iterator()
            while (it.hasNext()) {
                if (it.next().first == b) it.remove()
            }
        }
        adj[b]?.let { list ->
            val it = list.iterator()
            while (it.hasNext()) {
                if (it.next().first == a) it.remove()
            }
        }
        adj[a]?.removeIf { it.first == b }
        adj[b]?.removeIf { it.first == a }
        notifyChange()
    }

    fun clear() {
        vertices.clear()
        edges.clear()
        adj.clear()
        idCounter.set(1)
        notifyChange()
    }

    fun shortestPathBFS(start: Int, end: Int): Pair<List<Int>, Double> {
        if (!vertices.containsKey(start) || !vertices.containsKey(end)) return Pair(emptyList(), 0.0)
        val q: Queue<Int> = LinkedList()
        val parent = mutableMapOf<Int, Int?>()
        q.add(start)
        parent[start] = null
        while (q.isNotEmpty()) {
            val cur = q.remove()
            if (cur == end) break
            for (nei in adj[cur].orEmpty()) {
                val v = nei.first
                if (!parent.containsKey(v)) {
                    parent[v] = cur
                    q.add(v)
                }
            }
        }
        if (!parent.containsKey(end)) return Pair(emptyList(), 0.0)
        val path = mutableListOf<Int>()
        var cur: Int? = end
        while (cur!=null) {
            path.add(cur)
            cur = parent[cur]
        }
        return Pair(path.reversed(), path.size.toDouble())
    }

    fun shortestPathDijkstra(start: Int, end: Int): Pair<List<Int>, Double> {
        if (!vertices.containsKey(start) || !vertices.containsKey(end)) return Pair(emptyList(), 0.0)
        val dist = mutableMapOf<Int, Double>()
        val prev = mutableMapOf<Int, Int?>()
        var sumWeight = 0.0
        val pq = PriorityQueue(compareBy<Pair<Int, Double>> { it.second })
        for (v in vertices.keys) { dist[v] = Double.POSITIVE_INFINITY; prev[v] = null }
        dist[start] = 0.0
        pq.add(start to 0.0)
        while (pq.isNotEmpty()) {
            val (u, du) = pq.poll()
            if (du > dist[u]!!) continue
            if (u == end) break
            for ((v, w) in adj[u].orEmpty()) {
                val alt = dist[u]!! + w
                if (alt < dist[v]!!) {
                    sumWeight += dist[v]!!
                    dist[v] = alt
                    prev[v] = u
                    pq.add(v to alt)
                }
            }
        }
        if (dist[end] == Double.POSITIVE_INFINITY) return Pair(emptyList(), 0.0)
        val path = mutableListOf<Int>()
        var cur: Int? = end
        while (cur != null) { path.add(cur); cur = prev[cur] }
        return Pair(path.reversed(), dist[end]?: 0.0)
    }

    fun edgeWeight(a: Int, b: Int): Double? {
        return adj[a]?.firstOrNull { it.first == b }?.second
                ?: edges.firstOrNull {
                    (it.from == a && it.to == b) || (it.from == b && it.to == a)
                }?.weight
    }
}
