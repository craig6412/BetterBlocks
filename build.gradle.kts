// File: `build.gradle.kts`
plugins {
    id("com.android.application") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    // Add the Crashlytics Gradle plugin
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
    // Add the Play Services Ads plugin

}

configurations.all {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlin:kotlin-stdlib:1.9.0",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0",
            "org.jetbrains.kotlin:kotlin-stdlib-common:1.9.0",
        )
    }
}



tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}