package calebxzhou.rdi.client.auth

import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.model.LoginInfo
import calebxzhou.rdi.common.serdesJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.io.File


@Serializable
data class LocalCredentials(
    var loginInfos: MutableMap< @Contextual ObjectId,LoginInfo> = hashMapOf(),
    var carrier: Int = 0,
) {

    companion object {
        val file = File(RDIClient.DIR, "local_credentials.json")

        fun read() = try {
            if (!file.exists()){
                file.createNewFile()
                LocalCredentials().save()
            }
            serdesJson.decodeFromString<LocalCredentials>(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            LocalCredentials()
        }
    }
    val lastLogged
        get() = loginInfos.values.maxByOrNull { it.lastLoggedTime }
    fun save() = serdesJson.encodeToString(this).let { file.writeText(it) }
}
