package calebxzhou.rdi.client.ui.frag

import calebxzhou.rdi.client.auth.LocalCredentials
import calebxzhou.rdi.client.ui.FragmentSize
import calebxzhou.rdi.client.ui.center
import calebxzhou.rdi.client.ui.radioButton
import calebxzhou.rdi.client.ui.radioGroup
import icyllis.modernui.widget.LinearLayout

class CarrierFragment : RFragment("选择运营商节点") {
    override var fragSize = FragmentSize.SMALL
    private val creds = LocalCredentials.read()
    private val carriers = arrayListOf("电信", "移动", "联通", "教育网", "广电")
    override var contentViewInit: LinearLayout.() -> Unit = {
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