package iz2.graph

import iz2.agents.ClientAgent
import iz2.agents.TaxiAgent
import iz2.createAgentInContainer
import iz2.createdAgentNames
import iz2.killAgentInContainer
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

class GraphFrame(val graph: Graph) : JFrame("Taxi Simulation") {
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

        val addTaxiBtn = JButton("Add Taxi")
        val removeTaxiBtn = JButton("Remove Taxi")
        val addClientBtn = JButton("Add Client")
        val removeClientBtn = JButton("Remove Client")
        bottom.add(addTaxiBtn); bottom.add(removeTaxiBtn); bottom.add(addClientBtn); bottom.add(removeClientBtn)

        addTaxiBtn.addActionListener {
            val name = JOptionPane.showInputDialog(this, "Taxi agent name", "taxi${createdAgentNames.size + 1}")?.toString() ?: return@addActionListener
            val start = JOptionPane.showInputDialog(this, "Start vertex id", "1")?.toIntOrNull() ?: 1
            val time = JOptionPane.showInputDialog(this, "Time per weight (ms)", "1000")?.toDoubleOrNull() ?: 1000.0
            createAgentInContainer(name, TaxiAgent::class.java.name, arrayOf<Any?>(graph, start, time))
            graph.addLog("Requested creation of taxi agent $name")
        }

        removeTaxiBtn.addActionListener {
            val name = JOptionPane.showInputDialog(this, "Taxi agent name to remove", "taxi1")?.toString() ?: return@addActionListener
            try { killAgentInContainer(name); graph.taxiStates.remove(name); graph.addLog("Requested removal of taxi agent $name") } catch (ex: Exception) { JOptionPane.showMessageDialog(this, "Failed: ${ex.message}") }
        }


        addClientBtn.addActionListener {
            val name = JOptionPane.showInputDialog(this, "Client agent name", "client${createdAgentNames.size + 1}")?.toString() ?: return@addActionListener
            val start = JOptionPane.showInputDialog(this, "Start vertex id", "1")?.toIntOrNull() ?: 1
            val dest = JOptionPane.showInputDialog(this, "Dest vertex id", "2")?.toIntOrNull() ?: 2
            createAgentInContainer(name, ClientAgent::class.java.name, arrayOf<Any?>(graph, start, dest))
            graph.addLog("Requested creation of client agent $name")
        }


        removeClientBtn.addActionListener {
            val name = JOptionPane.showInputDialog(this, "Client agent name to remove", "client1")?.toString() ?: return@addActionListener
            try { killAgentInContainer(name); graph.clientStates.remove(name); graph.addLog("Requested removal of client agent $name") } catch (ex: Exception) { JOptionPane.showMessageDialog(this, "Failed: ${ex.message}") }
        }

        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}
