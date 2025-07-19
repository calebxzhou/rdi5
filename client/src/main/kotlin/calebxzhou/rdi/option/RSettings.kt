package calebxzhou.rdi.option

import calebxzhou.rdi.lgr
import calebxzhou.rdi.util.serdesJson
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
class RSettings {

    var autoAdjustWindowSize = true

    companion object {
        private val file
            get() = File("rdi", "settings.json")
        var now = RSettings()
        fun save() {
            if(!file.exists())
                file.createNewFile()
            val json = serdesJson.encodeToString(now)
            file.writeText(json)
        }

        fun load() {
            try {
                val json = file.readText()
                lgr.info(json)
                now= serdesJson.decodeFromString<RSettings>(json)
            } catch (e: Exception) {
                lgr.error("读取配置失败")
            }
        }
    }
}