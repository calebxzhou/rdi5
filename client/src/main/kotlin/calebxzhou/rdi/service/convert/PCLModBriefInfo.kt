package calebxzhou.rdi.service.convert

data class PCLModBriefInfo(
    val mcmodId: Int,
    val nameCn: String?,
    // mod通名
    val curseforgeSlugs: List<String>,
    val modrinthSlugs: List<String>,
)