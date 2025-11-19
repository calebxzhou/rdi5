package calebxzhou.rdi.ui2

import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.set
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.fragment.FragmentTransaction
import icyllis.modernui.mc.MuiScreen
import icyllis.modernui.mc.ScreenCallback
import icyllis.modernui.mc.UIManager
import icyllis.modernui.mc.neoforge.MuiForgeApi
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

val Minecraft.fragment
    get() = (mc.screen as? MuiScreen)?.fragment as? RFragment
var prevFragment: Fragment? = null
val nowFragment
    get() =
        if (isMcStarted)
            mc.fragment
        else
            FRAG_CTRL.fragmentManager.findFragmentById(FRAG_CONTAINER_ID) as? RFragment

/**
 * @see icyllis.modernui.ModernUI.fragment_container
 */
val FRAG_CONTAINER_ID = 0x01020007
val FRAG_CTRL
    get() = RodernUI.getInstance().fragmentController  /*ModernUI::class.java.getDeclaredField("mFragmentController").also { it.isAccessible = true }
        .get(ModernUI.getInstance()) as FragmentController*/
val SCREEN_CALLBACK = object : ScreenCallback {
    override fun shouldClose(): Boolean = false
}

fun Fragment.go(changePrev: Boolean = true) = goto(this,changePrev)
fun Fragment.showOver(parent: RFragment, w: Float = 640f, h: Float = 480f) = parent.showChildFragmentOver(this, w, h)
fun Fragment.refresh() {
    if (isMcStarted) {
        // In MC, rebuild the MuiScreen for this fragment
        val screen: Screen = MuiForgeApi.get().createScreen(this, SCREEN_CALLBACK, mc.screen)
        mc set screen
    } else {
        // In standalone ModernUI, force re-create the fragment's view by detach/attach
        val fm = FRAG_CTRL.fragmentManager
        try {
            fm.transaction {
                // Only detach/attach if the fragment is currently added, otherwise navigate to it
                if (this@refresh.isAdded) {
                    detach(this@refresh)
                    attach(this@refresh)
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                } else {
                    replace(FRAG_CONTAINER_ID, this@refresh, "main")
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    addToBackStack("main")
                }
            }
        } catch (_: Throwable) {
            // Fallback: navigate to ensure a visible refresh
            goto(this)
        }
    }
}

//changePrev 记录上个fragment false则不记录（刷新当前frag用）
fun goto(fragment: Fragment, changePrev: Boolean = true) {
    if (isMcStarted) {
        if (changePrev)
            prevFragment = mc.fragment
        val screen: Screen = MuiForgeApi.get().createScreen(fragment, SCREEN_CALLBACK, mc.screen)
        mc set screen
    } else {
        FRAG_CTRL.fragmentManager.transaction {
            replace(FRAG_CONTAINER_ID, fragment, "main")
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            if (changePrev)
                addToBackStack("main")
        }
    }
}

fun RFragment.goBack() {
    if (isMcStarted) {
        (mc.screen as? MuiScreen)?.let { mc set it.previousScreen }
            ?: UIManager.getInstance().onBackPressedDispatcher.onBackPressed()
    } else {
        FRAG_CTRL.fragmentManager.popBackStack()
    }
}
