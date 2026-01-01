package calebxzhou.rdi.model

import calebxzhou.mykotutils.std.jarResource
import calebxzhou.rdi.common.model.ModLoader
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.service.GameService

enum class McVersion(
    val mcVer: String,
    //预留多loader支持
    val loaderVersions: Map<ModLoader, ModLoader.Version>
) {
    /*V201(
        "1.20.1",
        "https://piston-meta.mojang.com/v1/packages/9318a951bbc903b54a21463a7eb8c4d451f7b132/1.20.1.json",
        mapOf(
            ModLoader.FORGE to ModLoader.Version("1.20.1-forge-47.4.13","https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.13/forge-1.20.1-47.4.13-installer.jar","790949ee0cb4671175a806befa370d69008b4b4e")
        )
    ),*/
    V211(
        "1.21.1" ,
       // "https://piston-meta.mojang.com/v1/packages/a56257b4bc475ecac33571b51b68b33ac046fc72/1.21.1.json",
        mapOf(
            ModLoader.NEOFORGE to ModLoader.Version("neoforge-21.1.216","https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.216/neoforge-21.1.216-installer.jar","62118f0aed41e5a5c2b1b9e4feb35a22c4609548")
        )
    )
    ;
    val metadata get() = this.jarResource("mcmeta/$mcVer.json").readAllBytes().decodeToString().let { serdesJson.decodeFromString<MojangVersionManifest>(it) }
    val baseDir get() = GameService.versionListDir.resolve(mcVer)
    val nativesDir get() = baseDir.resolve("natives")
    val firstLoader get() = loaderVersions.values.first()
    val firstLoaderDir get() = loaderVersions.values.first().let { GameService.versionListDir.resolve(it.id) }
    val manifest: MojangVersionManifest get() = metadata//GameService.versionListDir.resolve(mcVer).resolve("$mcVer.json").let { serdesJson.decodeFromString(it.readText()) }
    val loaderManifest: MojangVersionManifest get() = GameService.versionListDir.resolve(firstLoader.id).resolve(firstLoader.id + ".json").let { serdesJson.decodeFromString(it.readText()) }

    companion object{
        fun from(mcVer: String): McVersion? = entries.firstOrNull { it.mcVer == mcVer }
    }
}
