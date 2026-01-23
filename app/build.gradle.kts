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
        versionCode = 16
        versionName = "2.1"
 // need to set to 16 ahead of time for new update coming in with non major issues being changed. as app is ad ready to run.
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-1555261975639574/3521751106\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-1555261975639574/7944945337\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-1555261975639574/4085591370\"")

            signingConfig = signingConfigs.getByName("debug")
        }

        debug {
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"  // REQUIRED for Kotlin 1.9.24
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging.resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Modern stable Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))

    // Compose UI libs (versions controlled by BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.material:material-icons-extended")


    // REMOVE old Kotlin stdlib pins
    // implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0") <-- DELETE
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0") <-- DELETE
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0") <-- DELETE

    // Billing & Ads
    implementation("com.android.billingclient:billing:7.0.0")
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.google.ads.mediation:unity:4.9.2.0")



    //new stuff after issues with the sizing on release day..
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")


    // Firebase (BOM resolves versions)
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-crashlytics")
    // Messaging (FCM) for topic broadcasts and receiving messages
    implementation("com.google.firebase:firebase-messaging-ktx")

    // XML-based UI dependencies
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
