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
                implementation(project(":shared"))
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
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "nstk"
            packageVersion = providers.gradleProperty("app.version").get()

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                entitlementsFile.set(project.file("entitlements.plist"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}

// Entitlements might need to be copied or referenced if they were in root/composeApp
// Need to check where 'entitlements.plist' is. It was likely in composeApp root.
// We moved the folder to 'shared'. So 'shared/entitlements.plist'.
// We should probably move it to desktopApp?
