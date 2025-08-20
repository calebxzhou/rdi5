package calebxzhou.rdi.auth

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.serdesJson
import com.mojang.authlib.GameProfileRepository
import com.mojang.authlib.ProfileLookupCallback
import kotlinx.coroutines.launch

class RGameProfileRepo : GameProfileRepository {
    override fun findProfilesByNames(
        _names: Array<out String>,
        callback: ProfileLookupCallback
    ) {
        val names = _names.filter { it.isNotBlank() }.toTypedArray()
        ioScope.launch {
            val resp = RServer.now?.prepareRequest(
                false,
                "player-info-by-names",
                params = listOf("names" to names.joinToString("\n"))
            )
                ?: return@launch callback.onProfileLookupFailed(
                    names.firstOrNull() ?: "",
                    IllegalStateException("当前没有连接到服务器 无法获取profile")
                )
            try {
                val body = resp.body
                lgr.info("批量获取profile: $body")
                val infos = serdesJson.decodeFromString<List<RAccount.Dto>>(body)

                infos.forEach { callback.onProfileLookupSucceeded(it.mcProfile) }

            } catch (e: Exception) {
                callback.onProfileLookupFailed("解析profile失败",e)
            }


        }
    }
}