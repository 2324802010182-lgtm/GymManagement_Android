plugins {
    id("com.android.application")
}

// Chỉ bật Google Services khi người học đặt cấu hình Firebase thật vào app/.
// Điều này cho phép mở và chạy giao diện demo trước khi kết nối Firebase.
if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example.gymmanagement"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gymmanagement"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    }

    dependencies {
        implementation("com.github.bumptech.glide:glide:4.16.0")
        implementation("androidx.media3:media3-exoplayer:1.6.1")
        implementation("androidx.media3:media3-ui:1.6.1")
        implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
        implementation("androidx.media3:media3-ui:1.6.1")
        implementation("androidx.appcompat:appcompat:1.7.1")
        implementation("androidx.core:core:1.17.0")
        implementation("com.google.android.material:material:1.12.0")

        implementation(platform("com.google.firebase:firebase-bom:35.0.0"))
        implementation("com.google.firebase:firebase-auth")
        implementation("com.google.firebase:firebase-firestore")

        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.2.1")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    }
}