package calebxzhou.rdi.model

enum class ModLoader {
    FORGE,NEOFORGE;
    class Meta(
        val installerUrl: String,
        val installerSha1: String
    )
}