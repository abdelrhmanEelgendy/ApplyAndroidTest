// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.9.0")
    }
}
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.dokka") version "1.9.0"

}