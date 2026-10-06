plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.termux.view"
    compileSdk = 37

    defaultConfig {
        minSdk = 34
    }
}

dependencies {
    api(project(":terminal-emulator"))
    implementation(libs.androidx.core.ktx)
}
