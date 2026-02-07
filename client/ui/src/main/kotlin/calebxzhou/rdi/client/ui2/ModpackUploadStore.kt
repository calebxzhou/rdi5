package calebxzhou.rdi.client.ui2

import calebxzhou.rdi.client.service.ModpackService.UploadPayload
import calebxzhou.rdi.common.model.Mod

object ModpackUploadStore {
    data class Preset(
        val payload: UploadPayload,
        val mods: List<Mod>,
        val requireDownload: Boolean
    )

    var preset: Preset? = null
}

