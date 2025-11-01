package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.horizontal
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.scrollView
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.ui2.vertical
import icyllis.modernui.core.Context
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView

class ModGrid(ctx: Context,val onSelect: (List<ModBriefVo>) -> Unit={} ) : LinearLayout(ctx) {

    private val cardsContainer: LinearLayout = LinearLayout(ctx).apply {
        vertical()
    }
    private val stateTextView: TextView = TextView(ctx).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setTextColor(MaterialColor.GRAY_500.colorValue)
        setPadding(0, ctx.dp(8f), 0, ctx.dp(8f))
        text = "正在读取已经安装的mod…"
    }
    private val selectedMods = linkedSetOf<ModBriefVo>()
    private val cardMap = linkedMapOf<ModBriefVo, ModCard>()

    init {
        vertical()

        scrollView {
            layoutParams = linearLayoutParam(PARENT, 0) {
                weight = 1f
            }
            addView(cardsContainer, linearLayoutParam(PARENT, SELF))
        }

        cardsContainer.addView(stateTextView, linearLayoutParam(PARENT, SELF))
    }

    fun showLoading(message: String) = uiThread {
        displayState(message, paddingTopDp = 8f, clearSelection = false)
    }

    fun showEmpty(message: String) = uiThread {
        displayState(message, paddingTopDp = 16f, clearSelection = true)
    }

    fun showMods(briefs: List<ModBriefVo>) = uiThread {
        if (briefs.isEmpty()) {
            displayState("暂无可展示的Mod", paddingTopDp = 16f, clearSelection = true)
            return@uiThread
        }

    val availableBriefs = briefs.toSet()
    selectedMods.retainAll(availableBriefs)
    cardMap.clear()
        cardsContainer.removeAllViews()

        val rowContext = context
        briefs.chunked(3).forEach { rowItems ->
            val row = LinearLayout(rowContext).apply {
                horizontal()
                gravity = Gravity.TOP
            }
            rowItems.forEachIndexed { index, brief ->
                val card = ModCard(rowContext, brief)
                card.setSelectedState(selectedMods.contains(brief))
                card.setOnClickListener {
                    val isNowSelected = card.toggleSelectedState()
                    if (isNowSelected) {
                        selectedMods.add(brief)
                    } else {
                        selectedMods.remove(brief)
                    }
                    notifySelectionChanged()
                }
                cardMap[brief] = card
                row.addView(card, linearLayoutParam(0, SELF) {
                    weight = 1f
                    if (index < rowItems.lastIndex) {
                        rightMargin = rowContext.dp(12f)
                    }
                })
            }
            cardsContainer.addView(row, linearLayoutParam(PARENT, SELF) {
                bottomMargin = rowContext.dp(12f)
            })
        }
    }

    fun getSelectedMods(): List<ModBriefVo> = selectedMods.toList()

    fun selectAll() = uiThread {
        if (cardMap.isEmpty()) return@uiThread
        var changed = false
        selectedMods.clear()
        cardMap.forEach { (brief, card) ->
            selectedMods.add(brief)
            if (!card.isCardSelected()) {
                card.setSelectedState(true)
                changed = true
            }
        }
        if (changed || selectedMods.size == cardMap.size) {
            notifySelectionChanged()
        }
    }

    fun clearSelection() = uiThread {
        var changed = false
        if (selectedMods.isNotEmpty()) {
            selectedMods.clear()
            changed = true
        }
        cardMap.values.forEach { card ->
            if (card.isCardSelected()) {
                card.setSelectedState(false)
                changed = true
            }
        }
        if (changed) {
            notifySelectionChanged()
        }
    }



    private fun displayState(message: String, paddingTopDp: Float, clearSelection: Boolean) {
        if (clearSelection) {
            if (selectedMods.isNotEmpty()) {
                selectedMods.clear()
                notifySelectionChanged()
            }
        }
        cardMap.clear()
        cardsContainer.removeAllViews()
        stateTextView.text = message
        stateTextView.setPadding(0, context.dp(paddingTopDp), 0, context.dp(paddingTopDp))
        cardsContainer.addView(stateTextView, linearLayoutParam(PARENT, SELF))
    }

    private fun notifySelectionChanged() {
        val snapshot = getSelectedMods()
        onSelect(snapshot)
    }
}