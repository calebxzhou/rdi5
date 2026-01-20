package calebxzhou.rdi.client.ui.component

import calebxzhou.rdi.common.model.Modpack.BriefVo
import calebxzhou.rdi.client.ui.*
import calebxzhou.rdi.common.model.Modpack
import icyllis.modernui.core.Context
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.LinearLayout

class ModpackGrid(
	ctx: Context,
	var modpacks: List<Modpack.BriefVo> = arrayListOf(),
	private val onItemClick: (Modpack.BriefVo) -> Unit = {},
) : LinearLayout(ctx) {

	private val cardsContainer: LinearLayout = LinearLayout(ctx).apply { vertical() }

	private var pendingRender: Runnable? = null
	private val cards = linkedMapOf<Modpack.BriefVo, ModpackCard>()

	init {
		vertical()
		layoutParams = linearLayoutParam(PARENT, PARENT)

		scrollView {
			layoutParams = linearLayoutParam(PARENT, 0) {
				weight = 1f
			}
			addView(cardsContainer, linearLayoutParam(PARENT, SELF))
		}

	 	cardsContainer.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
			val newWidth = right - left
			val oldWidth = oldRight - oldLeft
			if (newWidth > 0 && newWidth != oldWidth && modpacks.isNotEmpty()) {
				scheduleRender(modpacks)
			}
		}
	}



	private fun renderGrid(items: List<Modpack.BriefVo>) {
		val availableWidth = cardsContainer.width
		if (availableWidth <= 0) {
			scheduleRender(items)
			return
		}

		val contentWidth = availableWidth - cardsContainer.paddingLeft - cardsContainer.paddingRight
		val minCardWidth = context.dp(240f)
		val spacing = context.dp(16f)
		val columns = maxOf(1, (contentWidth + spacing) / (minCardWidth + spacing))

		val recycled = HashMap(cards)
		cards.clear()
		cardsContainer.removeAllViews()

		items.chunked(columns).forEach { rowItems ->
			val row = LinearLayout(context).apply {
				horizontal()
				gravity = Gravity.TOP
			}

			rowItems.forEachIndexed { index, modpack ->
				val card = recycled[modpack] ?: ModpackCard(context, modpack)
				(card.parent as? ViewGroup)?.removeView(card)
				card.setOnClickListener { onItemClick.invoke(modpack) }

				cards[modpack] = card
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

	private fun scheduleRender(items: List<Modpack.BriefVo>) {
		pendingRender?.let { cardsContainer.removeCallbacks(it) }
		val task = Runnable {
			pendingRender = null
			if (items.isNotEmpty()) renderGrid(items)
		}
		pendingRender = task
		cardsContainer.post(task)
	}
}
