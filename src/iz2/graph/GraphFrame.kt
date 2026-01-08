package iz2.graph

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField

class GraphFrame(val graph: Graph) : JFrame("Kotlin Graph Editor + Visualization") {
    private val panel = GraphPanel(graph)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        add(panel, BorderLayout.CENTER)


        val logArea = JTextArea()
        logArea.isEditable = false
        logArea.lineWrap = true
        logArea.wrapStyleWord = true
        val logScroll = JScrollPane(logArea)
        logScroll.preferredSize = Dimension(320, 600)
        add(logScroll, BorderLayout.EAST)

// Bottom controls
        val bottom = JPanel()
        bottom.layout = FlowLayout(FlowLayout.LEFT)


        val startField = JTextField(4)
        val endField = JTextField(4)
        val findUnweighted = JButton("BFS shortest path")
        val findWeighted = JButton("Dijkstra shortest path")
        val addEdgeBtn = JButton("Add edge (from -> to)")
        val removeEdgeBtn = JButton("Remove edge")
        val clearBtn = JButton("Clear all")


        bottom.add(JLabel("From")); bottom.add(startField)
        bottom.add(JLabel("To")); bottom.add(endField)
        bottom.add(findUnweighted); bottom.add(findWeighted)
        bottom.add(addEdgeBtn); bottom.add(removeEdgeBtn)
        bottom.add(clearBtn)


        add(bottom, BorderLayout.SOUTH)


        findUnweighted.addActionListener {
            val s = startField.text.toIntOrNull(); val t = endField.text.toIntOrNull()
            if (s == null || t == null) { JOptionPane.showMessageDialog(this, "Введите корректные id вершин") ; return@addActionListener }
            val path = graph.shortestPathBFS(s, t).first
            panel.highlightedPath = path
            panel.repaint()
            if (path.isEmpty()) JOptionPane.showMessageDialog(this, "Путь не найден")
        }


        findWeighted.addActionListener {
            val s = startField.text.toIntOrNull(); val t = endField.text.toIntOrNull()
            if (s == null || t == null) { JOptionPane.showMessageDialog(this, "Введите корректные id вершин") ; return@addActionListener }
            val path = graph.shortestPathDijkstra(s, t).first
            panel.highlightedPath = path
            panel.repaint()
            if (path.isEmpty()) JOptionPane.showMessageDialog(this, "Путь не найден")
        }

        val addVertexBtn = JButton("Add vertex")
        bottom.add(addVertexBtn)

        addVertexBtn.addActionListener {
            // Поля для диалога
            val idField = JTextField()
            val xField = JTextField()
            val yField = JTextField()

            val inputPanel = JPanel(GridLayout(0, 2))
            inputPanel.add(JLabel("ID (оставьте пустым для авто):")); inputPanel.add(idField)
            inputPanel.add(JLabel("X (опционально):")); inputPanel.add(xField)
            inputPanel.add(JLabel("Y (опционально):")); inputPanel.add(yField)

            val result = JOptionPane.showConfirmDialog(
                this,
                inputPanel,
                "Создать вершину",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            )

            if (result != JOptionPane.OK_OPTION) return@addActionListener

            val idInput = idField.text.trim()
            val xInput = xField.text.trim()
            val yInput = yField.text.trim()

            val idVal = idInput.toIntOrNull()
            val xVal = xInput.toIntOrNull() ?: (panel.width / 2).coerceAtLeast(50)
            val yVal = yInput.toIntOrNull() ?: (panel.height / 2).coerceAtLeast(50)
            graph.addVertex(idVal, xVal, yVal)
        }

        addEdgeBtn.addActionListener {
            val s = startField.text.toIntOrNull(); val t = endField.text.toIntOrNull()
            if (s == null || t == null) { JOptionPane.showMessageDialog(this, "Введите корректные id вершин") ; return@addActionListener }
            val w = JOptionPane.showInputDialog(this, "Вес ребра (число)", "1.0")?.toDoubleOrNull() ?: 1.0
            graph.addEdge(s, t, w)
            panel.repaint()
        }


        removeEdgeBtn.addActionListener {
            val s = startField.text.toIntOrNull(); val t = endField.text.toIntOrNull()
            if (s == null || t == null) { JOptionPane.showMessageDialog(this, "Введите корректные id вершин") ; return@addActionListener }
            graph.removeEdge(s, t)
            panel.repaint()
        }


        clearBtn.addActionListener {
            graph.clear(); panel.highlightedPath = emptyList(); panel.repaint()
        }

        graph.addChangeListener {
            val logs = graph.getLogsSnapshot()
            val text = logs.joinToString("\n")
                if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                    logArea.text = text
                    logArea.caretPosition = text.length
                } else {
                    javax.swing.SwingUtilities.invokeLater {
                        logArea.text = text
                        logArea.caretPosition = text.length
                    }
                }
        }
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}
