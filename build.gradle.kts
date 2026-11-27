// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // AGP 9 compiles Kotlin with its built-in Kotlin support (AGP 9.5 bundles
        // Kotlin 2.2.10). Pin the Kotlin Gradle plugin to the version in the catalog.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
}

// The vendored library keeps its original build file, which only declares
// dependencies; apply what its own (Gradle 6) root project used to apply.
project(":sia") {
    apply(plugin = "java-library")
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
    }
}
