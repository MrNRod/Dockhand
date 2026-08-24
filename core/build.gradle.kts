import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    jvmToolchain(25)

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    macosArm64 {
        compilations.getByName("main") {
            cinterops {
                create("libusb") {
                    defFile(project.file("src/nativeInterop/cinterop/libusb.def"))
                    packageName("libusb.cinterop")
                }
            }
            compilerOptions.options.freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
        }
        binaries.framework {
            baseName = "core"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-ktx:1.9.0")
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.documentfile)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("io.github.dsheirer:usb4java:1.3.5")
                implementation("io.github.dsheirer:usb4java-native-libraries:1.3.1")
            }
        }
        val macosArm64Main by getting {
            dependencies {
                implementation("io.ktor:ktor-network:3.0.3")
            }
        }
    }
}

android {
    namespace = "com.mrnrod45.nstk.core"
    compileSdk = 34

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
