package calebxzhou.rdi.model

import kotlinx.serialization.Serializable

@Serializable
data class RBlockState(
    //res loca
    val name: String,
    val props: Map<String, String> = hashMapOf()
)
