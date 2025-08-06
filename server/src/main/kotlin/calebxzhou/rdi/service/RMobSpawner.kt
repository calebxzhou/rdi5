package calebxzhou.rdi.service

import calebxzhou.rdi.RDI
import calebxzhou.rdi.util.mcs
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.NaturalSpawner
import net.minecraft.world.level.chunk.LevelChunk

object RMobSpawner {
    var cooldownTicks = 0

    @JvmStatic
    fun spawnForChunk(
        lvl: ServerLevel,
        chunk: LevelChunk,
        state: NaturalSpawner.SpawnState,
        spawnFriendlies: Boolean,
        spawnMonsters: Boolean,
        forceDespawn: Boolean
    ) {
        //每2.5秒生成20次
        if (cooldownTicks >= 50) {
            mcs.execute {
                for (i in 1..20) {
                    NaturalSpawner.spawnForChunk(lvl, chunk, state, spawnFriendlies, spawnMonsters, forceDespawn)
                }
            }
            cooldownTicks = 0
        } else {
            cooldownTicks++
        }
    }

}