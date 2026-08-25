import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Version comes from the root build.  No Kotlin plugin here: AGP 9 provides
    // Kotlin support built in.
    id("com.android.application")
}

android {
    namespace = "dev.chell.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.chell.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

dependencies {
    implementation(project(":core"))
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
}

// Replaces the `kotlinOptions` block, which AGP 9 removed.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}
