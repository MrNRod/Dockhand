plugins {
    base
}

val isMac = System.getProperty("os.name").lowercase().contains("mac")

tasks.register<Exec>("assembleApp") {
    group = "build"
    description = "Builds the native macOS .app bundle."
    dependsOn(":core:linkReleaseFrameworkMacosArm64")

    onlyIf { isMac }

    workingDir = projectDir
    commandLine("./package.sh", "--app-only")
}

tasks.register<Exec>("packageDmg") {
    group = "distribution"
    description = "Builds the macOS .app bundle and packages it into a DMG installer."
    dependsOn(":core:linkReleaseFrameworkMacosArm64")

    onlyIf { isMac }

    workingDir = projectDir
    commandLine("./package.sh", "--dmg")
}

tasks.register<Exec>("run") {
    group = "application"
    description = "Builds and launches the native macOS .app bundle."
    dependsOn("assembleApp")

    onlyIf { isMac }

    commandLine("open", layout.buildDirectory.dir("dist/Dockhand.app").get().asFile.absolutePath)
}

tasks.named("assemble") {
    if (isMac) {
        dependsOn("assembleApp")
    }
}
