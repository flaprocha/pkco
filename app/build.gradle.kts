plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing (upload key). All four properties must be set outside the repo, for example in
// the user-level ~/.gradle/gradle.properties. Without them, debug builds and tests work as before
// and release builds stop with a message (see the task check below).
val releaseStoreFile = providers.gradleProperty("PKCO_UPLOAD_STORE_FILE")
val releaseStorePassword = providers.gradleProperty("PKCO_UPLOAD_STORE_PASSWORD")
val releaseKeyAlias = providers.gradleProperty("PKCO_UPLOAD_KEY_ALIAS")
val releaseKeyPassword = providers.gradleProperty("PKCO_UPLOAD_KEY_PASSWORD")
val releaseSigningReady = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { it.orNull?.isNotBlank() == true }

android {
    namespace = "com.example.pkco"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.flaprocha.pkco"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Values come only from Gradle properties (user-level ~/.gradle/gradle.properties
            // or -P on the command line), never from the repository.
            if (releaseSigningReady) {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
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
    buildFeatures {
        compose = true
    }
}

// A release APK or bundle needs the upload key. Fail early with a clear message instead of
// producing an unsigned release. Debug builds, unit tests and instrumented tests are not affected.
if (!releaseSigningReady) {
    tasks.matching { it.name == "packageRelease" || it.name == "signReleaseBundle" }.configureEach {
        doFirst {
            throw GradleException(
                "Release signing is not configured. Set PKCO_UPLOAD_STORE_FILE, " +
                    "PKCO_UPLOAD_STORE_PASSWORD, PKCO_UPLOAD_KEY_ALIAS and PKCO_UPLOAD_KEY_PASSWORD " +
                    "in your user-level ~/.gradle/gradle.properties (never in the repository) " +
                    "or pass them with -P."
            )
        }
    }
}

// Room writes the schema JSON here; it is committed so migrations can be checked.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
