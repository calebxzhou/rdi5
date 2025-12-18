import org.gradle.jvm.tasks.Jar


plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.2.0"
}

group = "calebxzhou.rdi"
version = "5"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("io.netty:netty-all:4.2.5.Final")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Main-Class" to "calebxzhou.rdi.prox.MainKt")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
