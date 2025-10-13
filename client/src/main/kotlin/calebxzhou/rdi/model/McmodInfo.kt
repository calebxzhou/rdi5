package calebxzhou.rdi.model

//mc百科的mod信息
data class McmodInfo(
    val pageUrl: String,
    val logoUrl: String,
    val name:String,
    //中文名
    val nameCn: String,
    val categories: List<String>,
    //介绍
    val intro: String,
    //作者
    val authors: List<ModAuthor>
)
