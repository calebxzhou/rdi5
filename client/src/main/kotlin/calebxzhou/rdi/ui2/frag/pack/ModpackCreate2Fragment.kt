package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.CurseForgeMod
import calebxzhou.rdi.model.ModBriefInfo
import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.service.ModService.logo
import calebxzhou.rdi.service.ModService.modDescription
import calebxzhou.rdi.service.ModService.murmur2Mods
import calebxzhou.rdi.service.ModService.readNeoForgeConfig
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.component.ModCard
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.failAlertPrint
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.ui2.horizontal
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.scrollView
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.ui2.vertical
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import kotlinx.coroutines.launch
import java.io.File
import java.util.jar.JarFile
import kotlin.collections.firstOrNull
import kotlin.collections.isNotEmpty
import kotlin.collections.orEmpty

/**
 * calebxzhou @ 2025-10-17 13:50
 */

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
        val modFiles = ModService.mods
        val modsCount = modFiles.size
        if (modsCount == 0) {
            updateLoadingText("未检测到已安装的mod")
            renderCards(emptyList())
            return@launch
        }

        updateLoadingText("找到了${modsCount}个mod，正在从 CurseForge 读取详细信息...大概5~10秒")

        val curseForgeResult = fetchCurseForgeCards()
        val curseForgeMatched = curseForgeResult.matchedFiles
        val curseForgeUnmatched = modFiles.filter { it !in curseForgeMatched }
        logUnmatched("CurseForge", curseForgeUnmatched)
        updateLoadingText("CurseForge 已匹配 ${curseForgeMatched.size} 个 Mod，正在载入结果...")
        val briefs = curseForgeResult.cards.map { it.brief }
        val foundCount = briefs.size
        title += "（$foundCount 个Mod）"
        renderCards(briefs)
    }

    private suspend fun fetchCurseForgeCards(): SourceResult {
        return runCatching {
            val fingerprintData = ModService.getFingerprintsCurseForge()
            val fingerprintToFile = murmur2Mods

            val modIdToFiles: MutableMap<Long, MutableList<File>> = mutableMapOf()
            fingerprintData.exactMatches.forEach { match ->
                val modId = match.id
                if (modId > 0) {
                    val fingerprint = match.file?.fileFingerprint ?: match.latestFiles.firstOrNull()?.fileFingerprint
                    if (fingerprint != null) {
                        val localFile = fingerprintToFile[fingerprint]
                        if (localFile != null) {
                            modIdToFiles.getOrPut(modId) { mutableListOf<File>() }.add(localFile)
                        }
                    }
                }
            }

            val modIds = modIdToFiles.keys.distinct()
            if (modIds.isEmpty()) return@runCatching SourceResult()

            val mods = ModService.getInfosCurseForge(modIds)
            val matchedFiles = mutableSetOf<File>()
            val cards = mods.mapNotNull { mod ->
                val slug = mod.slug.trim()
                if (slug.isEmpty()) return@mapNotNull null
                val normalizedSlug = slug.lowercase()
                val filesForMod = mod.id?.let { modIdToFiles[it] }.orEmpty()
                if (filesForMod.isNotEmpty()) {
                    matchedFiles += filesForMod
                }
                val modFile = filesForMod.firstOrNull() ?: return@mapNotNull null
                val briefInfo = ModService.cfSlugBriefInfo[normalizedSlug]
                val brief = when {
                    briefInfo != null -> briefInfo.toVo(modFile)
                    else -> mod.toBriefVo(modFile)
                }
                SourceCard(normalizedSlug, brief)
            }
            SourceResult(cards, matchedFiles)
        }.failAlertPrint().getOrElse { SourceResult() }
    }

    private data class SourceResult(
        val cards: List<SourceCard> = emptyList(),
        val matchedFiles: Set<File> = emptySet()
    )
    private data class SourceCard(val slug: String, val brief: ModBriefVo)

    private fun logUnmatched(source: String, unmatched: Collection<File>) {
        if (unmatched.isEmpty()) {
            lgr.info("$source: 所有本地 Mod 均已匹配")
        } else {
            lgr.info(
                "$source: 未匹配到 ${unmatched.size} 个 Mod: " +
                        unmatched.joinToString(", ") { it.name }
            )
        }
    }


    private fun CurseForgeMod.toBriefVo(modFile: File?): ModBriefVo {
        val icons = buildList {
            logo?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            logo?.url?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val resolvedName = (name ?: slug).ifBlank { slug }
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.logo } }.getOrNull()
        }
        val introText = summary?.takeIf { it.isNotBlank() }?.trim() ?: modFile?.let { JarFile(it).readNeoForgeConfig()?.modDescription }?:"暂无介绍"
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