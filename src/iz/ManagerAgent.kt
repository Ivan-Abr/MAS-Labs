package iz

import jade.core.Agent
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

class ManagerAgent : Agent() {
    override fun setup() {
        val cont = containerController
        SwingUtilities.invokeLater {
            val frame = javax.swing.JFrame("JADE Agent Creator (ManagerAgent)")
            frame.defaultCloseOperation = javax.swing.WindowConstants.DISPOSE_ON_CLOSE
            val tabs = JTabbedPane()
            tabs.addTab("Programmer", iz.programmers.ProgrammerGui(cont))
            tabs.addTab("Project", iz.projects.ProjectGui(cont))
            frame.contentPane.add(tabs)
            frame.pack()
            frame.setSize(560, 360)
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
        }
        println("ManagerAgent $localName started and GUI opened")
    }


    override fun takeDown() {
        println("ManagerAgent $localName terminating")
    }
}