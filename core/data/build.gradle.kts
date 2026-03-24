plugins {
    alias(libs.plugins.convention.kmp.library)
}

dependencies {
    commonMainImplementation(projects.core.domain)
}
