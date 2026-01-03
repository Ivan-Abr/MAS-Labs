package iz.projects

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

class ProjectGui(private val container: ContainerController) : JPanel() {
    private val nameField = JTextField("Project")
    private val slotsField = JTextField("2")
    private val reqExpField = JTextField("2")
    private val costField = JTextField("100")
    private val createBtn = JButton("Create ProjectAgent")
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
        add(JLabel("Required programmers (slots):"), c)
        c.gridx = 1; c.weightx = 1.0
        add(slotsField, c)


        c.gridx = 0; c.gridy = 2; c.weightx = 0.0
        add(JLabel("Required experience (years):"), c)
        c.gridx = 1; c.weightx = 1.0
        add(reqExpField, c)


        c.gridx = 0; c.gridy = 3; c.weightx = 0.0
        add(JLabel("Cost:"), c)
        c.gridx = 1; c.weightx = 1.0
        add(costField, c)


        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; c.weightx = 0.0
        add(createBtn, c)


        createBtn.addActionListener {
            val name = nameField.text.trim()
            val slotsText = slotsField.text.trim()
            val reqExpText = reqExpField.text.trim()
            val costText = costField.text.trim()
            if (name.isEmpty() || slotsText.isEmpty() || reqExpText.isEmpty() || costText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Заполните все поля")
                return@addActionListener
            }
            try {
                val slots = slotsText.toInt()
                val reqExp = reqExpText.toInt()
                val cost = costText.toInt()
                createProjectAgent(name, slots, reqExp, cost)
            } catch (ex: NumberFormatException) {
                JOptionPane.showMessageDialog(this, "Slots/Exp/Cost должны быть целыми числами")
            }
        }
    }


    private fun createProjectAgent(name: String, slots: Int, reqExp: Int, cost: Int) {
        try {
            val args: Array<Any> = arrayOf(slots.toString(), reqExp.toString(), cost.toString())
            val ac: AgentController = container.createNewAgent(name, "iz.projects.ProjectAgent", args)
            ac.start()
            JOptionPane.showMessageDialog(this, "ProjectAgent '$name' создан")
        } catch (ex: StaleProxyException) {
            ex.printStackTrace()
            JOptionPane.showMessageDialog(this, "Не удалось создать агента: ${ex.message}")
        }
    }


    fun showAsFrame() {
        val f = JFrame("Create ProjectAgent")
        f.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        f.contentPane.add(this)
        f.pack()
        f.setSize(420, 300)
        f.setLocationRelativeTo(null)
        f.isVisible = true
    }
}
