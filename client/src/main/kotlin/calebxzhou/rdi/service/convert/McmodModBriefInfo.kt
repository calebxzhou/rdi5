package calebxzhou.rdi.service.convert

import kotlinx.serialization.Serializable

//mc百科的mod信息
@Serializable
data class McmodModBriefInfo(
    //页面id  /class/{id}.html
    val id: Int,
    val logoUrl: String,
    val name:String,
    //中文名
    val nameCn: String?,
    //一句话介绍
    val intro: String,
)
