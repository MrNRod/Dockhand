plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(25)
}

android {
    namespace = "com.mrnrod45.nstk"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mrnrod45.nstk"
        minSdk = 26
        targetSdk = 34
        versionCode = providers.gradleProperty("app.version.code").get().toInt()
        versionName = providers.gradleProperty("app.version").get()
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
}
