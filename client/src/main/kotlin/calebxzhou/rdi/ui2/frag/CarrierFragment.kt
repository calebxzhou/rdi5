package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.ui2.center
import calebxzhou.rdi.ui2.radioButton
import calebxzhou.rdi.ui2.radioGroup
import icyllis.modernui.widget.LinearLayout

class CarrierFragment : RFragment("选择运营商节点") {
    private val creds = LocalCredentials.read()
    private val carriers = arrayListOf("电信", "移动", "联通", "教育网", "广电")
    override var contentLayoutInit: LinearLayout.() -> Unit = {
        radioGroup {
            center()
            carriers.forEachIndexed { i, c ->
                radioButton(c) {
                    id = i
                    isSelected = creds.carrier == i
                }
            }
            check(creds.carrier)
            setOnCheckedChangeListener { g, id ->
                creds.carrier = id
                creds.save()
            }
        }
    }
}