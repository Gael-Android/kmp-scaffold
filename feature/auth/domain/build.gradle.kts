plugins {
    alias(libs.plugins.convention.kmp.library)
}

dependencies {
    commonMainImplementation(projects.core.domain)
    commonMainImplementation(libs.kotlinx.coroutines.core)
    commonTestImplementation(libs.kotlinx.coroutines.test)
}
