package calebxzhou.rdi.client.ui2

object ModpackUploadStore {
    data class Preset(
        val updateModpackId: org.bson.types.ObjectId? = null,
        val updateModpackName: String? = null
    )

    var preset: Preset? = null
}
