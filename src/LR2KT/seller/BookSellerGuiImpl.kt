package LR2KT.seller

import jade.gui.TimeChooser
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.Date
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.border.BevelBorder

/**
 * This is the GUI of the agent that tries to sell books on behalf of its user
 */
class BookSellerGuiImpl : JFrame(), BookSellerGui {
    private var myAgent: BookSellerAgent? = null

    private val titleTF: JTextField
    private val desiredPriceTF: JTextField
    private val minPriceTF: JTextField
    private val deadlineTF: JTextField
    private val setDeadlineB: JButton
    private val setCCB: JButton? = null
    private val sellB: JButton
    private val resetB: JButton
    private val exitB: JButton
    private val logTA: JTextArea

    private var deadline: Date? = null

    override fun setAgent(a: BookSellerAgent) {
        myAgent = a
        setTitle(myAgent!!.getName())
    }

    init {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                myAgent!!.doDelete()
            }
        })

        val rootPanel = JPanel()
        rootPanel.setLayout(GridBagLayout())
        rootPanel.minimumSize = Dimension(330, 125)
        rootPanel.preferredSize = Dimension(330, 125)

        /**//////// */
// Line 0
        /**//////// */
        var l = JLabel("Book to sell:")
        l.setHorizontalAlignment(SwingConstants.LEFT)
        var gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 0
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(l, gridBagConstraints)

        titleTF = JTextField(64)
        titleTF.minimumSize = Dimension(222, 20)
        titleTF.preferredSize = Dimension(222, 20)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 1
        gridBagConstraints.gridy = 0
        gridBagConstraints.gridwidth = 3
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(titleTF, gridBagConstraints)

        /**//////// */
// Line 1
        /**//////// */
        l = JLabel("Best price:")
        l.setHorizontalAlignment(SwingConstants.LEFT)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 1
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(l, gridBagConstraints)

        desiredPriceTF = JTextField(64)
        desiredPriceTF.minimumSize = Dimension(70, 20)
        desiredPriceTF.preferredSize = Dimension(70, 20)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 1
        gridBagConstraints.gridy = 1
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(desiredPriceTF, gridBagConstraints)

        l = JLabel("Min price:")
        l.setHorizontalAlignment(SwingConstants.LEFT)
        l.minimumSize = Dimension(70, 20)
        l.preferredSize = Dimension(70, 20)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 2
        gridBagConstraints.gridy = 1
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(l, gridBagConstraints)

        minPriceTF = JTextField(64)
        minPriceTF.minimumSize = Dimension(70, 20)
        minPriceTF.preferredSize = Dimension(70, 20)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 3
        gridBagConstraints.gridy = 1
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(minPriceTF, gridBagConstraints)

        /**//////// */
// Line 2
        /**//////// */
        l = JLabel("Deadline:")
        l.setHorizontalAlignment(SwingConstants.LEFT)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 2
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(l, gridBagConstraints)

        deadlineTF = JTextField(64)
        deadlineTF.setMinimumSize(Dimension(146, 20))
        deadlineTF.setPreferredSize(Dimension(146, 20))
        deadlineTF.setEnabled(false)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 1
        gridBagConstraints.gridy = 2
        gridBagConstraints.gridwidth = 2
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(deadlineTF, gridBagConstraints)

        setDeadlineB = JButton("Set")
        setDeadlineB.setMinimumSize(Dimension(70, 20))
        setDeadlineB.setPreferredSize(Dimension(70, 20))
        setDeadlineB.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                var d = deadline
                if (d == null) {
                    d = Date()
                }
                val tc = TimeChooser(d)
                if (tc.showEditTimeDlg(this@BookSellerGuiImpl) == TimeChooser.OK) {
                    deadline = tc.getDate()
                    deadlineTF.setText(deadline.toString())
                }
            }
        })
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 3
        gridBagConstraints.gridy = 2
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(setDeadlineB, gridBagConstraints)

        rootPanel.setBorder(BevelBorder(BevelBorder.LOWERED))

        getContentPane().add(rootPanel, BorderLayout.NORTH)

        logTA = JTextArea()
        logTA.setEnabled(false)
        val jsp = JScrollPane(logTA)
        jsp.minimumSize = Dimension(300, 180)
        jsp.preferredSize = Dimension(300, 180)
        var p = JPanel()
        p.setBorder(BevelBorder(BevelBorder.LOWERED))
        p.add(jsp)
        contentPane.add(p, BorderLayout.CENTER)

        p = JPanel()
        sellB = JButton("Sell")
        sellB.addActionListener {
            val title = titleTF.getText()
            var desiredPrice = -1
            var minPrice = -1
            if (title != null && title.isNotEmpty()) {
                if (deadline != null && deadline!!.time > System.currentTimeMillis()) {
                    try {
                        desiredPrice = desiredPriceTF.getText().toInt()
                        try {
                            minPrice = minPriceTF.getText().toInt()
                            if (minPrice <= desiredPrice) {
                                // myAgent.addToCatalogue(title, desiredPrice, minPrice, deadline.getTime());
                                myAgent!!.putForSale(title, desiredPrice, minPrice, deadline!!)
                                notifyUser("PUT FOR SALE: $title between $desiredPrice and $minPrice by $deadline")
                            } else {
                                // minPrice > desiredPrice
                                JOptionPane.showMessageDialog(
                                    this@BookSellerGuiImpl,
                                    "Min price must be cheaper than best price",
                                    "WARNING",
                                    JOptionPane.WARNING_MESSAGE
                                )
                            }
                        } catch (ex1: Exception) {
                            // Invalid max cost
                            JOptionPane.showMessageDialog(
                                this@BookSellerGuiImpl,
                                "Invalid min price",
                                "WARNING",
                                JOptionPane.WARNING_MESSAGE
                            )
                        }
                    } catch (ex2: Exception) {
                        // Invalid desired cost
                        JOptionPane.showMessageDialog(
                            this@BookSellerGuiImpl,
                            "Invalid best price",
                            "WARNING",
                            JOptionPane.WARNING_MESSAGE
                        )
                    }
                } else {
                    // No deadline specified
                    JOptionPane.showMessageDialog(
                        this@BookSellerGuiImpl,
                        "Invalid deadline",
                        "WARNING",
                        JOptionPane.WARNING_MESSAGE
                    )
                }
            } else {
                // No book title specified
                JOptionPane.showMessageDialog(
                    this@BookSellerGuiImpl,
                    "No book title specified",
                    "WARNING",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }
        resetB = JButton("Reset")
        resetB.addActionListener {
            titleTF.text = ""
            desiredPriceTF.text = ""
            minPriceTF.text = ""
            deadlineTF.text = ""
            deadline = null
        }
        exitB = JButton("Exit")
        exitB.addActionListener { myAgent!!.doDelete() }

        sellB.preferredSize = resetB.getPreferredSize()
        exitB.preferredSize = resetB.getPreferredSize()

        p.add(sellB)
        p.add(resetB)
        p.add(exitB)

        p.setBorder(BevelBorder(BevelBorder.LOWERED))
        contentPane.add(p, BorderLayout.SOUTH)

        pack()

        setResizable(false)
    }

    override fun notifyUser(message: String?) {
        logTA.append(message + "\n")
    }
}