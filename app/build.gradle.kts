plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "fr.vinetos.tranquille"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "fr.vinetos.tranquille"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "0.2.0"
        resValue("string", "app_id", "fr.vinetos.tranquille")

        javaCompileOptions {
            annotationProcessorOptions {
                argument("eventBusIndex", "fr.vinetos.tranquille.EventBusIndex")
            }
        }
    }
    // Release signing is configured only when the keystore is provided through
    // environment variables (see .github/workflows/release.yml). Without them
    // the release build stays unsigned, as before.
    val releaseKeystore: String? = System.getenv("TRANQUILLE_KEYSTORE_FILE")
    signingConfigs {
        if (!releaseKeystore.isNullOrEmpty()) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("TRANQUILLE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("TRANQUILLE_KEY_ALIAS")
                keyPassword = System.getenv("TRANQUILLE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            // R8 removes unused code (mostly from Compose and the libraries); names stay
            // readable (-dontobfuscate in proguard-rules.pro). Keep rules cover the code
            // that is only reached by reflection.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = false
        lintConfig = file("lint.xml")
    }
    buildFeatures {
        buildConfig = true
        resValues = true
        compose = true
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("fr.vinetos.tranquille.data")
            dialect(libs.sqldelight.dialects.sql)
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.work.runtime)
    implementation(libs.slf4j)
    implementation(libs.conscrypt)
    implementation(libs.okhttp)
    implementation(project(":sia"))
    implementation(libs.commons.csv)
    implementation(libs.sqldelight.driver)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.bundles.sqlite)
    implementation(libs.eventbus)

    // todo add kotlin coriutines dependency
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    annotationProcessor(libs.eventbus.annotation.processor)

    runtimeOnly(libs.logback)

    testImplementation(libs.junit)
    testImplementation(libs.sqldelight.sqlite.driver)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}