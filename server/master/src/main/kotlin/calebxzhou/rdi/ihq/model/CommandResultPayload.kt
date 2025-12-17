package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Serializable

@Serializable
data class CommandResultPayload(
    val command: String,
    val output: String,
    val success: Boolean,
)
