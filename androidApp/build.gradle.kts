plugins {
    alias(libs.plugins.convention.android.application.compose)
    alias(libs.plugins.convention.android.uitest)
    alias(libs.plugins.google.services)
}

dependencies {
    implementation(projects.composeApp)
    androidTestImplementation(projects.core.designsystem)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
}
