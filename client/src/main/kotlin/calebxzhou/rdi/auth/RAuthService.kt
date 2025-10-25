package calebxzhou.rdi.auth

import com.mojang.authlib.GameProfileRepository
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import java.net.Proxy

class RAuthService: YggdrasilAuthenticationService(Proxy.NO_PROXY) {
    override fun createMinecraftSessionService() = RSessionService()

    override fun createProfileRepository(): GameProfileRepository {
       return RGameProfileRepo()
    }
}