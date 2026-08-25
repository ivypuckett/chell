plugins {
    // Plugin versions are declared here with `apply false`; subprojects apply
    // them without a version.  :app needs no Kotlin plugin of its own — AGP 9
    // has built-in Kotlin support (https://kotl.in/gradle/agp-built-in-kotlin).
    id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
    id("com.android.application") version "9.3.2" apply false
}
