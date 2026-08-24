import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

dependencies {
    // Reuses :core's existing desktop JVM actuals directly — DesktopUsbController,
    // DesktopFileSplitter, and the desktop NetworkServer already work unmodified;
    // this module only supplies a different (GTK4-native) UI layer on top.
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // java-gi: GTK4 bindings for the JVM via the Foreign Function & Memory API
    // (stable since JDK 22 — no --enable-preview needed).
    implementation("io.github.jwharm.javagi:gtk:0.12.2")
}

application {
    mainClass.set("com.mrnrod45.nstk.linux.MainKt")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")

    // macOS-only dev convenience: on a real Linux target, GTK4/GObject libs live on
    // the standard system library search path already and this is a harmless no-op.
    // Homebrew installs outside that path, so java-gi's FFM library lookup needs a hint
    // when smoke-testing this module locally on a Mac before deploying to a Linux VM.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        environment("DYLD_LIBRARY_PATH", "/opt/homebrew/lib")
        environment("DYLD_FALLBACK_LIBRARY_PATH", "/opt/homebrew/lib")
    }
}
