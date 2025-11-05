package LR2KT.buyer

interface BookBuyerGui {
    fun setAgent(bookBuyerAgent: BookBuyerAgent)

    fun show()

    fun hide()

    fun notifyUser(message: String)

    fun dispose()
}