package calebxzhou.rdi.auth

import com.mojang.authlib.AuthenticationService
import com.mojang.authlib.GameProfileRepository
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import snownee.jade.overlay.DatapackBlockManager.override
import java.net.Proxy

class RAuthService: YggdrasilAuthenticationService(Proxy.NO_PROXY) {
    override fun createMinecraftSessionService() = RSessionService()

    override fun createProfileRepository(): GameProfileRepository? {
       return null
        //todo
    }
}