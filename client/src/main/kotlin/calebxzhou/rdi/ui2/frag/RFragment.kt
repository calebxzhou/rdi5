package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.component.RButton
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.KeyEvent
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.CheckBox
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView

abstract class RFragment(initialTitle: String = "") : Fragment() {
    companion object {
        const val mainViewId = 66
        const val titleViewId = 67
        const val titleTextId = 68
    }

    var title: String = initialTitle
        set(value) {
            uiThread {
                field = value
                if (!showTitle) return@uiThread
                val titleView = view?.findViewById<TextView>(titleTextId)
                    ?: _mainView?.findViewById<TextView>(titleTextId)
                    ?: if (this::mainLayout.isInitialized) mainLayout.findViewById<TextView>(titleTextId) else null
                titleView?.text = value
            }
        }
    open var showTitle = true
    open var showBg = true
    open var closable = true
    open var showCloseButton = closable
    open var fragSize: FragmentSize = FragmentSize.FULL


    //true则缓存content view布局，fragment切换时，保存状态不重新计算，false反之
    open var mainViewCache = true
    lateinit var mainLayout: LinearLayout
    lateinit var contentView: LinearLayout
    lateinit var titleView: LinearLayout
    private var _mainView: View? = null
    open var contentViewInit: LinearLayout.() -> Unit = {}
    open var titleViewInit: LinearLayout.() -> Unit = {}

    // If this fragment is displayed as an overlay child, this remover will be set so close() dismisses only the overlay.
    internal var overlayRemover: (() -> Unit)? = null

    // Bottom options configuration - if null, no bottom options will be rendered
    open var bottomOptionsConfig: (QuickOptionsBuilder.() -> Unit)? = null

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

    private fun reloadFragment() {
        uiThread {
            try {
                onReload()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Called when the fragment should reload its UI (debug builds via F5 by default).
     * Default implementation tries to recreate the fragment instance and navigate to it without
     * disturbing the back stack. If recreation fails (for example, due to missing no-arg
     * constructor), it falls back to rebuilding the existing content layout.
     */
    protected open fun onReload() {
        if (!::contentView.isInitialized) {
            return
        }
        contentView.apply {
            removeAllViews()
            contentViewInit()
            requestLayout()
            invalidate()
        }
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
            if (showBg) {
                background = BG_IMAGE_MUI
            }

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

                 frameLayout {
                    layoutParams = frameLayoutParam {
                        bottomMargin = dp(4f)
                    }
                    linearLayout {
                        layoutParams =  frameLayoutParam (SELF,PARENT){
                            gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        }
                        if (showCloseButton) {
                            button(init = {
                                background = iconDrawable("back")
                                layoutParams = frameLayoutParam(dp(32f), dp(32f)) {
                                }
                                setOnClickListener {
                                    close()
                                }
                            })
                        }
                        if (showTitle) {
                            textView {
                                id = titleTextId
                                setTextColor(0xffffffff.toInt())
                                text = title
                                textSize = 20f
                                paddingDp(16,0,0,0)
                                setOnClickListener {
                                    reloadFragment()
                                }
                            }
                        }
                    }
                     titleView = linearLayout {
                         layoutParams = frameLayoutParam (SELF,PARENT){
                            gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        }
                         id = titleViewId

                         titleViewInit()
                    }
                }

                mainLayout = this

                contentView = linearLayout {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = linearLayoutParam(PARENT, 0) {
                        weight = 1f
                    }
                    gravity = Gravity.CENTER_HORIZONTAL
                    minimumWidth = dp(400f)
                }

                contentView.apply {
                    contentViewInit()
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


    //底部一堆按钮 - 使用connected button styling
    fun ViewGroup.bottomOptions(
        config: QuickOptionsBuilder.() -> Unit
    ) = quickOptions(true,config)

    fun ViewGroup.quickOptions(
        bottom: Boolean= false,
        config: QuickOptionsBuilder.() -> Unit
    ) {
        val builder = QuickOptionsBuilder()
        builder.config()
        val options = builder.buildOptions()

        if (options.isEmpty()) return

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = when (this@quickOptions) {
                is FrameLayout -> frameLayoutParam(PARENT, SELF) {
                    if(bottom) gravity = Gravity.BOTTOM
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
        options.forEachIndexed { index, option ->
            when (option.type) {
                OptionType.BUTTON -> {
                    val button = RButton(context, color = option.color ?: MaterialColor.WHITE) {
                        option.buttonHandler?.invoke()
                    }.apply {
                        text = option.text
                    }
                    option.buttonInit?.invoke(button)
                    button.layoutParams = LinearLayout.LayoutParams(SELF, SELF).apply {
                        if (index > 0) leftMargin = context.dp(12f)
                    }
                    buttonRow.addView(button)
                }

                OptionType.CHECKBOX -> {
                    val checkBox = CheckBox(context).apply {
                        text = option.text
                        option.initialChecked?.let { isChecked = it }
                        option.color?.let { setTextColor(it.colorValue) }
                    }
                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        option.checkboxHandler?.invoke(isChecked)
                    }
                    option.checkboxInit?.invoke(checkBox)
                    checkBox.layoutParams = LinearLayout.LayoutParams(SELF, SELF).apply {
                        if (index > 0) leftMargin = context.dp(12f)
                    }
                    buttonRow.addView(checkBox)
                }
            }
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

    class QuickOptionsBuilder {
        private val options = mutableListOf<OptionData>()

    val checkbox = OptionKind { label -> CheckboxDraft(label) }
    val button = OptionKind { label -> ButtonDraft(label) }

        infix fun String.with(handler: () -> Unit) {
            ButtonDraft(this).with(handler)
        }

        infix fun String.colored(color: MaterialColor): ButtonDraft = ButtonDraft(this).colored(color)

        infix fun String.init(block: RButton.() -> Unit): ButtonDraft = ButtonDraft(this).init(block)

        infix fun <T : OptionDraft> String.make(kind: OptionKind<T>): T = kind.factory.invoke(this@QuickOptionsBuilder, this)

        private fun addOption(option: OptionData) {
            options += option
        }

        internal fun buildOptions(): List<OptionData> = options

        interface OptionDraft

        inner class ButtonDraft internal constructor(private val label: String) : OptionDraft {
            private var color: MaterialColor? = null
            private var initBlock: (RButton.() -> Unit)? = null

            fun init(block: RButton.() -> Unit): ButtonDraft = apply { initBlock = block }

            infix fun colored(color: MaterialColor): ButtonDraft = apply { this.color = color }

            infix fun with(handler: () -> Unit) {
                addOption(
                    OptionData(
                        text = label,
                        type = OptionType.BUTTON,
                        color = color,
                        buttonInit = initBlock,
                        checkboxInit = null,
                        buttonHandler = handler,
                        checkboxHandler = null,
                        initialChecked = null
                    )
                )
            }
        }

        inner class CheckboxDraft internal constructor(private val label: String) : OptionDraft {
            private var color: MaterialColor? = null
            private var initBlock: (CheckBox.() -> Unit)? = null
            private var defaultChecked: Boolean? = null

            fun init(block: CheckBox.() -> Unit): CheckboxDraft = apply { initBlock = block }

            infix fun colored(color: MaterialColor): CheckboxDraft = apply { this.color = color }

            fun checked(default: Boolean = true): CheckboxDraft = apply { defaultChecked = default }

            infix fun with(handler: (Boolean) -> Unit) {
                addOption(
                    OptionData(
                        text = label,
                        type = OptionType.CHECKBOX,
                        color = color,
                        buttonInit = null,
                        checkboxInit = initBlock,
                        buttonHandler = null,
                        checkboxHandler = handler,
                        initialChecked = defaultChecked
                    )
                )
            }
        }

        class OptionKind<T : OptionDraft> internal constructor(
            internal val factory: QuickOptionsBuilder.(String) -> T
        )
    }

    data class OptionData(
        val text: String,
        val type: OptionType,
        val color: MaterialColor?,
        val buttonInit: (RButton.() -> Unit)?,
        val checkboxInit: (CheckBox.() -> Unit)?,
        val buttonHandler: (() -> Unit)?,
        val checkboxHandler: ((Boolean) -> Unit)?,
        val initialChecked: Boolean?
    )

    enum class OptionType {
        BUTTON,
        CHECKBOX,
    }

}
