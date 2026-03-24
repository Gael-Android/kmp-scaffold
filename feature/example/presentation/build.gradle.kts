plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    androidLibrary {
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }
}

dependencies {
    commonMainImplementation(projects.core.designsystem)
    commonMainImplementation(projects.core.navigation)
    commonMainImplementation(projects.core.presentation)
    commonMainImplementation(projects.feature.example.domain)
}
