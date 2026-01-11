package calebxzhou.rdi.client.ui

import calebxzhou.rdi.client.ui.frag.RFragment
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.fragment.FragmentTransaction

val nowFragment
    get() = FRAG_CTRL.fragmentManager.findFragmentById(FRAG_CONTAINER_ID) as? RFragment

/**
 * @see icyllis.modernui.ModernUI.fragment_container
 */
val FRAG_CONTAINER_ID = 0x01020007
val FRAG_CTRL
    get() = RodernUI.getInstance().fragmentController


fun Fragment.go(changePrev: Boolean = true) = goto(this, changePrev)
fun Fragment.showOver(parent: RFragment, w: Float = 640f, h: Float = 480f) = parent.showChildFragmentOver(this, w, h)
fun Fragment.refresh() {
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

//changePrev 记录上个fragment false则不记录（刷新当前frag用）
fun goto(fragment: Fragment, changePrev: Boolean = true) = uiThread{

    FRAG_CTRL.fragmentManager.transaction {
        replace(FRAG_CONTAINER_ID, fragment, "main")
        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        if (changePrev)
            addToBackStack("main")
    }

}

fun RFragment.goBack() {
    FRAG_CTRL.fragmentManager.popBackStack()

}
