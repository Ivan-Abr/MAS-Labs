package iz

import iz.programmers.ProgrammerGui
import iz.projects.ProjectGui
import jade.core.ProfileImpl
import jade.core.Runtime
import jade.wrapper.ContainerController
import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

class Main {
}

fun main() {
    val rt = Runtime.instance()
    val p = ProfileImpl()
    p.setParameter(ProfileImpl.GUI, "true")
    val mainContainer: ContainerController = rt.createMainContainer(p)


    SwingUtilities.invokeLater {
        val frame = JFrame("JADE Agent Creator (Programmer & Project)")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        val tabs = JTabbedPane()
        tabs.addTab("Programmer", ProgrammerGui(mainContainer))
        tabs.addTab("Project", ProjectGui(mainContainer))
        frame.contentPane.add(tabs)
        frame.pack()
        frame.setSize(560, 360)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}