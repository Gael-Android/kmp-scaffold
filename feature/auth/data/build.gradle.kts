plugins {
    alias(libs.plugins.convention.kmp.library)
}

dependencies {
    commonMainImplementation(projects.core.data)
    commonMainImplementation(projects.core.domain)
    commonMainImplementation(projects.feature.auth.domain)
    commonMainImplementation(libs.koin.core)
    androidMainImplementation(platform(libs.firebase.bom))
    androidMainImplementation(libs.firebase.auth)
    androidMainImplementation(libs.firebase.functions)
    commonTestImplementation(libs.kotlinx.coroutines.test)
}
