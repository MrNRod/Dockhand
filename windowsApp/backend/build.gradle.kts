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

// Bundles a minimal JRE via jlink so the MSI installer doesn't require end users to
// install Java themselves — BackendClient.cs launches this bundled java.exe instead of
// relying on PATH. Must run on the same JDK the CI job's JAVA_HOME points at (a
// downloaded Windows JDK 25), since jlink needs matching-platform jmods.
//
// Uses the full java.se module set rather than a hand-trimmed list: usb4java is a
// non-modular JNI bridge, so jdeps/jlink's automatic dependency analysis can't fully
// see through its reflection/sun.misc.Unsafe-style buffer access. Correctness over a
// few extra MB.
tasks.register<Exec>("jlinkRuntime") {
    group = "distribution"
    description = "Builds a minimal bundled JRE via jlink for the Windows MSI installer."

    val javaHome = System.getProperty("java.home")
    val outputDir = layout.buildDirectory.dir("runtime")

    inputs.property("javaHome", javaHome)
    outputs.dir(outputDir)

    doFirst {
        // jlink refuses to run if the output directory already exists.
        outputDir.get().asFile.deleteRecursively()
    }

    val jlinkExe = if (System.getProperty("os.name").lowercase().contains("windows"))
        "$javaHome/bin/jlink.exe" else "$javaHome/bin/jlink"

    commandLine(
        jlinkExe,
        "--module-path", "$javaHome/jmods",
        "--add-modules", "java.se,jdk.unsupported,jdk.crypto.ec,jdk.crypto.cryptoki",
        "--output", outputDir.get().asFile.absolutePath,
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages"
    )
}
