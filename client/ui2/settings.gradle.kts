pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
rootProject.name = "ui2"
include(":common")
project(":common").projectDir = file("../../common")