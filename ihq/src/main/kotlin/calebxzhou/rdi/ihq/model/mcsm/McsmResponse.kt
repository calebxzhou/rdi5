package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class McsmResponse<T>(
    val status: Int,
    val data: T,
    val time: Long,
){

}