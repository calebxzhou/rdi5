package calebxzhou.rdi.model

import calebxzhou.mykotutils.std.jarResource
import calebxzhou.mykotutils.std.readAllString
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.model.ModLoader
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.service.GameService

val McVersion.metadata
    get() = this.jarResource("mcmeta/$mcVer.json").readAllString()
        .let { serdesJson.decodeFromString<MojangVersionManifest>(it) }
val McVersion.baseDir get() = GameService.versionListDir.resolve(mcVer)
val McVersion.nativesDir get() = baseDir.resolve("natives")
val McVersion.firstLoader get() = loaderVersions.keys.first()
val McVersion.firstLoaderVersion get() = loaderVersions.values.first()
val McVersion.firstLoaderDir get() = loaderVersions.values.first().let { GameService.versionListDir.resolve(it.id) }
val McVersion.manifest: MojangVersionManifest get() = metadata//GameService.versionListDir.resolve(mcVer).resolve("$mcVer.json").let { serdesJson.decodeFromString(it.readText()) }
val McVersion.loaderManifest: MojangVersionManifest
    get() = GameService.versionListDir.resolve(firstLoaderVersion.id).resolve(firstLoaderVersion.id + ".json")
        .let { serdesJson.decodeFromString(it.readText()) }

