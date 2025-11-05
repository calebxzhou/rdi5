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
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView

class ModGrid(
    ctx: Context,
    isSelectionEnabled: Boolean = false,
    onSelectionChanged: ((List<ModBriefVo>) -> Unit)? = null
) : LinearLayout(ctx) {

    private val cardsContainer: LinearLayout = LinearLayout(ctx).apply { vertical() }

    private val stateTextView: TextView = TextView(ctx).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setTextColor(MaterialColor.GRAY_500.colorValue)
        setPadding(0, ctx.dp(8f), 0, ctx.dp(8f))
        text = "正在读取已经安装的mod…"
    }

    private val selectedMods = linkedSetOf<ModBriefVo>()
    private val cardMap = linkedMapOf<ModBriefVo, ModCard>()
    private val selectionListeners = mutableListOf<(List<ModBriefVo>) -> Unit>()
    private var currentBriefs: List<ModBriefVo> = emptyList()
    private var pendingRender: Runnable? = null

    var selectionEnabled: Boolean = isSelectionEnabled
        set(value) {
            if (field == value) return
            field = value
            cardMap.values.forEach { card ->
                card.enableSelect = value
                card.setOnClickListener(if (value) createCardClickListener(card) else null)
            }
            if (!value && selectedMods.isNotEmpty()) {
                selectedMods.clear()
                notifySelectionChanged()
            }
        }

    init {
        vertical()
        layoutParams = linearLayoutParam(PARENT, PARENT)

        scrollView {
            layoutParams = linearLayoutParam(PARENT, 0) {
                weight = 1f
            }
            addView(cardsContainer, linearLayoutParam(PARENT, SELF))
        }

        cardsContainer.addView(stateTextView, linearLayoutParam(PARENT, SELF))

        onSelectionChanged?.let { addSelectionListener(it) }

        cardsContainer.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            val newWidth = right - left
            val oldWidth = oldRight - oldLeft
            if (newWidth > 0 && newWidth != oldWidth && currentBriefs.isNotEmpty()) {
                schedulePendingRender(currentBriefs)
            }
        }
    }

    fun addSelectionListener(listener: (selected: List<ModBriefVo>) -> Unit) {
        selectionListeners.add(listener)
    }

    fun removeSelectionListener(listener: (selected: List<ModBriefVo>) -> Unit) {
        selectionListeners.remove(listener)
    }

    fun showLoading(message: String) = uiThread {
        displayState(message, paddingTopDp = 8f, clearSelection = false)
    }

    fun showEmpty(message: String) = uiThread {
        displayState(message, paddingTopDp = 16f, clearSelection = true)
    }

    fun showMods(briefs: List<ModBriefVo>) = uiThread {
        if (briefs.isEmpty()) {
            currentBriefs = emptyList()
            displayState("暂无可展示的Mod", paddingTopDp = 16f, clearSelection = true)
            return@uiThread
        }

        currentBriefs = briefs

        val availableBriefs = briefs.toSet()
        val selectionTrimmed = selectedMods.retainAll(availableBriefs)

        renderGrid(briefs)

        if (selectionTrimmed) {
            notifySelectionChanged()
        }
    }

    fun getSelectedMods(): List<ModBriefVo> = selectedMods.toList()

    fun selectAll() = uiThread {
        if (!selectionEnabled || cardMap.isEmpty()) return@uiThread
        selectedMods.clear()
        cardMap.forEach { (brief, card) ->
            selectedMods.add(brief)
            if (!card.isCardSelected()) {
                card.setSelectedState(true)
            }
        }
        notifySelectionChanged()
    }

    fun clearSelection(triggerNotification: Boolean = true) = uiThread {
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
        if (changed && triggerNotification) {
            notifySelectionChanged()
        }
    }

    private fun createCardClickListener(card: ModCard): View.OnClickListener {
        return View.OnClickListener {
            if (!selectionEnabled) return@OnClickListener
            val isNowSelected = card.toggleSelectedState()
            if (isNowSelected) {
                selectedMods.add(card.vo)
            } else {
                selectedMods.remove(card.vo)
            }
            notifySelectionChanged()
        }
    }

    private fun displayState(message: String, paddingTopDp: Float, clearSelection: Boolean) {
        pendingRender?.let { cardsContainer.removeCallbacks(it) }
        pendingRender = null
        currentBriefs = emptyList()
        if (clearSelection && selectedMods.isNotEmpty()) {
            selectedMods.clear()
            notifySelectionChanged()
        }
        cardMap.clear()
        cardsContainer.removeAllViews()
        stateTextView.text = message
        stateTextView.setPadding(0, context.dp(paddingTopDp), 0, context.dp(paddingTopDp))
        cardsContainer.addView(stateTextView, linearLayoutParam(PARENT, SELF))
    }

    private fun notifySelectionChanged() {
        val snapshot = getSelectedMods()
        selectionListeners.forEach { it(snapshot) }
    }

    private fun renderGrid(briefs: List<ModBriefVo>) {
        val availableWidth = cardsContainer.width
        if (availableWidth <= 0) {
            schedulePendingRender(briefs)
            return
        }

        val contentWidth = availableWidth - cardsContainer.paddingLeft - cardsContainer.paddingRight
        val minCardWidth = context.dp(180f)
        val spacing = context.dp(12f)
        val columns = maxOf(1, (contentWidth + spacing) / (minCardWidth + spacing))

        val existingCards = HashMap(cardMap)
        cardMap.clear()
        cardsContainer.removeAllViews()

        val rowContext = context
        briefs.chunked(columns).forEach { rowItems ->
            val row = LinearLayout(rowContext).apply {
                horizontal()
                gravity = Gravity.TOP
            }

            rowItems.forEachIndexed { index, brief ->
                val card = existingCards[brief] ?: ModCard(rowContext, brief, enableSelect = selectionEnabled)
                (card.parent as? ViewGroup)?.removeView(card)
                card.enableSelect = selectionEnabled
                card.setSelectedState(selectedMods.contains(brief))
                card.setOnClickListener(if (selectionEnabled) createCardClickListener(card) else null)
                cardMap[brief] = card
                row.addView(card, linearLayoutParam(0, SELF) {
                    weight = 1f
                    if (index < rowItems.lastIndex) {
                        rightMargin = rowContext.dp(12f)
                    }
                })
            }

            if (rowItems.size < columns) {
                repeat(columns - rowItems.size) {
                    row.addView(View(rowContext), linearLayoutParam(0, SELF) { weight = 1f })
                }
            }

            cardsContainer.addView(row, linearLayoutParam(PARENT, SELF) {
                bottomMargin = rowContext.dp(12f)
            })
        }
    }

    private fun schedulePendingRender(briefs: List<ModBriefVo>) {
        pendingRender?.let { cardsContainer.removeCallbacks(it) }
        pendingRender = Runnable {
            pendingRender = null
            if (currentBriefs.isNotEmpty()) {
                renderGrid(currentBriefs)
            }
        }
        cardsContainer.post(pendingRender)
    }
}
