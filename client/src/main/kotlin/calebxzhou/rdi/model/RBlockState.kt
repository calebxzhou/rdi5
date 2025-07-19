package calebxzhou.rdi.model

import kotlinx.serialization.Serializable

@Serializable
data class RBlockState(
    val name: String,
    val props: Map<String, String> = hashMapOf()
)
