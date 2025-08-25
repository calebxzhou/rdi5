package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class InstanceOperationResponse(
    val instanceUuid: String
)

@Serializable
data class BatchOperationRequest(
    val instanceUuid: String,
    val daemonId: String
)

@Serializable
data class InstallInstanceRequest(
    val targetUrl: String,
    val title: String,
    val description: String
)
