plugins {
    alias(libs.plugins.convention.cmp.library)
}

dependencies {
    commonMainImplementation(libs.androidx.lifecycle.viewmodel.navigation3)
    commonMainImplementation(libs.androidx.navigation3.ui)
    commonMainImplementation(libs.jetbrains.savedstate)
}
