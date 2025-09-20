package calebxzhou.rdi.auth

import calebxzhou.rdi.RDI
import calebxzhou.rdi.util.serdesJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.bson.types.ObjectId
import java.io.File
import kotlin.collections.maxByOrNull

@Serializable
data class LoginInfo(
    @Contextual
    val id: ObjectId,
    val pwd: String,
    var lastLoggedTime: Long = System.currentTimeMillis(),
){
}
val localCreds
    get() = LocalCredentials.read()
@Serializable
data class LocalCredentials(
    var loginInfos: MutableMap< @Contextual ObjectId,LoginInfo> = hashMapOf(),
    var carrier: Int = 0,
) {

    companion object {
        val file = File(RDI.DIR, "local_credentials.json")

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
