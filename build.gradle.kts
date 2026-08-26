plugins {
    // The plugin version is declared here with `apply false`; :app applies it
    // without a version.  It needs no Kotlin plugin of its own — AGP 9 has
    // built-in Kotlin support (https://kotl.in/gradle/agp-built-in-kotlin).
    id("com.android.application") version "9.3.2" apply false
}
