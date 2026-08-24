import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        }
    }
    
    jvmToolchain(25)

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":composeApp"))
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.mrnrod45.nstk.MainKt"
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")

        nativeDistributions {
            // macOS packaging removed — macOS now ships as the native SwiftUI app in
            // /macosApp. This module currently targets Windows + Linux only (see
            // README.md for the plan to retire it once windowsApp/linuxApp are verified).
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "nstk"
            packageVersion = providers.gradleProperty("app.version").get()
            vendor = "Noel Rodriguez-Lebron"

            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                menuGroup = "Noel Rodriguez-Lebron"
                console = false
                upgradeUuid = "D3D51849-6AB6-499A-A39A-66AA53D705E7"
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
                shortcut = true
            }
        }
    }
}
