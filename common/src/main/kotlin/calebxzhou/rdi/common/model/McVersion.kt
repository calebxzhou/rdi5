package calebxzhou.rdi.common.model

enum class McVersion(
    val mcVer: String,
    val icon : String,
    val jreVer: Int,
    //预留多loader支持
    val loaderVersions: Map<ModLoader, ModLoader.Version>
) {

    V211(
        "1.21.1" ,
        "assets/icons/mace.png",
        21,
        // "https://piston-meta.mojang.com/v1/packages/a56257b4bc475ecac33571b51b68b33ac046fc72/1.21.1.json",
        mapOf(
            ModLoader.neoforge to ModLoader.Version("neoforge-21.1.216","https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.216/neoforge-21.1.216-installer.jar","62118f0aed41e5a5c2b1b9e4feb35a22c4609548")
        )
    ),
    V201(
        "1.20.1",
        "assets/icons/brush.png",21,
        // "https://piston-meta.mojang.com/v1/packages/9318a951bbc903b54a21463a7eb8c4d451f7b132/1.20.1.json",
        mapOf(
            ModLoader.forge to ModLoader.Version("1.20.1-forge-47.4.13","https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.13/forge-1.20.1-47.4.13-installer.jar","790949ee0cb4671175a806befa370d69008b4b4e")
        )
    ),
    V192(
        "1.19.2",
        "assets/icons/frog.png",21,
        mapOf(
            ModLoader.forge to ModLoader.Version("1.19.2-forge-43.5.2","https://maven.minecraftforge.net/net/minecraftforge/forge/1.19.2-43.5.2/forge-1.19.2-43.5.2-installer.jar","d242b6786039d4acb9ea7579624772b6809bda91")
        )

    ),
    V182(
        "1.18.2",
        "assets/icons/copper.png",21,
        //https://piston-meta.mojang.com/v1/packages/334b33fcba3c9be4b7514624c965256535bd7eba/1.18.2.json
        mapOf(
            ModLoader.forge to ModLoader.Version("1.18.2-forge-40.3.12","https://maven.minecraftforge.net/net/minecraftforge/forge/1.18.2-40.3.12/forge-1.18.2-40.3.12-installer.jar","d7f759dec5b52ddb342c3e12511da1674d0401bf")
        )
    )
    ;

    companion object{
        fun from(mcVer: String): McVersion? = entries.firstOrNull { it.mcVer == mcVer }
    }
}