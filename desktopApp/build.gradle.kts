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
        if (System.getProperty("os.name").lowercase().contains("linux")) {
            jvmArgs += "-Dsun.java2d.uiScale=2.0"
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "nstk"
            packageVersion = providers.gradleProperty("app.version").get()
            vendor = "Noel Rodriguez"

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                entitlementsFile.set(project.file("entitlements.plist"))
                
                dmg {
                    iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                }
                pkg {
                    // PKG usually takes top-level iconFile, but explicit setting ensures it
                    iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                }
            }
            windows {
                menuGroup = "Noel Rodriguez"
                console = false
                upgradeUuid = "D3D51849-6AB6-499A-A39A-66AA53D705E7"
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
                shortcut = true
            }
        }
    }
}

// Entitlements might need to be copied or referenced if they were in root/composeApp
// Need to check where 'entitlements.plist' is. It was likely in composeApp root.
// We moved the folder to 'shared'. So 'shared/entitlements.plist'.
// We should probably move it to desktopApp?
