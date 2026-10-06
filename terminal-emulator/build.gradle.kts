plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.termux.terminal"
    compileSdk = 37

    defaultConfig {
        minSdk = 34
    }
}
