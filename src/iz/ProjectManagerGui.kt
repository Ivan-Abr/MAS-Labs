package iz

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField

class ProjectManagerGui(private val agent: ProjectManagerAgent) : JFrame(agent.localName) {
    private val titleField: JTextField
    private val priceField: JTextField

    init {
        var p = JPanel()
        p.setLayout(GridLayout(2, 2))
        p.add(JLabel("Book title:"))
        titleField = JTextField(15)
        p.add(titleField)
        p.add(JLabel("Price:"))
        priceField = JTextField(15)
        p.add(priceField)
        contentPane.add(p, BorderLayout.CENTER)

        val addButton = JButton("Add")
        addButton.addActionListener {
            try {
                val title = titleField.getText().trim { it <= ' ' }
                val price = priceField.getText().trim { it <= ' ' }
                titleField.text = ""
                priceField.text = ""
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this@ProjectManagerGui,
                    "Invalid values. " + e.message,
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
        p = JPanel()
        p.add(addButton)
        contentPane.add(p, BorderLayout.SOUTH)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                agent.doDelete()
            }
        })

        setResizable(false)
    }
    override fun show() {
        pack()
        val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
        val centerX = screenSize.getWidth().toInt() / 2
        val centerY = screenSize.getHeight().toInt() / 2
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2)
        super.show()
    }
}