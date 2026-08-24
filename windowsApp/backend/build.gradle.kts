import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

dependencies {
    // Reuses :core's existing desktop JVM actuals directly — same story as linuxApp.
    // This process is a thin JSON-RPC shell around DesktopUsbController/DesktopFileSplitter/
    // NetworkServer, launched as a subprocess by the WinUI frontend (see ../NstkWindowsApp).
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

application {
    mainClass.set("com.mrnrod45.nstk.windows.MainKt")
}

tasks.register<Jar>("fatJar") {
    group = "distribution"
    description = "Builds a single self-contained jar for the WinUI frontend to launch as a subprocess."
    archiveFileName.set("nstk-windows-backend.jar")
    manifest { attributes["Main-Class"] = "com.mrnrod45.nstk.windows.MainKt" }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
