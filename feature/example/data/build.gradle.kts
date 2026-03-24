plugins {
    alias(libs.plugins.convention.kmp.library)
}

dependencies {
    commonMainImplementation(projects.core.data)
    commonMainImplementation(projects.feature.example.domain)
}
