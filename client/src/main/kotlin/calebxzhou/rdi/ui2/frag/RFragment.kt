package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.set
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.mc.MuiScreen
import icyllis.modernui.mc.UIManager
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.Button
import icyllis.modernui.widget.LinearLayout

abstract class RFragment(var title: String = "") : Fragment() {
    open var showTitle = true
    open var closable = true
    open var showCloseButton = closable
    //true则缓存content view布局，fragment切换时，保存状态不重新计算，false反之
    open var contentViewCache = true
    protected lateinit var contentLayout: LinearLayout
    private var _contentView: View? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: DataSet?): View {
        // If caching is enabled and we have a cached view, return it
        if (contentViewCache && _contentView != null) {
            return _contentView!!
        }

        // Create new content view
        return LinearLayout(fctx).apply {
            contentLayout = this
            orientation = LinearLayout.VERTICAL
            paddingDp(16)
            // Create frame layout for back button and title
            if (showTitle || showCloseButton) {
                 frameLayout() {
                    layoutParams = linearLayoutParam {
                        bottomMargin = dp(32f)
                    }
                    // Title (add first to be in the background)
                    if (showTitle) {
                         textView() {
                            text = title
                            textSize = 24f
                            layoutParams = linearLayoutParam {
                                gravity = Gravity.CENTER
                            }
                            gravity = Gravity.CENTER
                        }
                    }
                    // Back button (add last to be on top)
                    if (showCloseButton) {

                         this+=Button(fctx).apply {
                            background = iconDrawable("back")
                            layoutParams = linearLayoutParam(dp(32f), dp(32f)) {
                                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                            }
                            setOnClickListener {
                                close()
                            //parentFragmentManager.popBackStack()
                            }
                        }
                    }
                }
            } else {
                initHeader()
            }
            initContent()
            // Store the view in cache if caching is enabled
            if (contentViewCache) {
                _contentView = this
            }
        }
    }
    open fun close(){
        //UIManager.getInstance().onBackPressedDispatcher.onBackPressed()

        val screen = mc.screen

        if(screen is MuiScreen){
            mc set screen.previousScreen
        }else{
            UIManager.getInstance().onBackPressedDispatcher.onBackPressed()
        }
    }
    //载入标题+返回按钮 和自定义header 二选一
    open fun initHeader() {

    }

    open fun initContent() {
        //只有用到linear layout的fragment才需要写这个，否则直接override onCreateView即可
    }

}