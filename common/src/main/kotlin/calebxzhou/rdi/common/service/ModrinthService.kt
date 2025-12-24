package calebxzhou.rdi.service

import calebxzhou.rdi.common.model.ModBriefInfo
import calebxzhou.rdi.common.service.ModService
import calebxzhou.rdi.common.service.ModService.briefInfo

object ModrinthService {
    val slugBriefInfo: Map<String, ModBriefInfo> by lazy { ModService.buildSlugMap(briefInfo) { it.modrinthSlugs } }
    const val BASE_URL = "https://mod.mcimirror.top/modrinth/v2"
    const val OFFICIAL_URL = "https://api.modrinth.com/v2"

    //mr - cf slug, 没查到就返回自身
    val String.mr2CfSlug: String
        get() {
            if (isBlank()) return this
            val info = ModrinthService.slugBriefInfo[trim().lowercase()] ?: return this
            return info.curseforgeSlugs.firstOrNull { it.isNotBlank() }?.trim() ?: this
        }

}
