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

@Serializable
data class LocalCredentials(
    var loginInfos: MutableMap< @Contextual ObjectId,LoginInfo> = hashMapOf()
) {

    companion object {
        val file = File(RDI.DIR, "local_credentials.json")

        fun read() = try {
            if (!file.exists()){
                file.createNewFile()
                LocalCredentials().write()
            }
            serdesJson.decodeFromString<LocalCredentials>(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            LocalCredentials()
        }
    }
    val lastLogged
        get() = loginInfos.values.maxByOrNull { it.lastLoggedTime }
    fun write() = serdesJson.encodeToString(this).let { file.writeText(it) }
}
