plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.rumahku.widget"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rumahku.widget"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
