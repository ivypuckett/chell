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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
    // The domain tests under dev.chell.launcher.core use kotlin.test.  Named
    // explicitly because no Kotlin plugin is applied here to supply a version, and
    // the version must match the Kotlin compiler AGP 9 brings in itself.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")
}

// Replaces the `kotlinOptions` block, which AGP 9 removed.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}
