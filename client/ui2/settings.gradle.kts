pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include(":common",":client-common")
project(":common").projectDir = file("../../common")
project(":client-common").projectDir = file("../common")