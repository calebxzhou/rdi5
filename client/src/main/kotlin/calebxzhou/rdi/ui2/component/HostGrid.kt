package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.model.Host
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.*
import icyllis.modernui.core.Context
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import kotlin.math.max

class HostGrid(
    ctx: Context,
    val hosts: List<Host.Vo> = emptyList(),
    private val onItemClick: (Host.Vo) -> Unit = {},
    private val onPlayClick: (Host.Vo) -> Unit = onItemClick,
) : LinearLayout(ctx) {

    private val cardsContainer: LinearLayout = LinearLayout(ctx).apply { vertical() }

    private val cards = linkedMapOf<Host.Vo, HostCard>()
    private var pendingRender: Runnable? = null


    init {
        vertical()
        layoutParams = linearLayoutParam(PARENT, PARENT)

        scrollView {
            layoutParams = linearLayoutParam(PARENT, PARENT)
            addView(cardsContainer, linearLayoutParam(PARENT, SELF))
        }

        cardsContainer.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            val newWidth = right - left
            val oldWidth = oldRight - oldLeft
            if (newWidth > 0 && newWidth != oldWidth && hosts.isNotEmpty()) {
                scheduleRender(hosts)
            }
        }

        updateHosts()
    }

    private fun updateHosts() {
            scheduleRender(hosts)

    }


    private fun scheduleRender(items: List<Host.Vo>) {
        pendingRender?.let { cardsContainer.removeCallbacks(it) }
        val task = Runnable {
            pendingRender = null
            if (items.isNotEmpty()) {
                renderGrid(items)
            }
        }
        pendingRender = task
        cardsContainer.post(task)
    }

    private fun renderGrid(items: List<Host.Vo>) {
        val availableWidth = cardsContainer.width
        if (availableWidth <= 0) {
            scheduleRender(items)
            return
        }

        val contentWidth = availableWidth - cardsContainer.paddingLeft - cardsContainer.paddingRight
        val minCardWidth = context.dp(240f)
        val spacing = context.dp(16f)
        val columns = max(1, (contentWidth + spacing) / (minCardWidth + spacing))

        val recycled = HashMap(cards)
        cards.clear()
        cardsContainer.removeAllViews()

        items.chunked(columns).forEach { rowItems ->
            val row = LinearLayout(context).apply {
                horizontal()
                gravity = Gravity.TOP
            }

            rowItems.forEachIndexed { index, host ->
                val card = recycled[host] ?: HostCard(context, host, onClickPlay = onPlayClick)
                (card.parent as? ViewGroup)?.removeView(card)
                card.setOnClickListener { onItemClick(host) }
                cards[host] = card
                row.addView(card, linearLayoutParam(0, SELF) {
                    weight = 1f
                    if (index < rowItems.lastIndex) {
                        rightMargin = context.dp(16f)
                    }
                })
            }

            if (rowItems.size < columns) {
                repeat(columns - rowItems.size) {
                    row.addView(View(context), linearLayoutParam(0, SELF) { weight = 1f })
                }
            }

            cardsContainer.addView(row, linearLayoutParam(PARENT, SELF) {
                bottomMargin = context.dp(16f)
            })
        }
    }
}
