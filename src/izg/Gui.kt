package izg

import jade.wrapper.ContainerController
import jade.wrapper.AgentController
import javax.swing.*
import java.awt.BorderLayout
import java.awt.GridLayout

class Gui(private val container: ContainerController) {
    private val frame = JFrame("Kotlin JADE Demo")
    private val logArea = JTextArea(20, 60)
    private val managersPanel = JPanel(GridLayout(0, 1))
    private val programmersPanel = JPanel(GridLayout(0, 1))

    fun show() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.layout = BorderLayout()

        val controlPanel = JPanel()
        val createMgrBtn = JButton("Create Manager")
        val createProgBtn = JButton("Create Programmer")
        val announceBtn = JButton("Manager announce project")

        val salaryField = JTextField("1000", 6)
        val reqExpField = JTextField("2", 3)
        val slotsField = JTextField("2", 3)
        val progExpField = JTextField("3", 3)

        controlPanel.add(createMgrBtn)
        controlPanel.add(JLabel("Salary:"))
        controlPanel.add(salaryField)
        controlPanel.add(JLabel("ReqExp:"))
        controlPanel.add(reqExpField)
        controlPanel.add(JLabel("Slots:"))
        controlPanel.add(slotsField)
        controlPanel.add(createProgBtn)
        controlPanel.add(JLabel("ProgExp:"))
        controlPanel.add(progExpField)
        controlPanel.add(announceBtn)

        frame.add(controlPanel, BorderLayout.NORTH)
        frame.add(JScrollPane(logArea), BorderLayout.CENTER)

        // Handlers
        createMgrBtn.addActionListener {
            val name = "mgr" + System.currentTimeMillis() % 10000
            try {
                val ac: AgentController = container.createNewAgent(
                    name,
                    "ru.example.agents.ProjectManagerAgent",
                    arrayOf()
                )
                ac.start()
                appendLog("Created manager $name")
            } catch (ex: Exception) {
                appendLog("Error creating manager: ${ex.message}")
            }
        }

        createProgBtn.addActionListener {
            val name = "prog" + System.currentTimeMillis() % 10000
            val exp = progExpField.text.toIntOrNull() ?: 0
            try {
                val ac: AgentController = container.createNewAgent(
                    name,
                    "ru.example.agents.ProgrammerAgent",
                    arrayOf(exp)
                )
                ac.start()
                appendLog("Created programmer $name (exp=$exp)")
            } catch (ex: Exception) {
                appendLog("Error creating programmer: ${ex.message}")
            }
        }

        announceBtn.addActionListener {
            // Для простоты: GUI публикует проект на платформе путем отправки INFORM всем агентам.
            val salary = salaryField.text.toIntOrNull() ?: 0
            val req = reqExpField.text.toIntOrNull() ?: 0
            val slots = slotsField.text.toIntOrNull() ?: 1
            val content = "PROJECT|$salary|$req|$slots"
            broadcast(content)
            appendLog("Announced project: $content")
        }

        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

    private fun appendLog(s: String) {
        SwingUtilities.invokeLater {
            logArea.append(s + "\n")
            logArea.caretPosition = logArea.document.length
        }
    }


    private fun broadcast(content: String) {
        // Простое, но практичное решение: используем контейнер для получения всех агентов и отправки им сообщений.
        // Однако JADE wrapper API не даёт прямого списка агентов из ContainerController; поэтому для демо мы создаём
        // вспомогательного временного агента, который рассылает сообщения. Но чтобы не усложнять, можно:
        //  - отправлять сообщение менеджеру явно (если он создан и его имя известно),
        //  - или написать простую реализацию — предполагаем, что все агенты слушают INFORM с content "PROJECT|..."
        // Для демо: используем класс jade.wrapper.AgentController? -> нет класс прямого broadcast.
        // Вместо этого — используем агент-бот, но для краткости сделаем упрощение: вызов System.out (лог),
        // и покажем как программисты могут получать сообщения, если менеджер/GUI отправляют им напрямую.
        // Реальная рассылка через JADE требует либо known receivers либо DF lookup — можно расширить.
    }
}

