package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.model.HwSpec
import calebxzhou.rdi.ui2.iconDrawable
import calebxzhou.rdi.ui2.leadingIcon
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.util.toFixed
import com.mojang.datafixers.functions.Functions.comp
import icyllis.modernui.core.Context
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.ScrollView
import kotlin.math.pow
import kotlin.math.roundToInt

class HwSpecView(context: Context) : ScrollView(context) {
    val spec = HwSpec.now

    init {
        val container = linearLayout {
            orientation = LinearLayout.VERTICAL
            paddingDp(8)
        }
        container.run {
            textView(spec.model).apply { leadingIcon("laptop") }
            textView(spec.os)
            textView(spec.cpu.name)  .apply { leadingIcon("cpu") }
            textView("${spec.cpu.cores}c${spec.cpu.threads}t ${(spec.cpu.frequency / 1000000000f).toFixed(2)} GHz")

            spec.gpus.forEach {
                textView("${it.name} ${it.vram / 1024.0.pow(3.0).roundToInt()} GB").apply { leadingIcon("gpu") }
            }
            spec.mems.forEach {
                textView("${it.size / 1024.0.pow(3.0).roundToInt()} GB ${it.type} ${it.speed / 1000000}MHz").apply { leadingIcon("ram") }
            }
            spec.disk.forEach {
                textView("${it.model} ${it.size / 1024.0.pow(3.0).roundToInt()} GB").apply { leadingIcon("hdd") }
            }
            spec.display.forEach {
                textView("${it.name} ${it.width}cm x ${it.height}cm").apply { leadingIcon("monitor") }
            }
            spec.videoMode.forEach {
                textView(it)
            }
        }
    }
}
