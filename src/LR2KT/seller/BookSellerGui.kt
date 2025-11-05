package LR2KT.seller


interface BookSellerGui {
    fun setAgent(a: BookSellerAgent)
    fun show()
    fun hide()
    fun notifyUser(message: String?)
    fun dispose()
}