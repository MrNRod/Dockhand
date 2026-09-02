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

val macIcon = project.file("packaging/icon.png")

application {
    mainClass.set("com.mrnrod45.dockhand.linux.MainKt")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "-Xdock:name=Dockhand"
    )
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
        jvmArgs(
            "-Xdock:name=Dockhand",
            "-Xdock:icon=${macIcon.absolutePath}"
        )
    }
}

// Native Linux packages via jpackage (bundled with the JDK, no extra plugin needed).
// jpackage also auto-generates the .desktop file + installs the icon — GTK4 removed
// per-window icon APIs, so this is the only way the app gets a taskbar/launcher icon
// on Linux at all, not just a distribution nicety.
//
// Must run on the target OS: `jpackageDeb` needs dpkg-deb (present by default on
// Debian/Ubuntu), `jpackageRpm` needs rpmbuild (`apt install rpm` on Debian/Ubuntu).
fun registerJpackageTask(taskName: String, type: String) = tasks.register<Exec>(taskName) {
    dependsOn("installDist")
    group = "distribution"
    description = "Builds a native .$type package via jpackage."

    val installLibDir = layout.buildDirectory.dir("install/linuxApp/lib")
    val destDir = layout.buildDirectory.dir("jpackage")
    val appIcon = project.file("packaging/icon.png")

    inputs.dir(installLibDir)
    outputs.dir(destDir)
    doFirst { destDir.get().asFile.mkdirs() }

    commandLine(
        "jpackage",
        "--type", type,
        "--name", "Dockhand",
        "--app-version", project.providers.gradleProperty("app.version").get(),
        "--vendor", "mrnrod45",
        "--input", installLibDir.get().asFile.absolutePath,
        "--main-jar", "linuxApp.jar",
        "--main-class", "com.mrnrod45.dockhand.linux.MainKt",
        "--icon", appIcon.absolutePath,
        "--dest", destDir.get().asFile.absolutePath,
        "--linux-shortcut",
        "--linux-menu-group", "Utility",
        // Overrides jpackage's auto-generated .desktop file to add StartupWMClass,
        // matching the GApplication ID set in Main.kt — without it, GNOME Shell's
        // dock can't associate a running window back to this launcher's icon
        // (it falls back to matching by Exec name, which doesn't line up), even
        // though the app-grid icon (read straight from the .desktop file) is fine.
        "--resource-dir", project.file("packaging/linux").absolutePath,
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        // jpackage doesn't auto-detect the GTK4 native dependency (java-gi loads it via
        // FFM at runtime, not a link-time dependency jpackage's own tooling can see).
        "--linux-package-deps", "libgtk-4-1"
    )
}

registerJpackageTask("jpackageDeb", "deb")
registerJpackageTask("jpackageRpm", "rpm")
