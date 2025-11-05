package LR2KT.buyer


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
 * J2SE (Swing-based) implementation of the GUI of the agent that
 * tries to buy books on behalf of its user
 */
class BookBuyerGuiImpl : JFrame(), BookBuyerGui {
    private var myAgent: BookBuyerAgent? = null

    private val titleTF: JTextField
    private val desiredCostTF: JTextField
    private val maxCostTF: JTextField
    private val deadlineTF: JTextField
    private val setDeadlineB: JButton
    private val setCCB: JButton
    private val buyB: JButton
    private val resetB: JButton
    private val exitB: JButton
    private val logTA: JTextArea

    private var deadline: Date? = null

    init {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                myAgent!!.doDelete()
            }
        })

        val rootPanel = JPanel()
        rootPanel.setLayout(GridBagLayout())
        rootPanel.setMinimumSize(Dimension(330, 125))
        rootPanel.setPreferredSize(Dimension(330, 125))

        /**//////// */
// Line 0
        /**//////// */
        var l = JLabel("Book to buy:")
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
        l = JLabel("Best cost:")
        l.setHorizontalAlignment(SwingConstants.LEFT)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 1
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(l, gridBagConstraints)

        desiredCostTF = JTextField(64)
        desiredCostTF.minimumSize = Dimension(70, 20)
        desiredCostTF.preferredSize = Dimension(70, 20)
        desiredCostTF.isEditable = true
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 1
        gridBagConstraints.gridy = 1
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(desiredCostTF, gridBagConstraints)

        l = JLabel("Max cost:")
        l.setHorizontalAlignment(SwingConstants.LEFT)
        l.minimumSize = Dimension(70, 20)
        l.preferredSize = Dimension(70, 20)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 2
        gridBagConstraints.gridy = 1
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(l, gridBagConstraints)

        maxCostTF = JTextField(64)
        maxCostTF.minimumSize = Dimension(70, 20)
        maxCostTF.preferredSize = Dimension(70, 20)
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 3
        gridBagConstraints.gridy = 1
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(maxCostTF, gridBagConstraints)

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
        setDeadlineB.minimumSize = Dimension(70, 20)
        setDeadlineB.preferredSize = Dimension(70, 20)
        setDeadlineB.addActionListener {
            var d = deadline
            if (d == null) {
                d = Date()
            }
            val tc = TimeChooser(d)
            if (tc.showEditTimeDlg(this@BookBuyerGuiImpl) == TimeChooser.OK) {
                deadline = tc.date
                deadlineTF.text = deadline.toString()
            }
        }
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 3
        gridBagConstraints.gridy = 2
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(setDeadlineB, gridBagConstraints)

        setCCB = JButton("Set CreditCard")
        setCCB.addActionListener {
            val cc = JOptionPane.showInputDialog(this@BookBuyerGuiImpl, "Insert the Credit Card number")
            if (cc != null && cc.isNotEmpty()) {
                myAgent!!.setCreditCard(cc)
            } else {
                JOptionPane.showMessageDialog(
                    this@BookBuyerGuiImpl,
                    "Invalid Credit Card number",
                    "WARNING",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }
        //setCCB.setMinimumSize(new Dimension(70, 20));
//setCCB.setPreferredSize(new Dimension(70, 20));
        gridBagConstraints = GridBagConstraints()
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 3
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
        gridBagConstraints.insets = Insets(5, 3, 0, 3)
        rootPanel.add(setCCB, gridBagConstraints)

        rootPanel.setBorder(BevelBorder(BevelBorder.LOWERED))

        contentPane.add(rootPanel, BorderLayout.NORTH)

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
        buyB = JButton("Buy")
        buyB.addActionListener {
            val title = titleTF.getText()
            var maxCost = -1
            if (title != null && title.isNotEmpty()) {
                if (deadline != null && deadline!!.time > System.currentTimeMillis()) {
                    try {
                        //desiredCost = Integer.parseInt(desiredCostTF.getText());
                        try {
                            maxCost = maxCostTF.getText().toInt()
                            // if (maxCost >= desiredCost) {
                            // myAgent.purchase(title, desiredCost, maxCost, deadline.getTime());
                            myAgent!!.purchase(title, maxCost, deadline!!)
                            notifyUser("PUT FOR BUY: $title at max $maxCost by $deadline")
                            //}
                            //else {
                            // Max cost < desiredCost
                            //JOptionPane.showMessageDialog(BookBuyerGuiImpl.this, "Max cost must be greater than best cost", "WARNING", JOptionPane.WARNING_MESSAGE);
                            //}
                        } catch (ex1: Exception) {
                            // Invalid max cost
                            JOptionPane.showMessageDialog(
                                this@BookBuyerGuiImpl,
                                "Invalid max cost",
                                "WARNING",
                                JOptionPane.WARNING_MESSAGE
                            )
                        }
                    } catch (ex2: Exception) {
                        // Invalid desired cost
                        JOptionPane.showMessageDialog(
                            this@BookBuyerGuiImpl,
                            "Invalid best cost",
                            "WARNING",
                            JOptionPane.WARNING_MESSAGE
                        )
                    }
                } else {
                    // No deadline specified
                    JOptionPane.showMessageDialog(
                        this@BookBuyerGuiImpl,
                        "Invalid deadline",
                        "WARNING",
                        JOptionPane.WARNING_MESSAGE
                    )
                }
            } else {
                // No book title specified
                JOptionPane.showMessageDialog(
                    this@BookBuyerGuiImpl,
                    "No book title specified",
                    "WARNING",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }
        resetB = JButton("Reset")
        resetB.addActionListener {
            titleTF.text = ""
            desiredCostTF.text = ""
            maxCostTF.text = ""
            deadlineTF.text = ""
            deadline = null
        }
        exitB = JButton("Exit")
        exitB.addActionListener { myAgent!!.doDelete() }

        buyB.preferredSize = resetB.getPreferredSize()
        exitB.preferredSize = resetB.getPreferredSize()

        p.add(buyB)
        p.add(resetB)
        p.add(exitB)

        p.setBorder(BevelBorder(BevelBorder.LOWERED))
        contentPane.add(p, BorderLayout.SOUTH)

        pack()

        setResizable(false)
    }

    override fun setAgent(a: BookBuyerAgent) {
        myAgent = a
        setTitle(myAgent!!.name)
    }

    override fun notifyUser(message: String) {
        logTA.append(message + "\n")
    }
}