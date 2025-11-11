package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.FlowLayout
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.view.MotionEvent
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.CheckBox
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import icyllis.modernui.widget.ScrollView
import kotlinx.coroutines.launch
import java.io.File
import kotlin.text.Charsets

class ModpackCreate3Fragment(val mods: List<Mod>) : RFragment("制作整合包") {
    //读取config
    override var fragSize = FragmentSize.FULL

    private var confData: Map<String, ByteArray> = emptyMap()
    private var kjsData: Map<String, ByteArray> = emptyMap()
    private var confEntries: List<Map.Entry<String, ByteArray>> = emptyList()
    private var kjsEntries: List<Map.Entry<String, ByteArray>> = emptyList()
    private val selectedConfKeys = linkedSetOf<String>()
    private val selectedKjsKeys = linkedSetOf<String>()
    private var confSummaryView: TextView? = null
    private var kjsSummaryView: TextView? = null
    private var confGridContainer: LinearLayout? = null
    private var kjsGridContainer: LinearLayout? = null
    private val confCheckboxes = linkedMapOf<String, CheckBox>()
    private val kjsCheckboxes = linkedMapOf<String, CheckBox>()
    private var selectAllCheckbox: CheckBox? = null
    private var previewTitleView: TextView? = null
    private var previewContentView: TextView? = null
    private var previewScrollView: ScrollView? = null
    private var previewInitialized = false
    private var isBulkUpdating = false
    private var isUpdatingSelectAll = false
    private var dataLoaded = false

    companion object {
        fun readConfKjs(): Pair<Map<String, ByteArray>, Map<String, ByteArray>> {
            val confs = hashMapOf<String, ByteArray>()
            val kjs = hashMapOf<String, ByteArray>()
            val configRoot = File("config")
            if (configRoot.exists()) {
                configRoot.walk().forEach { file ->
                    if (file.isFile) {
                        val relative = file.relativeTo(configRoot).invariantSeparatorsPath
                        confs += relative to file.readBytes()
                    }
                }
            }

            val kubeRoot = File("kubejs")
            if (kubeRoot.exists()) {
                kubeRoot.walk().forEach { file ->
                    if (file.isFile) {
                        val relative = file.relativeTo(kubeRoot).invariantSeparatorsPath
                        kjs += relative to file.readBytes()
                    }
                }
            }
            return confs to kjs
        }
    }

    private fun renderSelectionUi() {
        previewInitialized = false
        contentView.apply {
            removeAllViews()
            linearLayout {
                orientation = LinearLayout.VERTICAL
                layoutParams = linearLayoutParam(PARENT, PARENT)
                paddingDp(12)

                textView("读取完成！") {
                    textSize = 16f
                }

                confSummaryView = textView("") {
                    layoutParams = linearLayoutParam(PARENT, SELF) {
                        topMargin = context.dp(8f)
                    }
                    textSize = 14f
                    setTextColor(MaterialColor.GRAY_500.colorValue)
                }

                kjsSummaryView = textView("") {
                    layoutParams = linearLayoutParam(PARENT, SELF) {
                        topMargin = context.dp(4f)
                    }
                    textSize = 14f
                    setTextColor(MaterialColor.GRAY_500.colorValue)
                }

                previewTitleView = textView("悬停左侧文件以预览内容") {
                    layoutParams = linearLayoutParam(PARENT, SELF) {
                        topMargin = context.dp(12f)
                    }
                    textSize = 15f
                    setTextColor(MaterialColor.GRAY_700.colorValue)
                }

                linearLayout {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = linearLayoutParam(PARENT, 0) {
                        weight = 1f
                        topMargin = context.dp(12f)
                    }

                    val listRoot = scrollView {
                        layoutParams = linearLayoutParam(0, PARENT) {
                            weight = 0.45f
                        }
                    }.linearLayout {
                        orientation = LinearLayout.VERTICAL
                    }

                    confGridContainer = listRoot.linearLayout {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = linearLayoutParam(PARENT, SELF)
                    }

                    kjsGridContainer = listRoot.linearLayout {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = linearLayoutParam(PARENT, SELF) {
                            topMargin = context.dp(16f)
                        }
                    }

                    previewScrollView = scrollView {
                        layoutParams = linearLayoutParam(0, PARENT) {
                            weight = 0.55f
                            leftMargin = context.dp(12f)
                        }
                    }

                    previewContentView = previewScrollView?.textView("（悬停左侧文件可在此查看内容）") {
                        layoutParams = linearLayoutParam(PARENT, SELF) {
                            topMargin = context.dp(4f)
                        }
                        typeface = Fonts.CODE.typeface
                        textSize = 12f
                        setPadding(context.dp(8f), context.dp(8f), context.dp(8f), context.dp(8f))
                        setTextIsSelectable(true)
                    }
                }
            }
        }

        confGridContainer?.let { container ->
            populateCheckboxList(container, "配置文件", confEntries, selectedConfKeys, confCheckboxes) {
                updateSelectionSummaries()
            }
        }

        kjsGridContainer?.let { container ->
            populateCheckboxList(container, "KubeJS 脚本", kjsEntries, selectedKjsKeys, kjsCheckboxes) {
                updateSelectionSummaries()
            }
        }

        updateSelectionSummaries()
        selectAllCheckbox?.isEnabled = confEntries.isNotEmpty() || kjsEntries.isNotEmpty()
    }

    private fun populateCheckboxList(
        container: LinearLayout,
        sectionTitle: String,
        entries: List<Map.Entry<String, ByteArray>>,
        selectedSet: MutableSet<String>,
        checkboxMap: MutableMap<String, CheckBox>,
        onSelectionChanged: () -> Unit
    ) {
        val ctx = container.context
        container.removeAllViews()
        checkboxMap.clear()

        container.textView(sectionTitle) {
            layoutParams = linearLayoutParam(PARENT, SELF)
            textSize = 13f
            setTextColor(MaterialColor.GRAY_600.colorValue)
        }

        val entryKeys = entries.map { it.key }
        val keySet = entryKeys.toSet()
        if (selectedSet.isEmpty()) {
            selectedSet.addAll(entryKeys)
        } else {
            selectedSet.retainAll(keySet)
            entryKeys.forEach { key ->
                if (key !in selectedSet) {
                    selectedSet += key
                }
            }
        }

        if (entries.isEmpty()) {
            container.textView("未检测到相关文件") {
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    topMargin = ctx.dp(4f)
                }
                setTextColor(MaterialColor.GRAY_500.colorValue)
            }
            onSelectionChanged()
            return
        }

        val margin = ctx.dp(4f)
        var suppress = true

        val flow = container.flowLayout {
            layoutParams = linearLayoutParam(PARENT, SELF) {
                topMargin = ctx.dp(6f)
            }
            setPadding(margin, margin, margin, margin)
        }

        entries.forEachIndexed { index, entry ->
            val key = entry.key
            val bytes = entry.value
            val checkBox = flow.checkBox(
                key,
                init = {
                    layoutParams = FlowLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = margin
                        topMargin = margin
                        bottomMargin = margin
                    }
                    textSize = 12f
                    maxLines = 3
                    isChecked = key in selectedSet
                }
            ) { _, isChecked ->
                if (!suppress && !isBulkUpdating) {
                    showPreview(key, bytes)
                }
                if (isChecked) {
                    selectedSet += key
                } else {
                    selectedSet -= key
                }
                if (!suppress && !isBulkUpdating) {
                    onSelectionChanged()
                }
            }

            checkBox.setOnHoverListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                        if (!isBulkUpdating) {
                            showPreview(key, bytes)
                        }
                    }
                }
                false
            }

            checkBox.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && !isBulkUpdating) {
                    showPreview(key, bytes)
                }
            }

            checkboxMap[key] = checkBox

            if (!previewInitialized && index == 0) {
                showPreview(key, bytes)
                previewInitialized = true
            }
        }

        suppress = false
        onSelectionChanged()
    }

    private fun updateSelectionSummaries() {
        confSummaryView?.text = buildSummaryText("配置文件", selectedConfKeys, confData)
        kjsSummaryView?.text = buildSummaryText("KubeJS 脚本", selectedKjsKeys, kjsData)
        syncSelectAllCheckbox()
    }

    private fun buildSummaryText(
        label: String,
        selectedKeys: Set<String>,
        data: Map<String, ByteArray>
    ): String {
        if (data.isEmpty()) {
            return "$label：未检测到文件"
        }
        val selectedCount = selectedKeys.size
        val totalCount = data.size
        val selectedSize = selectedKeys.sumOf { key -> data[key]?.size?.toLong() ?: 0L }
        val totalSize = data.values.sumOf { it.size.toLong() }
        val selectedSizeText = selectedSize.humanSize
        val totalSizeText = totalSize.humanSize
        return "$label：已选择 ${selectedCount}/${totalCount} 个，${selectedSizeText}/${totalSizeText}"
    }

    private fun showPreview(name: String, data: ByteArray) {
        previewTitleView?.text = "$name · ${data.size.toLong().humanSize}"
        previewContentView?.text = data.toPreviewString()
        previewScrollView?.scrollTo(0, 0)
    }

    private fun ByteArray.toPreviewString(): String {
        if (isEmpty()) {
            return "(空文件)"
        }
        val textAttempt = runCatching { String(this, Charsets.UTF_8) }.getOrNull()
            ?.replace('\u0000', ' ')
            ?.replace("\r\n", "\n")
        if (textAttempt != null && textAttempt.isLikelyReadable()) {
            val trimmed = if (textAttempt.length > 8000) {
                textAttempt.take(8000) + "\n… (预览已截断)"
            } else {
                textAttempt
            }
            return trimmed
        }

        val sample = if (size <= 128) this else copyOfRange(0, 128)
        return buildString {
            append("(二进制文件，以下为前 ${sample.size} 字节的十六进制预览)\n")
            sample.forEachIndexed { index, byte ->
                if (index % 16 == 0) {
                    append(index.toString(16).padStart(4, '0'))
                    append(": ")
                }
                append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
                append(' ')
                if (index % 16 == 15 || index == sample.lastIndex) {
                    append('\n')
                }
            }
        }
    }

    private fun String.isLikelyReadable(): Boolean {
        if (isEmpty()) return true
        val allowed = setOf('\n', '\r', '\t')
        val problematic = count { ch ->
            (ch < ' ' && ch !in allowed) || ch == '\uFFFD'
        }
        return problematic <= length / 10
    }

    init {
        titleViewInit = {
            textView("选择你要使用的配置文件与KubeJS脚本。")
            quickOptions {
                "全选".make(checkbox)
                    .checked()
                    .init {
                        selectAllCheckbox = this
                        isEnabled = false
                    } with { checked ->
                    if (!dataLoaded) return@with
                    if (isUpdatingSelectAll) return@with
                    setAllSelections(checked)
                }
                "下一步" colored MaterialColor.GREEN_900 with {
                    if (!dataLoaded) {
                        alertErr("配置仍在读取，请稍候")
                        return@with
                    }

                    if (selectedConfKeys.isEmpty() && selectedKjsKeys.isEmpty()) {
                        alertErr("请至少选择一个配置文件或脚本")
                        return@with
                    }

                    val selectedConf = confEntries
                        .filter { it.key in selectedConfKeys }
                        .associate { it.key to it.value }
                    val selectedKjs = kjsEntries
                        .filter { it.key in selectedKjsKeys }
                        .associate { it.key to it.value }

                    // ModpackCreate4Fragment(name, mods, selectedConf, selectedKjs).go()
                }
            }
        }
        contentViewInit = {
            textView("正在读取配置与脚本……") {
                setTextColor(MaterialColor.GRAY_500.colorValue)
            }
            ioScope.launch {
                val (conf, kjs) = readConfKjs()
                confEntries = conf.entries.sortedBy { it.key.lowercase() }
                kjsEntries = kjs.entries.sortedBy { it.key.lowercase() }
                confData = conf
                kjsData = kjs
                uiThread {
                    renderSelectionUi()
                    dataLoaded = true
                }
            }
        }
    }

    private fun syncSelectAllCheckbox() {
        val totalCount = confEntries.size + kjsEntries.size
        if (totalCount == 0) {
            isUpdatingSelectAll = true
            selectAllCheckbox?.apply {
                isChecked = false
                isEnabled = false
            }
            isUpdatingSelectAll = false
            return
        }

        val shouldCheck = selectedConfKeys.size + selectedKjsKeys.size == totalCount
        isUpdatingSelectAll = true
        selectAllCheckbox?.apply {
            isEnabled = true
            isChecked = shouldCheck
        }
        isUpdatingSelectAll = false
    }

    private fun setAllSelections(checked: Boolean) {
        if (!dataLoaded) return
        isBulkUpdating = true
        if (checked) {
            selectedConfKeys.clear()
            selectedConfKeys.addAll(confEntries.map { it.key })
            selectedKjsKeys.clear()
            selectedKjsKeys.addAll(kjsEntries.map { it.key })
        } else {
            selectedConfKeys.clear()
            selectedKjsKeys.clear()
        }

        confCheckboxes.values.forEach { it.isChecked = checked }
        kjsCheckboxes.values.forEach { it.isChecked = checked }

        isBulkUpdating = false
        updateSelectionSummaries()
    }
}