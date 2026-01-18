package calebxzhou.rdi.client.ui2.screen

import kotlinx.serialization.Serializable


@Serializable
object Wardrobe
@Serializable data class HostCreate(
    val modpackId: String,
    val modpackName: String,
    val packVer: String,
    val skyblock: Boolean,
)
@Serializable object ModpackList
@Serializable data class ModpackInfo(val modpackId: String, val fromHostId: String? = null)
@Serializable object ModpackManage
@Serializable object ModpackUpload
@Serializable object Login
@Serializable object Profile
@Serializable object HostList
@Serializable data class HostInfo(val hostId: String)
@Serializable object Mail
@Serializable object WorldList
@Serializable object TaskView
