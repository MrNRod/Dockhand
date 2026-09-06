import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // AGP 9 dropped com.android.library support for Kotlin Multiplatform modules; this is its
    // replacement, and it configures the Android target from inside the kotlin {} block.
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(25)

    android {
        namespace = "com.mrnrod45.dockhand.composeApp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // D8/R8 lag behind the JVM's own class-file versions; target a D8-safe LTS release.
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        // Off by default for KMP Android libraries. This module ships src/androidMain/res
        // (launcher icon layers, the USB device_filter), and androidApp's manifest references
        // them, so resource processing has to be turned on explicitly.
        androidResources {
            enable = true
        }

        packaging {
            resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }

        // Gives commonTest a compilation to attach to; without it Kotlin warns that the source
        // set is configured but unused.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended) // CMP 1.8+: icons no longer bundled
            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
