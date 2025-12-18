import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

val kotlinVersion: String by project// = providers.gradleProperty("kotlin_version").get()
val ktorVersion = providers.gradleProperty("ktor_version").get()
val modId = providers.gradleProperty("mod_id").get()
val modGroupId = providers.gradleProperty("mod_group_id").get()
val modVersion = providers.gradleProperty("mod_version").get()
val neoVersion = providers.gradleProperty("neo_version").get()
val parchmentMinecraftVersion = providers.gradleProperty("parchment_minecraft_version").get()
val parchmentMappingsVersion = providers.gradleProperty("parchment_mappings_version").get()
val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val minecraftVersionRange = providers.gradleProperty("minecraft_version_range").getOrElse("")
val neoVersionRange = providers.gradleProperty("neo_version_range").getOrElse("")
val loaderVersionRange = providers.gradleProperty("loader_version_range").getOrElse("")

plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.neoforged.moddev") version "2.0.124"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("auto-embed-jarjar-transitives") version "0.1.0"
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

version = modVersion
group = modGroupId

repositories {
    mavenLocal()
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content {
            includeGroup("thedarkcolour")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://cursemaven.com")
            }
        }
        filter {
            includeGroup("curse.maven")
        }
    } 
}

base {
    archivesName.set(modId)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion = parchmentMappingsVersion
        minecraftVersion = parchmentMinecraftVersion
    }

    validateAccessTransformers = true

    runs {
        register("server") {
            server()
            jvmArgument("-Xmx4G")
            programArgument("--nogui")
            systemProperty("rdi.debug", "true")
            systemProperty("neoforge.enabledGameTestNamespaces", "rdi")
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            val sourceSets = the<SourceSetContainer>()
            sourceSet(sourceSets.named("main").get())
        }
    }
}

sourceSets.named("main") {
    resources.srcDir("src/generated/resources")
}

val localRuntimeConfiguration = configurations.maybeCreate("localRuntime")
val librariesConfiguration = configurations.maybeCreate("libraries")
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntimeConfiguration)
}
configurations.named("implementation") {
    extendsFrom(librariesConfiguration)
}

librariesConfiguration.isTransitive = true

allprojects {
    configurations.all {
        resolutionStrategy {
            //for ktor
            force("org.slf4j:slf4j-api:2.0.16")
        }
    }
}

val mcLibs = listOf(
    "io.ktor:ktor-client-okhttp:$ktorVersion",
    "io.ktor:ktor-client-core:$ktorVersion",
    "io.ktor:ktor-client-encoding:$ktorVersion",
    "io.ktor:ktor-client-websockets:$ktorVersion",
    "org.mongodb:bson:5.1.0",
    "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0",
    "io.ktor:ktor-client-content-negotiation:$ktorVersion",
    "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion"
)

val modrinthMods = listOf("lithium:mc1.21.1-0.15.0-neoforge")

val autoEmbedJarJarExcludeCoords = setOf(
    //--kotlin for forge已经有了
    "org.jetbrains.kotlin:kotlin-stdlib",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm",
    "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm",
    "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm",
    //--
    "org.slf4j:slf4j-api",
    "com.google.errorprone:error_prone_annotations",
    "org.ow2.asm:asm",
    "org.ow2.asm:asm-commons",
    "org.ow2.asm:asm-tree",
    "org.ow2.asm:asm-analysis",
    "org.ow2.asm:asm-util"
)

autoEmbedJarJarTransitives {
    jarJarExcludeCoords.addAll(autoEmbedJarJarExcludeCoords)
}

dependencies {
    testImplementation(kotlin("test"))

    mcLibs.forEach { lib ->
        add("libraries", lib)
        autoEmbedJarJarTransitives.include(lib)
        add("additionalRuntimeClasspath", lib)
    }

    modrinthMods.forEach { dep ->
        implementation("maven.modrinth:$dep")
    }
}

val metadataOutput = layout.buildDirectory.dir("generated/sources/modMetadata")

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "neo_version_range" to neoVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to providers.gradleProperty("mod_name").get(),
        "mod_license" to providers.gradleProperty("mod_license").get(),
        "mod_version" to modVersion,
        "mod_authors" to providers.gradleProperty("mod_authors").get(),
        "mod_description" to providers.gradleProperty("mod_description").get()
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(metadataOutput)
}

sourceSets.named("main") {
    resources.srcDir(metadataOutput)
}

neoForge.ideSyncTask(generateModMetadata)

val localRepoDir = project.layout.projectDirectory.dir("repo")

publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = localRepoDir.asFile.toURI()
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

tasks.register<Copy>("出imgBuild") {
    dependsOn(tasks.named("build"))
    notCompatibleWithConfigurationCache("Executes build.bat post copy")
    val targetDir = File(System.getProperty("user.home"), "Documents\\rdi5skypro\\server_image_test")
    from(layout.buildDirectory.file("libs/rdi-${'$'}{version}.jar"))
    into(File(targetDir, "mods"))
    doLast {
        /*project.exec {
            workingDir = targetDir
            commandLine("cmd", "/c", "build.bat")
        }*/
    }
}
