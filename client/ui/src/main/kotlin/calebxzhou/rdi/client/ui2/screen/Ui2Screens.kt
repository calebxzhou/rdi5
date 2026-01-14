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
@Serializable object ModpackManage
@Serializable object Login
@Serializable object Profile