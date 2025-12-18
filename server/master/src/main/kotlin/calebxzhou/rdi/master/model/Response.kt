package calebxzhou.rdi.master.model

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    val code: Int,
    val msg: String,
    val data: T? = null
) {
}