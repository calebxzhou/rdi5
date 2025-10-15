package calebxzhou.rdi.model

data class ModBriefVo(
    val name: String,
    val nameCn: String?,
    val intro: String,
    //jar里的icon 不一定有
    val iconData: ByteArray?,
    //curseforge的icon&mc百科的icon  哪个能用用哪个
    val iconUrls: List<String>
) {
}