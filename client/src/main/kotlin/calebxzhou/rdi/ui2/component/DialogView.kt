package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.frag.RFragment
import icyllis.modernui.core.Context
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.view.*
import icyllis.modernui.widget.FrameLayout
import org.lwjgl.util.tinyfd.TinyFileDialogs

fun alertErr(msg: String, parent: RFragment? = nowFragment) {
    if (parent != null) {
        uiThread { DialogView(parent.fctx, msg,msglvl = MessageLevel.ERR) .showOver(parent) }
    }
}

fun alertErrOs(msg: String) {
    TinyFileDialogs.tinyfd_messageBox("RDI错误", msg, "ok", "error", true)
}

fun alertOk(msg: String, parent: RFragment? = nowFragment) {
    if (parent != null) {
        uiThread { DialogView(parent.fctx, msg, msglvl = MessageLevel.OK).apply {  }.showOver(parent) }
    }
}
fun alertWarn(msg: String, parent: RFragment? = nowFragment) {
    if (parent != null) {
        uiThread { DialogView(parent.fctx, msg, msglvl = MessageLevel.WARN).apply {  }.showOver(parent) }
    }
}

fun confirm(
    msg: String,
    parent: RFragment? = nowFragment,
    noText: String = "❎ 否",
    yesText: String = "✅ 是",
    onNo: () -> Unit = {},
    onYes: () -> Unit = {}
) {
    if (parent != null) {
        uiThread {
            DialogView(parent.fctx, msg,type = RDialogType.CONFIRM,
                noText = noText, yesText = yesText, onNo = onNo,onYes=onYes).showOver(parent)
        }
    }
}


enum class RDialogType {
    CONFIRM, ALERT
}

class DialogView(
    context: Context,
    val msg: String,
    var type: RDialogType = RDialogType.ALERT,
    var msglvl: MessageLevel = MessageLevel.INFO,
    var noText: String = "❎ 否",
    var yesText: String = if (type == RDialogType.ALERT) "明白" else "✅ 是",
    val onNo: () -> Unit = { },
    var onYes: () -> Unit = { },
) : FrameLayout(context) {


    private val dialogContainer: FrameLayout
    private var animTimer: java.util.Timer? = null
    private var animTask: java.util.TimerTask? = null
    private var isClosing = false
    private var hasEntered = false
    private var restoredBlur = false

    companion object {
        private var openCount = 0
        private var prevGlobalBlur = false
    }

    init {
        layoutParams = linearLayoutParam(PARENT, PARENT)
        // Dim + (optional) blur backdrop. We only draw dim here; blur is toggled globally via BlurHandler.
        background = object : Drawable() {
            override fun draw(canvas: Canvas) {
                val b = bounds
                val p = Paint.obtain()
                // semi-transparent black dim
                p.setRGBA(0, 0, 0, 140)
                p.style = Paint.Style.FILL.ordinal
                canvas.drawRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), p)
                p.recycle()
            }
        }
        dialogContainer = FrameLayout(context).apply {
            paddingDp(16)
            // Use FrameLayout.LayoutParams with CENTER gravity for reliable centering
            layoutParams = frameLayoutParam(dp(480f), dp(320f)).apply { gravity = Gravity.CENTER }
            background = drawable { canvas ->
                val b = bounds
                val fill = Paint.obtain()
                fill.setRGBA(16, 16, 16, 230)
                fill.style = Paint.Style.FILL.ordinal
                canvas.drawRoundRect(
                    b.left.toFloat(),
                    b.top.toFloat(),
                    b.right.toFloat(),
                    b.bottom.toFloat(),
                    64f,
                    fill
                )
                fill.recycle();
            }

        }
        addView(dialogContainer)

        // Title
        dialogContainer.textView {
            text = msglvl.msg
            // Colored title bar background using level.color
            background = drawable { canvas ->
                val b = bounds
                val p = Paint.obtain()
                val c = msglvl.color.colorValue
                // Extract ARGB from packed int (assuming AARRGGBB or 0xFFRRGGBB)
                val a = (c ushr 24) and 0xFF
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val bch = c and 0xFF
                p.setRGBA(r, g, bch, if (a==0) 255 else a)
                p.style = Paint.Style.FILL.ordinal
                // Slightly rounded top rectangle spanning full width of container minus padding
                canvas.drawRoundRect(
                    b.left.toFloat(),
                    b.top.toFloat(),
                    b.right.toFloat(),
                    b.bottom.toFloat(),
                    32f,
                    p
                )
                p.recycle()
            }
            paddingDp(8,4,8,4)
            layoutParams = frameLayoutParam { gravity = Gravity.CENTER_HORIZONTAL }
            gravity = Gravity.CENTER_HORIZONTAL
        }
        // Message
        dialogContainer.textView {
            text = msg
            layoutParams = frameLayoutParam { gravity = Gravity.CENTER; topMargin = dp(16f); bottomMargin = dp(16f) }
            gravity = Gravity.CENTER
        }
        // OK button
        dialogContainer.button(
            yesText,
            color = if (type == RDialogType.ALERT) MaterialColor.BLUE_800 else MaterialColor.GREEN_800, init = {

                layoutParams = frameLayoutParam(SELF, SELF) {
                    gravity =
                        if (type == RDialogType.ALERT) Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL else Gravity.BOTTOM or Gravity.END
                    bottomMargin = dp(16f)
                }
            }, onClick = {
                onYes()
                closeWithAnimation()
            })
        // Cancel button for confirm
        if (type == RDialogType.CONFIRM) {
            dialogContainer.button(noText, color = MaterialColor.RED_800, init = {

                layoutParams = frameLayoutParam(SELF, SELF) {
                    gravity = Gravity.BOTTOM or Gravity.START
                    marginStart = dp(16f)
                    bottomMargin = dp(16f)
                }
                setOnClickListener { onNo(); closeWithAnimation() }
            })
        }
    }

    // Back-compat API: show over our custom RFragment by delegating to the generic view-based overload
    fun showOver(parent: RFragment) = showOver(parent.mainLayout)

    // New API: show over any ModernUI Fragment
    fun showOver(fragment: Fragment) {
        val v = fragment.view ?: return
        showOver(v)
    }

    // New API: show over any anchor view in the hierarchy
    fun showOver(anchor: View) = uiThread {
        // Ascend to the topmost root so we center relative to whole screen
        var root: ViewGroup? = when (anchor) {
            is ViewGroup -> anchor
            else -> anchor.parent as? ViewGroup
        }
        var cur: ViewParent? = root?.parent
        while (cur is ViewGroup) {
            root = cur
            cur = cur.parent
        }
        val nonNullRoot = root ?: return@uiThread
        // Manage global blur state (enable while at least one dialog is open)
        if (openCount == 0) {
            prevGlobalBlur = true

        }
        openCount++
        // Add overlay to root
        // Prepare initial invisible state BEFORE first draw to avoid flash (double animation effect)
        if (!hasEntered) {
            dialogContainer.alpha = 0f
            dialogContainer.scaleX = 0.92f
            dialogContainer.scaleY = 0.92f
        }
        nonNullRoot.addView(this, WindowManager.LayoutParams())
        post {
            ensureCentered(nonNullRoot)
            if (!hasEntered) {
                playEnterAnimation()
                hasEntered = true
            }
        }
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> ensureCentered(nonNullRoot) }
    }

    private fun ensureCentered(root: ViewGroup) {
        // If layout params support gravity (FrameLayout), dialogContainer already centered.
        if (dialogContainer.layoutParams is FrameLayout.LayoutParams && root is FrameLayout) return
        val rw = root.width
        val rh = root.height
        val cw = dialogContainer.width
        val ch = dialogContainer.height
        if (rw > 0 && rh > 0 && cw > 0 && ch > 0) {
            dialogContainer.x = ((rw - cw) / 2f).coerceAtLeast(0f)
            dialogContainer.y = ((rh - ch) / 2f).coerceAtLeast(0f)
        }
    }

    private fun playEnterAnimation() {
        cancelAnim()
        // Assume initial alpha/scale already set before first draw
        val duration = 90
        val start = System.currentTimeMillis()
        animTimer = java.util.Timer("DialogIn", true)
        animTask = object : java.util.TimerTask() {
            override fun run() {
                val t = ((System.currentTimeMillis() - start).coerceAtLeast(0)).toFloat() / duration
                val f = easeOutCubic(t.coerceIn(0f, 1f))
                post {
                    dialogContainer.alpha = f
                    val sc = 0.92f + (1f - 0.92f) * f
                    dialogContainer.scaleX = sc
                    dialogContainer.scaleY = sc
                }
                if (t >= 1f) {
                    cancelAnim()
                }
            }
        }
        animTimer?.scheduleAtFixedRate(animTask, 0L, 16L)
    }

    private fun closeWithAnimation() {
        if (isClosing) return
        isClosing = true
        cancelAnim()
        val startAlpha = dialogContainer.alpha
        val startScale = dialogContainer.scaleX
        val duration = 70
        val start = System.currentTimeMillis()
        animTimer = java.util.Timer("DialogOut", true)
        animTask = object : java.util.TimerTask() {
            override fun run() {
                val t = ((System.currentTimeMillis() - start).coerceAtLeast(0)).toFloat() / duration
                val f = (t.coerceIn(0f, 1f))
                val inv = 1f - f
                post {
                    dialogContainer.alpha = startAlpha * inv
                    val sc = 1f - ((1f - 0.92f) * f)
                    dialogContainer.scaleX = sc
                    dialogContainer.scaleY = sc
                }
                if (t >= 1f) {
                    cancelAnim()
                    post {
                        (parent as? ViewGroup)?.removeView(this@DialogView)
                        // Restore blur state if this is the last dialog
                        if (openCount > 0) openCount--
                        if (openCount == 0 && !restoredBlur) {
                            restoredBlur = true
                        }
                    }
                }
            }
        }
        animTimer?.scheduleAtFixedRate(animTask, 0L, 16L)
    }

    private fun cancelAnim() {
        animTask?.cancel(); animTask = null
        animTimer?.cancel(); animTimer = null
    }

    private fun easeOutCubic(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)
}