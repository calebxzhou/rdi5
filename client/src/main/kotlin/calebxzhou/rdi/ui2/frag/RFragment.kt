package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.component.RButton
import icyllis.modernui.ModernUI
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.KeyEvent
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.Button
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView

abstract class RFragment(initialTitle: String = "") : Fragment() {
    var title: String = initialTitle
        set(value) {
            uiThread {
                field = value
                if (!showTitle) return@uiThread
                val titleView = view?.findViewById<TextView>(titleViewId)
                    ?: _mainView?.findViewById<TextView>(titleViewId)
                    ?: if (this::mainLayout.isInitialized) mainLayout.findViewById<TextView>(titleViewId) else null
                titleView?.text = value
            }
        }
    open var showTitle = true
    open var closable = true
    open var showCloseButton = closable
    open var fragSize: FragmentSize = FragmentSize.FULL
    val mainViewId = 666
    val titleViewId = 667

    //true则缓存content view布局，fragment切换时，保存状态不重新计算，false反之
    open var mainViewCache = true
    lateinit var mainLayout: LinearLayout
    lateinit var contentLayout: LinearLayout
    private var _mainView: View? = null
    open var contentLayoutInit: LinearLayout.() -> Unit = {}

    // If this fragment is displayed as an overlay child, this remover will be set so close() dismisses only the overlay.
    internal var overlayRemover: (() -> Unit)? = null

    // Bottom options configuration - if null, no bottom options will be rendered
    open var bottomOptionsConfig: (BottomOptionsBuilder.() -> Unit)? = null

    private var keyActions: List<Pair<Int, () -> Unit>> = emptyList()

    // DSL: keyAction { KeyEvent.KEY_ENTER { ... } ESCAPE { ... } }
    fun View.keyAction(build: KeyActionBuilder.() -> Unit) {
        keyActions = KeyActionBuilder().apply(build).build()
        setOnKeyListener { v, keyCode, event ->
            if (closable) {
                if (keyCode == KeyEvent.KEY_ESCAPE && event.action == KeyEvent.ACTION_UP) {
                    close()
                    return@setOnKeyListener true
                }
            }
            keyActions.forEach { (key, handler) ->
                if (keyCode == key && event.action == KeyEvent.ACTION_UP) {
                    handler()
                    return@setOnKeyListener true
                }
            }

            false
        }
    }

    class KeyActionBuilder {
        private val actions = mutableListOf<Pair<Int, () -> Unit>>()

        // Allow syntax: KeyEvent.KEY_ENTER { ... }
        operator fun Int.invoke(handler: () -> Unit) {
            actions += this to handler
        }

        // Convenience names
        fun enter(handler: () -> Unit) {
            KeyEvent.KEY_ENTER.invoke(handler)
            KeyEvent.KEY_KP_ENTER.invoke(handler)
        }

        fun esc(handler: () -> Unit) = KeyEvent.KEY_ESCAPE.invoke(handler)

        internal fun build(): List<Pair<Int, () -> Unit>> = actions
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: DataSet?): View {
        // If caching is enabled and we have a cached view, return it
        if (mainViewCache && _mainView != null) {
            return _mainView!!
        }
        val sizeDims = when (fragSize) {
            FragmentSize.FULL -> null
            FragmentSize.SMALL -> 480f to 320f
            FragmentSize.MEDIUM -> 720f to 576f
            FragmentSize.LARGE -> 1280f to 720f
        }
        // Create new content view - use FrameLayout as root to allow bottom positioning
        val root = FrameLayout(mui).apply {

            // 20% dim background for better contrast with light content
            background = BG_IMAGE_MUI

            // Main content in a LinearLayout
            linearLayout {
                id = mainViewId
                orientation = LinearLayout.VERTICAL
                layoutParams = sizeDims?.let { (width, height) ->
                    frameLayoutParam(dp(width), dp(height)) {
                        gravity = Gravity.CENTER
                    }
                } ?: frameLayoutParam(PARENT, PARENT)
                gravity = Gravity.CENTER_HORIZONTAL
                val radius = context.dp(20f).toFloat()
                val fillColor = 0xEE1A1A1A.toInt()
                background = drawable { canvas ->
                    val b = bounds
                    val paint = Paint.obtain()
                    paint.color = fillColor
                    paint.style = Paint.Style.FILL.ordinal
                    canvas.drawRoundRect(
                        b.left.toFloat(),
                        b.top.toFloat(),
                        b.right.toFloat(),
                        b.bottom.toFloat(),
                        radius,
                        paint
                    )
                    paint.recycle()
                }
                paddingDp(12)
                // Create frame layout for back button and title
                if (showTitle || showCloseButton) {
                    frameLayout() {
                        layoutParams = linearLayoutParam {
                            bottomMargin = dp(8f)
                        }
                        // Title (add first to be in the background)
                        if (showTitle) {
                            textView {
                                id = titleViewId
                                setTextColor(0xffffffff.toInt())
                                text = title
                                textSize = 20f
                                layoutParams = linearLayoutParam {
                                    gravity = Gravity.CENTER
                                }
                                gravity = Gravity.CENTER
                            }
                        }
                        // Back button (add last to be on top)
                        if (showCloseButton) {
                            this += Button(ModernUI.getInstance()).apply {
                                paddingDp(4)
                                background = iconDrawable("back")
                                layoutParams = linearLayoutParam(dp(32f), dp(32f)) {
                                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                                }
                                setOnClickListener {
                                    close()
                                }
                            }
                        }
                    }
                } else {
                    initHeader()
                }

                mainLayout = this

                contentLayout = linearLayout {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = linearLayoutParam(PARENT, 0) {
                        weight = 1f
                    }
                    gravity = Gravity.CENTER_HORIZONTAL
                }

                initContent()
                contentLayout.apply {
                    contentLayoutInit()
                }

                if (fragSize != FragmentSize.FULL) {
                    bottomOptionsConfig?.let { config ->
                        bottomOptions(config)
                    }
                }
            }

            // Render bottom options if configured - positioned at actual bottom
            if (fragSize == FragmentSize.FULL) {
                bottomOptionsConfig?.let { config ->
                    bottomOptions(config)
                }
            }

            keyAction { }
        }
        // Store the view in cache if caching is enabled
        if (mainViewCache) {
            _mainView = root
        }


        return root
    }

    // Make this a var so inheritors can either override it or assign in init {}

    fun close() {
        // If this fragment is being shown as an overlay child, prefer dismissing the overlay.
        overlayRemover?.let { remover ->
            overlayRemover = null
            remover()
            return
        }
        goBack()/*
        if (Minecraft.getInstance() == null) {
            UIManager.getInstance().onBackPressedDispatcher.onBackPressed()
            return
        }
        (mc.screen as? MuiScreen)?.let { mc set it.previousScreen }
            ?: UIManager.getInstance().onBackPressedDispatcher.onBackPressed()*/
    }

    //载入标题+返回按钮 和自定义header 二选一
    open fun initHeader() {

    }

    open fun initContent() {
        //只有用到linear layout的fragment才需要写这个，否则直接override onCreateView即可
    }

    //底部一堆按钮 - 使用connected button styling
    private fun ViewGroup.bottomOptions(
        config: BottomOptionsBuilder.() -> Unit
    ) {
        val builder = BottomOptionsBuilder()
        builder.config()
        val buttons = builder.getButtons()

        if (buttons.isEmpty()) return

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = when (this@bottomOptions) {
                is FrameLayout -> frameLayoutParam(PARENT, SELF) {
                    gravity = Gravity.BOTTOM
                    bottomMargin = context.dp(16f)
                }

                is LinearLayout -> linearLayoutParam(SELF, SELF).apply {
                    topMargin = context.dp(16f)
                    gravity = Gravity.CENTER_HORIZONTAL
                }

                else -> ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }

        addView(buttonRow)

        // Create buttons that stick together in material design style
        buttons.forEachIndexed { index, buttonData ->
            val button =
                RButton(context, color = buttonData.color ?: MaterialColor.WHITE) { buttonData.handler() }.apply {

                }
            button.text = buttonData.text
            // Apply optional customizations via init block
            buttonData.init?.invoke(button)

            button.layoutParams = LinearLayout.LayoutParams(SELF, SELF).apply {
                if (index > 0) leftMargin = context.dp(12f)
            }
            buttonRow.addView(button)
        }
    }

    // No longer needed: color luminance helpers moved into MaterialButton

    /**
     * Show any other Fragment as a centered child overlay on top of this RFragment.
     * - Parent is dimmed by a semi-transparent scrim.
     * - Child fragment's view is sized to [widthDp] x [heightDp] (defaults 480x320 dp) and centered.
     * - Returns a function you can call to remove the overlay.
     */
    fun showChildFragmentOver(
        child: Fragment,
        widthDp: Float = 640f,
        heightDp: Float = 480f
    ): () -> Unit {
        val root = (view as? ViewGroup) ?: return {}
        // Fullscreen dim overlay to sit above our content
        val overlay = FrameLayout(mainLayout.context).apply {
            layoutParams = frameLayoutParam(PARENT, PARENT)
            // Dim the background so the child stands out
            background = ColorDrawable(0xBB000000.toInt()) // ~55% opacity black
            isClickable = true   // intercept clicks behind
            isFocusable = true
        }
        // Container for the child fragment's view, centered with fixed dp size
        val childContainer = FrameLayout(mainLayout.context).apply {
            layoutParams = frameLayoutParam(dp(widthDp), dp(heightDp)) {
                gravity = Gravity.CENTER
            }
            background = drawable { canvas ->
                val b = bounds
                val fill = Paint.obtain()
                fill.setRGBA(16, 16, 16, 200)
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
        overlay.addView(childContainer)

        // Ask the fragment to create its view into our container
        val childView = child.onCreateView(layoutInflater, childContainer, null)
        if (childView != null) {
            childContainer.addView(childView)
            child.onViewCreated(childView, null)
        }

        root.addView(overlay)

        // Provide a remover to close/detach the overlay later
        val remover: () -> Unit = {
            (overlay.parent as? ViewGroup)?.removeView(overlay)
            // Notify the child its view is being destroyed if needed
            try {
                child.onDestroyView()
            } catch (_: Throwable) {
            }
        }
        // If child is an RFragment, set its overlayRemover so back/close dismisses only itself
        if (child is RFragment) {
            child.overlayRemover = remover
        }
        return remover
    }

    // Builder class for bottom options DSL - using "with" syntax and colored options

    class BottomOptionsBuilder {
        private val buttons = mutableListOf<ButtonData>()

        infix fun String.with(handler: () -> Unit) {
            buttons.add(ButtonData(this, null, handler, null))
        }

        infix fun String.colored(color: MaterialColor): ColoredButtonBuilder {
            return ColoredButtonBuilder(this, color)
        }

        // Allow customizing the created button via an init block
        infix fun String.init(block: RButton.() -> Unit): InitButtonBuilder {
            return InitButtonBuilder(this, block)
        }

        inner class ColoredButtonBuilder(private val text: String, private val color: MaterialColor) {
            private var initBlock: (RButton.() -> Unit)? = null
            infix fun init(block: RButton.() -> Unit): ColoredButtonBuilder {
                initBlock = block
                return this
            }

            infix fun with(handler: () -> Unit) {
                buttons.add(ButtonData(text, color, handler, initBlock))
            }
        }

        inner class InitButtonBuilder(private val text: String, private val initBlock: RButton.() -> Unit) {
            infix fun with(handler: () -> Unit) {
                buttons.add(ButtonData(text, null, handler, initBlock))
            }

            infix fun colored(color: MaterialColor): ColoredInitButtonBuilder {
                return ColoredInitButtonBuilder(text, color, initBlock)
            }
        }

        inner class ColoredInitButtonBuilder(
            private val text: String,
            private val color: MaterialColor,
            private val initBlock: RButton.() -> Unit
        ) {
            infix fun with(handler: () -> Unit) {
                buttons.add(ButtonData(text, color, handler, initBlock))
            }
        }

        internal fun getButtons() = buttons
    }

    data class ButtonData(
        val text: String,
        val color: MaterialColor?,
        val handler: () -> Unit,
        val init: (RButton.() -> Unit)?
    )

}

/*
Example usage with "with" DSL:

class MyFragment : RFragment("Title") {
    init {
        bottomOptionsConfig = {
            "Save" with { saveData() }
            "Cancel" with { close() }
            "Settings" with { openSettings() }
        }
    }
}
*/
