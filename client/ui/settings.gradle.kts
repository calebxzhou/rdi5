pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

/*plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}*/
include(":common",":client-common")
project(":common").projectDir = file("../../common")
project(":client-common").projectDir = file("../common")