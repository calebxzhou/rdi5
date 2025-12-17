package calebxzhou.rdi.model

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.WEB_USER_AGENT
import calebxzhou.rdi.util.serdesJson
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class GeoLocation(
    val continent: String,
    val country: String,
    val zipcode: String,
    val owner: String,
    val isp: String,
    val adcode: String,
    val prov: String,
    val city: String,
    val district: String
){
    companion object{
        val DEFAULT =  GeoLocation(
            continent = "未知",
            country = "未知",
            zipcode = "未知",
            owner = "未知",
            isp = "未知",
            adcode = "000000",
            prov = "未知",
            city = "未知",
            district = "未知"
        )
        var now = DEFAULT
        suspend fun get(): GeoLocation {
            val resp = calebxzhou.rdi.net.httpRequest {
                url("https://qifu.baidu.com/ip/local/geo/v1/district")
                method = io.ktor.http.HttpMethod.Get
                header("Referer", "https://qifu.baidu.com/?activeKey=SEARCH_IP&trace=apistore_ip_aladdin&activeId=SEARCH_IP_ADDRESS&ip=")
                header("User-Agent", WEB_USER_AGENT)
                header("Host", "qifu.baidu.com")
                header("Content-Type", "application/json")
            }
            try {
                // Parse JSON response without creating classes
                val jsonElement = serdesJson.parseToJsonElement(resp.bodyAsText())
                val jsonObject = jsonElement.jsonObject

                val dataElement = jsonObject["data"]
                if (dataElement == null || dataElement.jsonPrimitive.content.isEmpty()) {
                    lgr.error("Empty data field in response")
                    return DEFAULT
                }
                // Extract location fields from the data object
                return serdesJson.decodeFromString<GeoLocation>(dataElement.jsonPrimitive.content).also { now=it }
            } catch (e: Exception) {
                lgr.error("无法获取位置信息 ${e.message}")
                lgr.error(resp.bodyAsText())
                return DEFAULT
            }
        }
    }
}
