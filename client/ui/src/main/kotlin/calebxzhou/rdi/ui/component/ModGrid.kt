package calebxzhou.rdi.ui.component

import calebxzhou.rdi.Const
import calebxzhou.rdi.common.model.CurseForgeLocalResult
import calebxzhou.rdi.common.model.Mod
import calebxzhou.rdi.common.service.CurseForgeService.loadInfoCurseForge
import calebxzhou.rdi.common.service.ModService.filterServerOnlyMods
import calebxzhou.rdi.common.service.ModService.installedMods
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.ui.MaterialColor
import calebxzhou.rdi.ui.PARENT
import calebxzhou.rdi.ui.SELF
import calebxzhou.rdi.ui.dp
import calebxzhou.rdi.ui.horizontal
import calebxzhou.rdi.ui.linearLayoutParam
import calebxzhou.rdi.ui.scrollView
import calebxzhou.rdi.ui.uiThread
import calebxzhou.rdi.ui.vertical

import icyllis.modernui.core.Context
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import java.io.File
import kotlin.collections.isNotEmpty
import kotlin.collections.toSet

class ModGrid(
    ctx: Context,
    val isSelectionEnabled: Boolean = false,
    //有=不从cf获取 直接用
    var mods: List<Mod> = emptyList(),
    val onSelectionChanged: ((List<Mod>) -> Unit)? = null,
    ) : LinearLayout(ctx) {
    companion object {
        val MOCK_DATA: List<Mod>
            get() = File("temp_mods.cbor").readBytes().let { Cbor.decodeFromByteArray(it) }
    }

    private val cardsContainer: LinearLayout = LinearLayout(ctx).apply { vertical() }

    private val stateTextView: TextView = TextView(ctx).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setTextColor(MaterialColor.GRAY_500.colorValue)
        setPadding(0, ctx.dp(8f), 0, ctx.dp(8f))
        text = "正在读取mod…"
    }

    private val selectedMods = linkedSetOf<Mod>()
    private val cards = linkedMapOf<Mod, ModCard>()
    private var pendingRender: Runnable? = null

    //mod载入结果 如果直接用了brief就是null
    var modLoadResult: CurseForgeLocalResult? = null

    var selectionEnabled: Boolean = isSelectionEnabled
        set(value) {
            if (field == value) return
            field = value
            cards.forEach { card ->
                card.value.enableSelect = value
                card.value.setOnClickListener(if (value) createCardClickListener(card.value) else null)
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


        cardsContainer.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            val newWidth = right - left
            val oldWidth = oldRight - oldLeft
            if (newWidth > 0 && newWidth != oldWidth && mods.isNotEmpty()) {
                schedulePendingRender(mods)
            }
        }


    }

    fun loadModsFromLocalInstalled() = ioTask {
        val mods = installedMods.filterServerOnlyMods()
        if (mods.isEmpty()) {
            showEmpty()
            return@ioTask
        }
        showLoading("找到了${mods.size}个mod，正在从CurseForge读取信息...大概5~10秒")
        if (Const.USE_MOCK_DATA) {
            showMods(MOCK_DATA)
        } else {
            mods.loadInfoCurseForge().let { modLoadResult = it; showMods(it.matched) }
        }
    }

    fun showLoading(message: String) = uiThread {
        displayState(message, paddingTopDp = 8f, clearSelection = false)
    }

    fun showEmpty() = uiThread {
        displayState("暂无可展示的Mod", paddingTopDp = 16f, clearSelection = true)
    }

    fun showMods(modsToShow: List<Mod>) = uiThread {
        if (modsToShow.isEmpty()) {
            this.mods = emptyList()
            showEmpty()
            return@uiThread
        }

        this.mods = modsToShow

        val availableBriefs = modsToShow.toSet()
        val selectionTrimmed = selectedMods.retainAll(availableBriefs)

        renderGrid(modsToShow)

        if (selectionTrimmed) {
            notifySelectionChanged()
        }
    }

    fun getSelectedMods(): List<Mod> = selectedMods.toList()

    fun selectAll() = uiThread {
        if (!selectionEnabled || cards.isEmpty()) return@uiThread
        selectedMods.clear()
        cards.forEach { (mod, card) ->
            selectedMods.add(mod)
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
        cards.values.forEach { card ->
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
                selectedMods.add(card.mod)
            } else {
                selectedMods.remove(card.mod)
            }
            notifySelectionChanged()
        }
    }

    private fun displayState(message: String, paddingTopDp: Float, clearSelection: Boolean) {
        pendingRender?.let { cardsContainer.removeCallbacks(it) }
        pendingRender = null
        mods = emptyList()
        if (clearSelection && selectedMods.isNotEmpty()) {
            selectedMods.clear()
            notifySelectionChanged()
        }
        cards.clear()
        cardsContainer.removeAllViews()
        stateTextView.text = message
        stateTextView.setPadding(0, context.dp(paddingTopDp), 0, context.dp(paddingTopDp))
        cardsContainer.addView(stateTextView, linearLayoutParam(PARENT, SELF))
    }

    private fun notifySelectionChanged() {
        val snapshot = getSelectedMods()
        onSelectionChanged?.invoke(snapshot)
    }

    private fun renderGrid(modsToRender: List<Mod>) {
        val availableWidth = cardsContainer.width
        if (availableWidth <= 0) {
            schedulePendingRender(modsToRender)
            return
        }

        val contentWidth = availableWidth - cardsContainer.paddingLeft - cardsContainer.paddingRight
        val minCardWidth = context.dp(180f)
        val spacing = context.dp(12f)
        val columns = maxOf(1, (contentWidth + spacing) / (minCardWidth + spacing))

        val existingCards = HashMap(cards)
        cards.clear()
        cardsContainer.removeAllViews()

        val rowContext = context
        modsToRender.chunked(columns).forEach { rowItems ->
            val row = LinearLayout(rowContext).apply {
                horizontal()
                gravity = Gravity.TOP
            }

            rowItems.forEachIndexed { index, mod ->
                val card = existingCards[mod] ?: ModCard(rowContext, mod, enableSelect = selectionEnabled)
                (card.parent as? ViewGroup)?.removeView(card)
                card.enableSelect = selectionEnabled
                card.setSelectedState(selectedMods.contains(mod))
                card.setOnClickListener(if (selectionEnabled) createCardClickListener(card) else null)
                cards[mod] = card
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

    private fun schedulePendingRender(modsToRender: List<Mod>) {
        pendingRender?.let { cardsContainer.removeCallbacks(it) }
        pendingRender = Runnable {
            pendingRender = null
            if (modsToRender.isNotEmpty()) {
                renderGrid(modsToRender)
            }
        }
        cardsContainer.post(pendingRender)
    }
}
