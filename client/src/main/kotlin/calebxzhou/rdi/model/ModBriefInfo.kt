package calebxzhou.rdi.model

data class ModBriefInfo(
    //页面id  /class/{id}.html
    val mcmodId: Int,
    val logoUrl: String,
    val name:String,
    val nameCn: String?,
    // mod通名
    val curseforgeSlugs: List<String>,
    val modrinthSlugs: List<String>,
) {
}