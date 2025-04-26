package calebxzhou.rdi.ui.general

import calebxzhou.rdi.common.*
import calebxzhou.rdi.ui.RMessageLevel
import calebxzhou.rdi.util.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastComponent
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Mth
import kotlin.math.max


class RToast(val type: RMessageLevel, val msg: String, val time: Int = 5000) : Toast {
    var progress = 0f
    private var lastProgress = 0f
    private var lastProgressTime = 0L

    private val msgLines: MutableList<FormattedCharSequence> = mc.font.split(Component.literal(msg), 150)
    private val width = max(150, msgLines.maxOf(mc.font::width))

    private var timeSinceLastVisible = 0L
    private var visibility = Toast.Visibility.SHOW
    private val bgColor = when(type){
        RMessageLevel.ERR -> LIGHT_RED
        RMessageLevel.WARN -> LIGHT_YELLOW
        RMessageLevel.INFO -> KLEIN_BLUE
        RMessageLevel.OK -> LIGHT_GREEN
    }
    override fun render(
        guiGraphics: GuiGraphics,
        toastComponent: ToastComponent,
        timeSinceLastVisible: Long
    ): Toast.Visibility {

        guiGraphics.fill(3, 10, 157, 33, bgColor);
        msgLines.forEachIndexed { i, str ->
            guiGraphics.drawString(mc.font, str, 18, 18 + i * 12, WHITE)
        }

        if (progress > 0f) {
            renderProgress(guiGraphics, timeSinceLastVisible)
        }
        checkVisibility(timeSinceLastVisible)
        renderDisplayTimeLeft(guiGraphics)

        return visibility
    }

    //下方剩余时间条
    private fun renderDisplayTimeLeft(guiGraphics: GuiGraphics) {
        val prog = 1 - timeSinceLastVisible.toFloat() / time.toFloat()
        var progX = (3 + width * prog).toInt()
        if (progX < 3)
            progX = 3
        val h = height()
        guiGraphics.fill(3, h, progX, h + 1, WHITE)
        guiGraphics.fill(3, h + 1, progX, h + 2, KLEIN_BLUE)
        guiGraphics.fill(3, h + 2, progX, h + 3, WHITE)
    }

    //上方进度条
    private fun renderProgress(guiGraphics: GuiGraphics, timeSinceLastVisible: Long) {
        val prog = Mth.clampedLerp(lastProgress, progress, (timeSinceLastVisible - lastProgressTime).toFloat() / 100.0f)
        val progX = getProgressBarLength(prog).toInt()
        guiGraphics.fill(3, 8, progX, 9, RED)
        lastProgress = prog
        lastProgressTime = timeSinceLastVisible
    }

    //根据进度获取进度条长度
    private fun getProgressBarLength(progress: Float): Float {
        return 3f + 154f * progress
    }

    //检查一下到没到时间，到时间就不显示了
    private fun checkVisibility(timeSinceLastVisible: Long) {
        this.timeSinceLastVisible = timeSinceLastVisible
        visibility = if (timeSinceLastVisible < time) {
            Toast.Visibility.SHOW
        } else {
            Toast.Visibility.HIDE
        }
    }


    override fun width(): Int {
        return width
    }

    override fun height(): Int {
        return 20 + msgLines.size * 12
    }

}
