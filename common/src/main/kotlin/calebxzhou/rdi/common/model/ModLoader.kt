package calebxzhou.rdi.common.model

enum class ModLoader {
    forge,neoforge;
    companion object{
        fun from(name:String):ModLoader?{
            val normalized = name.trim().substringBefore('-')
            return entries.find { it.name.equals(normalized, true) }
        }
    }
    class Version(
        //version目录的名字
        val id: String,
        val installerUrl: String,
        val installerSha1: String
    )
}
