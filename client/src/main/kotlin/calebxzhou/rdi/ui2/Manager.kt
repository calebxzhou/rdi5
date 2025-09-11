package calebxzhou.rdi.ui2

import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.set
import icyllis.modernui.ModernUI
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.fragment.FragmentContainerView
import icyllis.modernui.fragment.FragmentController
import icyllis.modernui.fragment.FragmentTransaction
import icyllis.modernui.mc.MuiModApi
import icyllis.modernui.mc.MuiScreen
import icyllis.modernui.mc.ScreenCallback
import icyllis.modernui.mc.UIManager
import icyllis.modernui.mc.neoforge.MuiForgeApi
import icyllis.modernui.mc.neoforge.UIManagerForge
import net.minecraft.client.gui.screens.Screen
import org.apache.commons.lang3.reflect.FieldUtils.getDeclaredField
import kotlin.jvm.javaClass


var prevFragment: Fragment? = null
val FRAG_CTRL
    get() = ModernUI::class.java.getDeclaredField("mFragmentController").also { it.isAccessible=true }.get(ModernUI.getInstance()) as FragmentController
fun goto(fragment: Fragment) {

    if (isMcStarted) {
        prevFragment = mc.fragment
        val screen: Screen = MuiForgeApi.get().createScreen(fragment, object : ScreenCallback {
            override fun shouldClose(): Boolean {
                return false
            }
        }, mc.screen)
        mc set screen
    }else{/*
        prevFragment = (ModernUI::class.java.getDeclaredField("mFragmentContainerView").also { it.isAccessible = true }
            .get(ModernUI.getInstance()) as FragmentContainerView).*/
        FRAG_CTRL.fragmentManager.transaction{
            replace(0x01020007,fragment,"main")
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
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
