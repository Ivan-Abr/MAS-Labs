package iz2.graph

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.hypot

class GraphPanel(val graph: Graph) : JPanel() {
    private val radius = 20
    private var selectedVertex: Vertex? = null
    private var draggingVertex: Vertex? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    var highlightedPath: List<Int> = emptyList()

    init {
        background = Color(240, 240, 240)
        preferredSize = Dimension(800, 600)

        graph.addChangeListener {
            if (SwingUtilities.isEventDispatchThread()) {
                repaint()
            } else {
                SwingUtilities.invokeLater { repaint() }
            }
        }

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val v = findVertexAt(e.x, e.y)
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (v == null) {
                        selectedVertex = null
                        repaint()
                    } else {
                        selectedVertex = v
                        draggingVertex = v
                        dragOffsetX = e.x - v.x
                        dragOffsetY = e.y - v.y
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    selectedVertex = v
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                draggingVertex = null
            }

            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && SwingUtilities.isRightMouseButton(e)) {
                    val v = findVertexAt(e.x, e.y)
                    if (v != null) {
                        graph.removeVertex(v.id)
                        if (highlightedPath.isNotEmpty() && highlightedPath.contains(v.id)) highlightedPath = emptyList()
                        repaint()
                    }
                }
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                draggingVertex?.let {
                    it.x = e.x - dragOffsetX
                    it.y = e.y - dragOffsetY
                    repaint()
                }
            }
        })
    }

    private fun findVertexAt(x: Int, y: Int): Vertex? {
        return graph.vertices.values.find { hypot((it.x - x).toDouble(), (it.y - y).toDouble()) <= radius }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        for (edge in graph.edges) {
            val a = graph.vertices[edge.from] ?: continue
            val b = graph.vertices[edge.to] ?: continue
            g2.stroke = BasicStroke(2f)
            g2.color = Color.DARK_GRAY
            g2.drawLine(a.x, a.y, b.x, b.y)
            val mx = (a.x + b.x) / 2
            val my = (a.y + b.y) / 2
            g2.font = Font("Arial", Font.PLAIN, 12)
            g2.drawString(edge.weight.toString(), mx + 6, my - 6)
        }
        if (highlightedPath.size >= 2) {
            g2.stroke = BasicStroke(4f)
            g2.color = Color.RED
            for (i in 0 until highlightedPath.size - 1) {
                val v1 = graph.vertices[highlightedPath[i]] ?: continue
                val v2 = graph.vertices[highlightedPath[i + 1]] ?: continue
                g2.drawLine(v1.x, v1.y, v2.x, v2.y)
            }
        }

        val taxiAt = mutableMapOf<Int, MutableList<Pair<String, Graph.TaxiState>>>()
        for ((aid, st) in graph.taxiStates) {
            taxiAt.getOrPut(st.location) { mutableListOf() }.add(aid to st)
        }

        val clientAt = mutableMapOf<Int, MutableList<String>>()
        for ((cid, st) in graph.clientStates) {
            clientAt.getOrPut(st.location) { mutableListOf() }.add(cid)
        }

// draw vertices
        for (v in graph.vertices.values) {
            val taxis = taxiAt[v.id].orEmpty()
            val clients = clientAt[v.id].orEmpty()
            val isSelected = (selectedVertex?.id == v.id)
            val colorFill = when {
                taxis.any { it.second.withClient } -> Color(255, 0, 0) //такси занято клиентом
                clients.isNotEmpty() && taxis.any { !it.second.withClient} -> Color(255, 255, 0) //
                clients.isNotEmpty() -> Color(255, 100, 0) //клиент ждет
                taxis.any { !it.second.withClient } -> Color(0, 255, 0) //свободное такси
                isSelected -> Color(180, 220, 255) // выбранная вершина
                else -> Color(255, 255, 255) // по умолчанию
            }
            g2.color = colorFill
            g2.fillOval(v.x - radius, v.y - radius, radius * 2, radius * 2)
            g2.color = Color.BLACK
            g2.drawOval(v.x - radius, v.y - radius, radius * 2, radius * 2)
            val label = v.label
            val fm = g2.fontMetrics
            val lw = fm.stringWidth(label)
            g2.drawString(label, v.x - lw / 2, v.y + fm.ascent / 2)

            var badgeY = v.y - radius - 10
            for ((aid, st) in taxis) {
                val badge = if (st.withClient) "T:${aid}(C)" else "T:${aid}"
                val bw = g2.fontMetrics.stringWidth(badge)
                g2.drawString(badge, v.x - bw / 2, badgeY)
                badgeY -= 12
            }
            for (cid in clients) {
                val badge = "C:${cid}"
                val bw = g2.fontMetrics.stringWidth(badge)
                g2.drawString(badge, v.x - bw / 2, badgeY)
                badgeY -= 12
            }
        }
    }
}
