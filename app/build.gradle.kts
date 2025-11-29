plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.betterblocks"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.betterblocks"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-1555261975639574/3521751106\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-1555261975639574/7839435910\"")
        }
        debug {
            // DEBUG keeps defaults; BuildConfig.DEBUG stays true
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// Use the Kotlin JVM toolchain to ensure the Kotlin compiler targets Java 17
kotlin {
    jvmToolchain(17)
}

// Align all Google Play Services Measurement artifacts to one version to prevent duplicate classes
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.android.gms" && requested.name.startsWith("play-services-measurement")) {
            useVersion("21.6.2")
            because("Avoid duplicate classes by aligning play-services-measurement* artifacts to 21.6.2")
        }
    }
}

dependencies {
    // Use BOM to align Compose artifact versions
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("com.android.billingclient:billing:7.0.0") // Or latest version
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    // Material Icons (for MonetizationOn and others)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation.layout)


    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")


    //firebase sdk
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-auth")




// Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

// Firebase Analytics (optional but recommended)

// Firebase Auth (optional for identifying users)
    implementation("com.google.firebase:firebase-auth-ktx")


    // --- View-system libraries required by existing XML layouts ---
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // Removed constraints forcing 22.0.2 measurement modules; let Firebase BOM keep all measurement libs at 21.6.2 to avoid duplicate classes.
    // If upgrading later, update the Firebase BOM version instead of mixing measurement artifact versions.

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Optional: If you need compose testing later
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}