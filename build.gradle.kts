plugins {
    id("com.android.application") version "8.1.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

// REMOVE resolutionStrategy forcing 1.9.0 — it breaks Compose preview

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}