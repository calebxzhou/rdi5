package calebxzhou.rdi.auth

import calebxzhou.rdi.util.serdesJson
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class LocalCredentials(
    var lastLoggedId: String = "",
    var idPwds: MutableMap<String, String> = hashMapOf()
) {
    companion object {
        val file = File("rdi", "local_credentials.json")
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

    fun write() = serdesJson.encodeToString(this).let { file.writeText(it) }
}
