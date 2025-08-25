package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInstanceResponse(
    val instanceUuid: String
)
