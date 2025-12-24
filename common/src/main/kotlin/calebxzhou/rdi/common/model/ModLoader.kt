package calebxzhou.rdi.common.model

enum class ModLoader {
    FORGE,NEOFORGE;
    class Version(
        //version目录的名字
        val id: String,
        val installerUrl: String,
        val installerSha1: String
    )
}