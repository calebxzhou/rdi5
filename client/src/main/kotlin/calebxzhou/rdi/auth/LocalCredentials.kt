package calebxzhou.rdi.auth

import calebxzhou.rdi.RDI
import calebxzhou.rdi.util.serdesJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class LoginInfo(
    val id: String,
    val pwd: String,
    var lastLoggedTime: Long = System.currentTimeMillis(),
)

@Serializable
data class LocalCredentials(
    var loginInfos: List<LoginInfo> = arrayListOf()
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
        get() = loginInfos.maxByOrNull { it.lastLoggedTime }
    fun write() = serdesJson.encodeToString(this).let { file.writeText(it) }
}
