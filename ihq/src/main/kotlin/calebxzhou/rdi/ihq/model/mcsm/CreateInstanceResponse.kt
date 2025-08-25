package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class CreateInstanceResponse(
    val instanceUuid: String,
    val config: InstanceConfig
)
