package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.model.CurseForgeMod
import calebxzhou.rdi.model.ModBriefInfo
import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.model.ModrinthProject
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.service.ModService.logo
import calebxzhou.rdi.service.ModService.murmur2Mods
import calebxzhou.rdi.service.ModService.sha1Mods
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.ImageSelection
import calebxzhou.rdi.ui2.component.ModCard
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.graphics.BitmapFactory
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.jar.JarFile

class ModpackCreateFragment : RFragment("制作整合包1") {
    private lateinit var nameInput: RTextField
    //private lateinit var picker: ImagePicker

    private companion object {
        private const val MAX_FILE_BYTES = 256 * 1024
        private const val MAX_DIMENSION = 512
    }

    override var fragSize = FragmentSize.SMALL

    init {
        contentLayoutInit = {
            nameInput = editText("给你的包起个名字")
            /*textView("选择一个图标（可选）")
            picker = ImagePicker(fctx).apply {
                validator = ::validateSelection
            }.also { this += it }*/
        }
        bottomOptionsConfig = {
            "下一步" colored MaterialColor.GREEN_900 with {
                // val image = picker.selectedBytes
                val params = buildList {
                    add("name" to nameInput.text)
                    // add("info" to infoInput.text)
                    // image?.let { add("image" to it.encodeBase64()) }
                }
                ModpackCreate2Fragment(params).go()
            }
        }
    }

    private fun validateSelection(selection: ImageSelection): Boolean {
        val sizeBytes = selection.bytes.size
        if (sizeBytes > MAX_FILE_BYTES) {
            alertErr("图片大小需小于256KB，当前为${sizeBytes.toLong().humanSize}")
            return false
        }

        val bitmap = BitmapFactory.decodeByteArray(selection.bytes, 0, sizeBytes)
        if (bitmap == null) {
            alertErr("无法解析所选图片")
            return false
        }

        val width = bitmap.width
        val height = bitmap.height
        bitmap.recycle()

        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            alertErr("图片尺寸需不超过512x512像素，当前为${width}×${height}")
            return false
        }

        return true
    }
}

class ModpackCreate2Fragment(val params: List<Pair<String, String>>) : RFragment("制作整合包2") {
    override var fragSize = FragmentSize.LARGE
    private lateinit var modsGrid: LinearLayout
    private lateinit var loadingTv: TextView

    init {
        contentLayoutInit = {
            textView("将使用以下这些Mod：")
            scrollView {
                layoutParams = linearLayoutParam(PARENT, 0) {
                    weight = 1f
                    topMargin = context.dp(12f)
                }
                modsGrid = linearLayout {
                    vertical()
                }
                loadingTv = modsGrid.textView("正在读取已经安装的mod…") {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setTextColor(MaterialColor.GRAY_500.colorValue)
                    setPadding(0, context.dp(8f), 0, context.dp(8f))
                }
            }
            loadMods()
        }
        bottomOptionsConfig = {
            "下一步" colored MaterialColor.GREEN_900 with {

            }
        }
    }

    private fun updateLoadingText(text: String) {
        uiThread {
            loadingTv.text = text
        }
    }

    private fun loadMods() = ioScope.launch {
        val modsCount = ModService.mods.size
        if (modsCount == 0) {
            updateLoadingText("未检测到已安装的mod")
            renderCards(emptyList())
            return@launch
        }

        updateLoadingText("找到了${modsCount}个mod，正在同时从 Modrinth 和 CurseForge 读取详细信息...")

        val (modrinthResult, curseForgeResult) = coroutineScope {
            val modrinthDeferred = async { fetchModrinthCards() }
            val curseForgeDeferred = async { fetchCurseForgeCards() }
            modrinthDeferred.await() to curseForgeDeferred.await()
        }

        val combinedBriefs = mergeSourceCards(modrinthResult, curseForgeResult)
        val foundCount = combinedBriefs.size
        val summaryMessage = when {
            foundCount == 0 -> "未能从 Modrinth 或 CurseForge 获取到任何详细信息"
            foundCount < modsCount -> "已为${foundCount}/${modsCount}个mod加载详情"
            else -> "已为全部${modsCount}个mod加载详情"
        }
        updateLoadingText(summaryMessage)
        renderCards(combinedBriefs)
    }

    private suspend fun fetchModrinthCards(): SourceResult {
        return runCatching {
            val versionData = ModService.getVersionsModrinth()
            if (versionData.isEmpty()) return@runCatching SourceResult()

            val hashToFile = sha1Mods
            val projectIdToFiles = versionData.mapNotNull { (hash, info) ->
                val file = hashToFile[hash] ?: return@mapNotNull null
                info.projectId to file
            }.groupBy({ it.first }, { it.second })

            val projectIds = projectIdToFiles.keys.filter { it.isNotBlank() }
            if (projectIds.isEmpty()) return@runCatching SourceResult()

            val projects = ModService.getProjectsModrinth(projectIds)
            val cards = projects.mapNotNull { project ->
                val slug = project.slug.trim()
                if (slug.isEmpty()) return@mapNotNull null
                val normalizedSlug = slug.lowercase()
                val modFile = projectIdToFiles[project.id]?.firstOrNull()
                val briefInfo = ModService.mrSlugBriefInfo[normalizedSlug]
                val brief = when {
                    briefInfo != null -> briefInfo.toVo(modFile)
                    else -> project.toBriefVo(modFile)
                }
                SourceCard(normalizedSlug, brief)
            }
            SourceResult(cards)
        }.failAlertPrint().getOrElse { SourceResult() }
    }

    private suspend fun fetchCurseForgeCards(): SourceResult {
        return runCatching {
            val fingerprintData = ModService.getFingerprintsCurseForge()
            val fingerprintToFile = murmur2Mods

            val modIdToFiles = fingerprintData.exactMatches.fold(mutableMapOf<Long, MutableList<File>>()) { acc, match ->
                val modId = match.id
                if (modId > 0) {
                    val fingerprint = match.file?.fileFingerprint ?: match.latestFiles.firstOrNull()?.fileFingerprint
                    if (fingerprint != null) {
                        val localFile = fingerprintToFile[fingerprint.toInt()]
                        if (localFile != null) {
                            acc.getOrPut(modId) { mutableListOf() }.add(localFile)
                        }
                    }
                }
                acc
            }

            val modIds = modIdToFiles.keys.distinct()
            if (modIds.isEmpty()) return@runCatching SourceResult()

            val mods = ModService.getInfosCurseForge(modIds)
            val cards = mods.mapNotNull { mod ->
                val slug = mod.slug.trim()
                if (slug.isEmpty()) return@mapNotNull null
                val normalizedSlug = slug.lowercase()
                val modFile = mod.id?.let { modIdToFiles[it]?.firstOrNull() }
                val briefInfo = ModService.cfSlugBriefInfo[normalizedSlug]
                val brief = when {
                    briefInfo != null -> briefInfo.toVo(modFile)
                    else -> mod.toBriefVo(modFile)
                }
                SourceCard(normalizedSlug, brief)
            }
            SourceResult(cards)
        }.failAlertPrint().getOrElse { SourceResult() }
    }

    private fun mergeSourceCards(vararg results: SourceResult): List<ModBriefVo> {
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<ModBriefVo>()
        results.forEach { result ->
            result.cards.forEach { sourceCard ->
                if (seen.add(sourceCard.slug)) {
                    merged += sourceCard.brief
                }
            }
        }
        return merged
    }

    private data class SourceResult(val cards: List<SourceCard> = emptyList())
    private data class SourceCard(val slug: String, val brief: ModBriefVo)

    private fun ModrinthProject.toBriefVo(modFile: File?): ModBriefVo {
        val introText = when {
            !description.isNullOrBlank() -> description.trim()
            !body.isNullOrBlank() -> body.trim()
            else -> "暂无简介"
        }
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.logo } }.getOrNull()
        }
        return ModBriefVo(
            name = title.ifBlank { slug },
            nameCn = null,
            intro = introText,
            iconData = iconBytes,
            iconUrls = buildList {
                if (!iconUrl.isNullOrBlank()) add(iconUrl)
            }
        )
    }

    private fun CurseForgeMod.toBriefVo(modFile: File?): ModBriefVo {
        val introText = summary?.takeIf { it.isNotBlank() }?.trim() ?: "暂无简介"
        val icons = buildList {
            logo?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            logo?.url?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val resolvedName = (name ?: slug).ifBlank { slug }
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.logo } }.getOrNull()
        }
        return ModBriefVo(
            name = resolvedName,
            nameCn = null,
            intro = introText,
            iconData = iconBytes,
            iconUrls = icons
        )
    }

    private fun showEmptyState(message: String) {
        modsGrid.removeAllViews()
        modsGrid.textView(message) {
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(MaterialColor.GRAY_500.colorValue)
            setPadding(0, context.dp(16f), 0, context.dp(16f))
        }
    }

    private fun renderCards(briefs: List<ModBriefVo>) = uiThread {
        modsGrid.removeAllViews()
        if (briefs.isEmpty()) {
            showEmptyState("暂无可展示的Mod")
            return@uiThread
        }

        val rowContext = modsGrid.context
        briefs.chunked(3).forEach { rowItems ->
            val row = LinearLayout(rowContext).apply {
                horizontal()
                gravity = Gravity.TOP
            }
            rowItems.forEachIndexed { index, brief ->
                val card = ModCard(rowContext, brief)
                row.addView(card, linearLayoutParam(0, SELF) {
                    weight = 1f
                    if (index < rowItems.lastIndex) {
                        rightMargin = rowContext.dp(12f)
                    }
                })
            }
            modsGrid.addView(row, linearLayoutParam(PARENT, SELF) {
                bottomMargin = rowContext.dp(12f)
            })
        }

    }

    private fun ModBriefInfo.toVo(modFile: File? = null): ModBriefVo {
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.logo } }.getOrNull()
        }
        return ModBriefVo(
            name = name,
            nameCn = nameCn,
            intro = intro,
            iconData = iconBytes,
            iconUrls = buildList {
                if (logoUrl.isNotBlank()) add(logoUrl)
            }
        )
    }
}