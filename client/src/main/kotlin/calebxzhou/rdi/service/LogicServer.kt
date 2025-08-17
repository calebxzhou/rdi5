package calebxzhou.rdi.service

import calebxzhou.rdi.RDI
import net.minecraft.server.MinecraftServer
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.packs.repository.ServerPacksSource
import net.minecraft.world.level.storage.LevelStorageSource
import java.nio.file.Path

object LogicServer {
    val path: Path = RDI.DIR.toPath().resolve("logic_server").also { it.toFile().mkdir() }
    fun start(){
        /*val lvlAccess = LevelStorageSource.createDefault(path.resolve("saves")).createAccess("rdi")
        MinecraftServer.spin {
            DedicatedServer(it,
                lvlAccess,
                ServerPacksSource.createPackRepository(lvlAccess),



                )
        }*/
    }
}