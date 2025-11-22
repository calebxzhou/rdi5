package calebxzhou.rdi.auth

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.server
import com.mojang.authlib.GameProfileRepository
import com.mojang.authlib.ProfileLookupCallback

class RGameProfileRepo : GameProfileRepository {
    override fun findProfilesByNames(
        _names: Array<out String>,
        callback: ProfileLookupCallback
    ) {
        val names = _names.filter { it.isNotBlank() }.toTypedArray()
        server.request<List<RAccount.Dto>>(
            "player/infos",
            params = mapOf("names" to names.joinToString("\n")),
            onErr = {
                callback.onProfileLookupFailed(
                    names.firstOrNull() ?: "未知",
                    IllegalStateException("无法获取profile")
                )
            },
            onOk = { resp ->
                resp.data?.forEach { callback.onProfileLookupSucceeded(it.mcProfile) }
            }
        )

    }
}