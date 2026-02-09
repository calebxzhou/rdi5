package calebxzhou.rdi.client.ui2.screen

import kotlinx.serialization.Serializable


@Serializable
object Wardrobe
@Serializable data class HostCreate(
    val modpackId: String,
    val modpackName: String,
    val packVer: String,
    val skyblock: Boolean,
    val hostId: String? = null,
)
@Serializable object ModpackList
@Serializable data class ModpackInfo(val modpackId: String, val fromHostId: String? = null)
@Serializable object ModpackUpload
@Serializable object Login
@Serializable object Register
@Serializable object Setting
@Serializable object HostList
@Serializable data class HostInfo(val hostId: String)
@Serializable object Mail
@Serializable object WorldList
@Serializable object TaskView
@Serializable object McPlayView
@Serializable data class RMcVersion(val mcVer: String? = null)
