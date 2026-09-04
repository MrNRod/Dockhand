plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.mrnrod45.dockhand"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mrnrod45.dockhand"
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

}

// Names the APK after the app version. The legacy `applicationVariants` API this used to go
// through was removed from AGP 9's DSL; VariantOutput.outputFileName is the supported equivalent.
androidComponents {
    val appVersion = providers.gradleProperty("app.version")
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(appVersion.map { "dockhand-android-$it.apk" })
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
}
