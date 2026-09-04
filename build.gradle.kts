plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinJvm).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidKotlinMultiplatformLibrary).apply(false)
    alias(libs.plugins.jetbrainsCompose).apply(false)
    alias(libs.plugins.composeCompiler).apply(false)
}

// Lets packaging scripts (macosApp/package.sh, the Windows CI job) read the single
// source of truth for the app version without re-parsing gradle.properties themselves.
tasks.register("printVersion") {
    doLast { println(providers.gradleProperty("app.version").get()) }
}
