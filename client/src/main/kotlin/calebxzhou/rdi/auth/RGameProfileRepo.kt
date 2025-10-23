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
        _root_ide_package_.calebxzhou.rdi.net.server.request<List<RAccount.Dto>>(
            "player-info-by-names",
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