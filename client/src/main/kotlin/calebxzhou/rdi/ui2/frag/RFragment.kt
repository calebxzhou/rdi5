package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui.screen.LoadingScreen
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.frameLayout
import calebxzhou.rdi.ui2.iconDrawable
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.textView
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
import icyllis.modernui.widget.LinearLayout
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class RFragment(var title: String = "") : Fragment() {
    protected val background: CoroutineScope = CoroutineScope(Dispatchers.IO)
    open var showTitle = true
    open var closable = true
    open var showCloseButton = closable
    protected lateinit var contentLayout: LinearLayout
    private var _contentView: View? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: DataSet?) =
        _contentView ?: linearLayout(fctx) {
            contentLayout = this
            orientation = LinearLayout.VERTICAL
            paddingDp(16)
            // Create frame layout for back button and title
            if (showTitle || showCloseButton) {
                this += frameLayout(fctx) {
                    layoutParams = linearLayoutParam {
                        bottomMargin = dp(32f)
                    }
                    // Title (add first to be in the background)
                    if (showTitle) {
                        this += textView(fctx) {
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

                        this += button(fctx) {
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
        }.also { _contentView = it }
    fun close(){
        //UIManager.getInstance().onBackPressedDispatcher.onBackPressed()
        //parentFragmentManager.popBackStack()
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

    abstract fun initContent()

}