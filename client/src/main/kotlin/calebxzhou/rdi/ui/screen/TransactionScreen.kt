package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.ui.component.RScreen

//事务日志
class TransactionScreen(
    val name: String,
    val exec: TransactionScreen.() -> Unit
) : RScreen("正在$name") {

}