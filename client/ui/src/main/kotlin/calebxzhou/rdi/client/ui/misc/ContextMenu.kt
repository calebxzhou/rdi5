package calebxzhou.rdi.client.ui.misc

import icyllis.modernui.view.View

/**
 * Simple DSL to attach a context menu to any [View].
 *
 * Usage:
 *
 * myView.contextMenu {
 *   "Option 1" with { /* do something */ }
 *   "Option 2" with { v -> /* use clicked view */ }
 * }
 */
object ContextMenuDsl {
    class Builder(private val view: View) {
        internal val items = mutableListOf<Pair<CharSequence, (View) -> Unit>>()

        // "Title" with { ... }
        /*infix fun String.with(action: () -> Unit) {
            items += this to { _ -> action() }
        }*/

        // "Title" with { v -> ... }
        infix fun String.with(action: (View) -> Unit) {
            items += this to action
        }
    }
}

fun View.contextMenu(build: ContextMenuDsl.Builder.() -> Unit) {
    // Build the menu model once; it will be created each time the menu is shown
    val builder = ContextMenuDsl.Builder(this).apply(build)
    isLongClickable = true

    setOnCreateContextMenuListener { menu, v, _ ->
        builder.items.forEach { (title, action) ->
            menu.add(title).setOnMenuItemClickListener {
                action(v)
                true
            }
        }
    }
}