package calebxzhou.rdi.common.model

import kotlinx.serialization.Serializable

//展示modcard的信息
@Serializable
data class ModCardVo(
    val name: String,
    val nameCn: String?=null,
    val intro: String="",
    //jar里的icon 不一定有
    val iconData: ByteArray?=null,
    //curseforge的icon&mc百科的icon  哪个能用用哪个
    val iconUrls: List<String> =emptyList(),
) {
}