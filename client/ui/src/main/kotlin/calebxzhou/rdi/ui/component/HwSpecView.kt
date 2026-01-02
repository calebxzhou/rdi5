package calebxzhou.rdi.ui.component

import calebxzhou.mykotutils.hwspec.HwSpec
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.rdi.ui.*
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.ScrollView
import icyllis.modernui.widget.TextView
import kotlin.math.pow
import kotlin.math.roundToInt

class HwSpecView(context: Context) : ScrollView(context) {
    val spec = HwSpec.get()

    init {
    // 50% dim background to improve readability
    background = ColorDrawable(0x80000000.toInt())

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

        // Apply code typeface to all TextViews without modifying each creation site
        applyTypefaceRecursive(container, Fonts.CODE)
    }

    private fun applyTypefaceRecursive(view: View, font: Fonts) {
        when (view) {
            is TextView -> view.typeface = font.typeface
            is ViewGroup -> {
                val count = view.childCount
                for (i in 0 until count) {
                    applyTypefaceRecursive(view.getChildAt(i), font)
                }
            }
        }
    }
}
