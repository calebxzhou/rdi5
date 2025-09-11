package calebxzhou.rdi.ui2

import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.view.MotionEvent
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.TextView
import java.util.*

object HoldToConfirm {
    private data class State(
        var enabled: Boolean = false,
        var thresholdMs: Long = 500,
        var showTooltip: Boolean = true,
        var tooltipIconName: String? = "lmb",
        var tooltipFormatter: (Long) -> String = { ms ->
            val s = (ms.coerceAtLeast(0) / 100) / 10.0
            "⏰${String.format("%.1f", s)}s"
        },
        var onConfirm: ((View) -> Unit)? = null,
        var onShortClick: ((View) -> Unit)? = null,
        var onTick: ((View, Long, Float) -> Unit)? = null,

        var pressing: Boolean = false,
        var canceledByExit: Boolean = false,
        var pressStart: Long = 0,
        var tickTimer: Timer? = null,
        var tickTask: TimerTask? = null,
        var label: TextView? = null
    )

    private val states = WeakHashMap<View, State>()

    // Extension properties on View for tooltip configuration
    var View.holdTooltipEnabled: Boolean
        get() = states[this]?.showTooltip ?: true
        set(value) {
            val st = states.getOrPut(this) { State() }
            st.showTooltip = value
            // Reflect change immediately if mid-press
            if (!value && st.label != null) hideLabel(st)
            if (value && st.pressing && st.label == null) showLabel(this, st)
        }

    var View.holdTooltipFormatter: (Long) -> String
        get() = states[this]?.tooltipFormatter ?: { ms ->
            val s = (ms.coerceAtLeast(0) / 100) / 10.0
            "⏰${String.format("%.1f", s)}s"
        }
        set(value) {
            val st = states.getOrPut(this) { State() }
            st.tooltipFormatter = value
        }

    fun View.enableHoldToConfirm(
        thresholdMs: Long = 500,
        showTooltip: Boolean = true,
        tooltipIconName: String? = "lmb",
        tooltipFormatter: (Long) -> String = { ms ->
            val s = (ms.coerceAtLeast(0) / 100) / 10.0
            "⏰${String.format("%.1f", s)}s"
        },
        onConfirm: (View) -> Unit,
        onShortClick: ((View) -> Unit)? = null,
        onTick: ((View, Long, Float) -> Unit)? = null,
    ) {
        val st = states.getOrPut(this) { State() }
        st.enabled = true
        st.thresholdMs = thresholdMs
        st.showTooltip = showTooltip
        st.tooltipIconName = tooltipIconName
        st.tooltipFormatter = tooltipFormatter
        st.onConfirm = onConfirm
        st.onShortClick = onShortClick
        st.onTick = onTick

        setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    st.pressing = true
                    st.canceledByExit = false
                    st.pressStart = System.currentTimeMillis()
                    if (st.showTooltip && st.thresholdMs > 0) {
                        scheduleTicks(v, st)
                        showLabel(v, st)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (st.pressing) {
                        val inside = ev.x >= 0 && ev.y >= 0 && ev.x < v.width && ev.y < v.height
                        if (!inside) {
                            st.pressing = false
                            st.canceledByExit = true
                            cancelTicks(st)
                            hideLabel(st)
                            true
                        } else {
                            if (st.label != null) updateLabelPos(v, st)
                            true
                        }
                    } else false
                }
                MotionEvent.ACTION_UP -> {
                    if (st.pressing) {
                        val duration = System.currentTimeMillis() - st.pressStart
                        st.pressing = false
                        cancelTicks(st)
                        hideLabel(st)
                        if (duration >= st.thresholdMs) {
                            st.onConfirm?.invoke(v)
                            true
                        } else {
                            if (st.onShortClick != null) {
                                st.onShortClick?.invoke(v)
                                true
                            } else {
                                false
                            }
                        }
                    } else if (st.canceledByExit) {
                        st.canceledByExit = false
                        true
                    } else false
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (st.pressing || st.canceledByExit) {
                        st.pressing = false
                        st.canceledByExit = false
                        cancelTicks(st)
                        hideLabel(st)
                        true
                    } else false
                }
                else -> false
            }
        }
    }

    // Convenience API: View.onLongPress(millis) { ... }
    fun View.onLongPress(
        millis: Long,
        onShortClick: ((View) -> Unit)? = null,
        onConfirm: (View) -> Unit,
    ) {
        enableHoldToConfirm(
            thresholdMs = millis,
            showTooltip = holdTooltipEnabled,
            tooltipIconName = "lmb",
            tooltipFormatter = holdTooltipFormatter,
            onConfirm = onConfirm,
            onShortClick = onShortClick,
            onTick = null
        )
    }

    fun View.clearHoldToConfirm() {
        disableHoldToConfirm()
    }

    fun View.disableHoldToConfirm() {
        val st = states[this] ?: return
        st.enabled = false
        st.pressing = false
        st.canceledByExit = false
        cancelTicks(st)
        hideLabel(st)
        setOnTouchListener(null)
        states.remove(this)
    }

    private fun scheduleTicks(v: View, st: State) {
        cancelTicks(st)
        val th = st.thresholdMs
        if (th <= 0L) return
        st.tickTimer = Timer("HoldToConfirmTick", true)
        st.tickTask = object : TimerTask() {
            override fun run() {
                if (!st.pressing) return
                val elapsed = System.currentTimeMillis() - st.pressStart
                val remaining = (th - elapsed).coerceAtLeast(0)
                val fraction = (elapsed.toFloat() / th).coerceIn(0f, 1f)
                st.onTick?.let { cb -> uiThread { cb(v, remaining, fraction) } }
                if (st.showTooltip) {
                    val text = if (remaining <= 0L) "松开" else st.tooltipFormatter(remaining)
                    uiThread { updateLabelText(v, st, text) }
                }
            }
        }
        st.tickTimer?.scheduleAtFixedRate(st.tickTask, 0L, 100L)
    }

    private fun cancelTicks(st: State) {
        st.tickTask?.cancel(); st.tickTask = null
        st.tickTimer?.cancel(); st.tickTimer = null
    }

    private fun showLabel(v: View, st: State) {
        if (!st.showTooltip || st.label != null) return
        val root = findOverlayRootFrame(v) ?: return
        val tv = TextView(v.context).apply {
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            background = object : Drawable() {
                override fun draw(canvas: Canvas) {
                    val paint = Paint.obtain()
                    paint.color = 0xCC000000.toInt()
                    paint.style = Paint.Style.FILL.ordinal
                    val r = v.dp(6f).toFloat()
                    canvas.drawRoundRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), r, paint)
                    paint.recycle()
                }
            }
            setPadding(v.dp(8f), v.dp(4f), v.dp(8f), v.dp(4f))
        }
        st.tooltipIconName?.let { name ->
            val icon = iconDrawable(name)
            icon.bounds.set(0, 0, v.dp(14f), v.dp(14f))
            tv.setCompoundDrawables(icon, null, null, null)
            tv.compoundDrawablePadding = v.dp(6f)
        }
        val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        root.addView(tv, lp)
        st.label = tv
        updateLabelPos(v, st)
    }

    private fun updateLabelText(v: View, st: State, text: String) {
        st.label?.text = text
        updateLabelPos(v, st)
    }

    private fun hideLabel(st: State) {
        val parent = st.label?.parent as? ViewGroup
        if (parent != null && st.label != null) parent.removeView(st.label)
        st.label = null
    }

    private fun updateLabelPos(v: View, st: State) {
        val tv = st.label ?: return
        val root = tv.parent as? FrameLayout ?: return
        val (x, y) = computePositionIn(v, root)
        val lp = tv.layoutParams as FrameLayout.LayoutParams
        lp.leftMargin = x + v.width + v.dp(8f)
        val approxH = if (tv.height > 0) tv.height else v.dp(20f)
        lp.topMargin = y + (v.height - approxH) / 2
        tv.layoutParams = lp
    }

    private fun findOverlayRootFrame(v: View): FrameLayout? {
        var p = v.parent
        while (p is ViewGroup) {
            if (p is FrameLayout) return p
            p = p.parent
        }
        return null
    }

    private fun computePositionIn(v: View, root: ViewGroup): Pair<Int, Int> {
        var vx: Int = v.left
        var vy: Int = v.top
        var p = v.parent
        while (p is ViewGroup && p !== root) {
            vx += p.left - p.scrollX
            vy += p.top - p.scrollY
            p = p.parent
        }
        return Pair(vx, vy)
    }
}

