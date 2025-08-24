package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.model.HwSpec
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.textView
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
            textView("型号: ${spec.model}")
            textView("操作系统: ${spec.os}")
            textView("处理器: ${spec.cpu.name}")
            textView("核心/线程: ${spec.cpu.cores}/${spec.cpu.threads}")
            textView("频率: ${spec.cpu.frequency / 1000000} MHz")
            textView("显卡:")
            spec.gpus.forEach {
                textView("  ${it.name} - ${it.vram / 1024.0.pow(3.0).roundToInt()} GB")
            }
            textView("内存:")
            spec.mems.forEach {
                textView("  ${it.size / 1024.0.pow(3.0).roundToInt()} GB ${it.type} ${it.speed / 1000000}MHz")
            }
            textView("硬盘:")
            spec.disk.forEach {
                textView("  ${it.model} - ${it.size / 1024.0.pow(3.0).roundToInt()} GB")
            }
            textView("显示器:")
            spec.display.forEach {
                textView("  ${it.name} - ${it.width}cm x ${it.height}cm")
            }
            textView("可用视频模式:")
            spec.videoMode.forEach {
                textView("  $it")
            }
        }
    }
}
