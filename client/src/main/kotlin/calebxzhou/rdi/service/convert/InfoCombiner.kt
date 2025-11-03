package calebxzhou.rdi.service.convert

import calebxzhou.rdi.model.ModBriefInfo
import calebxzhou.rdi.util.gson
import calebxzhou.rdi.util.serdesJson
import java.io.File

/**
 * calebxzhou @ 2025-10-14 23:28
 */
fun main() {
    val mcmodData = File("mcmod_mod_data.json").readText().let { serdesJson.decodeFromString<List<McmodModBriefInfo>>(it) }
    val pclData = File("pcl_mod_data.json").readText().let { serdesJson.decodeFromString<List<PCLModBriefInfo>>(it) }

    val combined = combineModBriefInfo(mcmodData, pclData)

    File("mod_brief_info.json").writeText(combined.gson)
    println("Combined ${combined.size} mods")
}

fun combineModBriefInfo(
    mcmodData: List<McmodModBriefInfo>,
    pclData: List<PCLModBriefInfo>
): List<ModBriefInfo> {
    val pclByMcmodId = pclData.associateBy { it.mcmodId }

    return mcmodData.map { mcmodInfo ->
        val pclInfo = pclByMcmodId[mcmodInfo.id]

        val nameCn = mcmodInfo.nameCn ?: pclInfo?.nameCn
        val curseforgeSlugs = pclInfo?.curseforgeSlugs ?: emptyList()
        val modrinthSlugs = pclInfo?.modrinthSlugs ?: emptyList()

        ModBriefInfo(
            mcmodId = mcmodInfo.id,
            logoUrl = mcmodInfo.logoUrl,
            name = mcmodInfo.name,
            nameCn = nameCn,
            intro = mcmodInfo.intro,
            curseforgeSlugs = curseforgeSlugs,
            modrinthSlugs = modrinthSlugs
        )
    }
}