plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")

    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val adjustAppToken = providers.gradleProperty("ADJUST_APP_TOKEN").orElse("").get()
val facebookAppId = providers.gradleProperty("FACEBOOK_APP_ID").orElse("").get()
val facebookClientToken = providers.gradleProperty("FACEBOOK_CLIENT_TOKEN").orElse("").get()

android {
    namespace = "com.example.lovecounter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lovecounter"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "ADJUST_APP_TOKEN", "\"$adjustAppToken\"")
        buildConfigField("String", "FACEBOOK_APP_ID", "\"$facebookAppId\"")
        buildConfigField("String", "FACEBOOK_CLIENT_TOKEN", "\"$facebookClientToken\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
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
        viewBinding = true
        buildConfig = true
        compose = false
    }

    lint {
        disable.addAll(
            hashSetOf(
                "ContentDescription",
                "HardcodedText",
                "SpUsage",
                "KotlinNullnessAnnotation",
                "NotifyDataSetChanged",
                "SetTextI18n",
                "DefaultLocale",
                "GradleDynamicVersion",
                "CustomSplashScreen",
                "ClickableViewAccessibility",
                "LockedOrientationActivity",
                "StaticFieldLeak"
            )
        )
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    flavorDimensions += "default"
    productFlavors {
        create("appDebug") {
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADJUST_ENVIRONMENT", "\"sandbox\"")
            buildConfigField("String", "inter_splash", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "native_splash", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "inter_main", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "appopen_resume", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "app_open", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "native_main", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_language", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_onboarding", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_onboarding_fullscreen", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_onboarding_fullscreen_2f", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "reward_unlock", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "banner_main", "\"ca-app-pub-3940256099942544/2014213617\"")
            buildConfigField("String", "native_test", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("Boolean", "build_debug", "true")
        }
        create("appProduct") {
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADJUST_ENVIRONMENT", "\"production\"")
            buildConfigField("String", "inter_splash", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "native_splash", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "inter_main", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "appopen_resume", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "app_open", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "native_main", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_language", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_onboarding", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_onboarding_fullscreen", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "native_onboarding_fullscreen_2f", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "reward_unlock", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "banner_main", "\"ca-app-pub-3940256099942544/2014213617\"")
            buildConfigField("String", "native_test", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("Boolean", "build_debug", "false")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.multidex:multidex:2.0.1")

    // Ads Module
    implementation(project(":ads"))

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")

    // ViewModel + Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // AdMob + UMP
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // UI & Utilities
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.karumi:dexter:6.2.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
