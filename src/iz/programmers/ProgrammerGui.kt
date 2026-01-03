package iz.programmers

import jade.wrapper.AgentController
import jade.wrapper.ContainerController
import jade.wrapper.StaleProxyException
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField

class ProgrammerGui(private val container: ContainerController) : JPanel() {
    private val nameField = JTextField("P")
    private val expField = JTextField("3")
    private val createBtn = JButton("Create ProgrammerAgent")

    init {
        layout = GridBagLayout()
        val c = GridBagConstraints()
        c.insets = Insets(8, 8, 8, 8)
        c.fill = GridBagConstraints.HORIZONTAL


        c.gridx = 0; c.gridy = 0; c.weightx = 0.0
        add(JLabel("Agent name:"), c)
        c.gridx = 1; c.weightx = 1.0
        add(nameField, c)


        c.gridx = 0; c.gridy = 1; c.weightx = 0.0
        add(JLabel("Experience (years):"), c)
        c.gridx = 1; c.weightx = 1.0
        add(expField, c)


        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weightx = 0.0
        add(createBtn, c)


        createBtn.addActionListener {
            val name = nameField.text.trim()
            val expText = expField.text.trim()
            if (name.isEmpty() || expText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Заполните имя агента и опыт")
                return@addActionListener
            }
            try {
                val exp = expText.toInt()
                createProgrammerAgent(name, exp)
            } catch (ex: NumberFormatException) {
                JOptionPane.showMessageDialog(this, "Опыт должен быть целым числом")
            }
        }
    }


    private fun createProgrammerAgent(name: String, experience: Int) {
        try {
            val args: Array<Any> = arrayOf(experience.toString())
            val ac: AgentController = container.createNewAgent(name, "iz.programmers.ProgrammerAgent", args)
            ac.start()
            JOptionPane.showMessageDialog(this, "ProgrammerAgent '$name' создан")
        } catch (ex: StaleProxyException) {
            ex.printStackTrace()
            JOptionPane.showMessageDialog(this, "Не удалось создать агента: ${ex.message}")
        }
    }


    // Convenience: show this panel in its own frame (useful for testing)
    fun showAsFrame() {
        val f = JFrame("Create ProgrammerAgent")
        f.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        f.contentPane.add(this)
        f.pack()
        f.setSize(360, 200)
        f.setLocationRelativeTo(null)
        f.isVisible = true
    }
}