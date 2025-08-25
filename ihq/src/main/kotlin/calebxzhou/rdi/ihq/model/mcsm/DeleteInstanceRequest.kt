package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class DeleteInstanceRequest(
    val uuids: List<String>, // Instance Id list
    val deleteFile: Boolean = false // Whether to delete instance files
)
