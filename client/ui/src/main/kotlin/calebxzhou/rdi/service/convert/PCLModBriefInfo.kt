package calebxzhou.rdi.service.convert

import kotlinx.serialization.Serializable

@Serializable
data class PCLModBriefInfo(
    val mcmodId: Int,
    val nameCn: String?=null,
    // mod通名
    val curseforgeSlugs: List<String>,
    val modrinthSlugs: List<String>,
)