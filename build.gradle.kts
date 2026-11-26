// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
}

// Project build.gradle.kts file.
buildscript {
    extra["kotlin_version"] = "1.8.0"
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    }
}

// The vendored library keeps its original build file, which only declares
// dependencies; apply what its own (Gradle 6) root project used to apply.
project(":sia") {
    apply(plugin = "java-library")
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
    }
}
