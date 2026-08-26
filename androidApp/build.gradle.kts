import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(25)
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // D8/R8 lag behind the JVM's own class-file versions; target a D8-safe LTS release.
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.mrnrod45.nstk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mrnrod45.nstk"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("app.version.code").get().toInt()
        versionName = providers.gradleProperty("app.version").get()
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    applicationVariants.all(object : org.gradle.api.Action<com.android.build.gradle.api.ApplicationVariant> {
        override fun execute(variant: com.android.build.gradle.api.ApplicationVariant) {
            variant.outputs.forEach { output ->
                (output as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)?.outputFileName = "nstk-android-${variant.versionName}.apk"
            }
        }
    })
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
}
