import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // AGP 9 dropped com.android.library support for Kotlin Multiplatform modules; this is its
    // replacement, and it configures the Android target from inside the kotlin {} block.
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "com.mrnrod45.dockhand.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // D8/R8 lag behind the JVM's own class-file versions; target a D8-safe LTS
        // release here even though the rest of this module (desktop) targets 25.
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        // commonTest has real tests; without this the new plugin creates no Android host-test
        // compilation and they would silently stop running on the Android target.
        withHostTest {}
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
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-ktx:1.9.0")
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.documentfile)
        }
        // Custom-named jvm target, so there is no generated accessor for it.
        getByName("desktopMain").dependencies {
            implementation("io.github.dsheirer:usb4java:1.3.5")
            implementation("io.github.dsheirer:usb4java-native-libraries:1.3.1")
        }
        macosArm64Main.dependencies {
            implementation("io.ktor:ktor-network:3.0.3")
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
