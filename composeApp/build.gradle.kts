import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.convention.cmp.application)
}

kotlin {
    androidLibrary {
        namespace = "com.crazyenough.unknown"
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.withType<Framework>().configureEach {
            export(projects.feature.auth.domain)
            export(projects.feature.auth.data)
            export(projects.feature.auth.presentation)
        }
    }
}

dependencies {
    commonMainImplementation(platform(libs.koin.bom))
    commonMainImplementation(libs.koin.core)
    commonMainImplementation(libs.koin.compose.viewmodel)
    commonMainImplementation(projects.core.designsystem)
    commonMainImplementation(projects.core.navigation)
    commonMainApi(projects.feature.auth.domain)
    commonMainApi(projects.feature.auth.data)
    commonMainApi(projects.feature.auth.presentation)
    commonMainImplementation(libs.androidx.lifecycle.viewmodel.compose)
    commonMainImplementation(libs.androidx.lifecycle.runtime.compose)
    commonMainImplementation(libs.androidx.navigation3.ui)
    commonMainImplementation(libs.jetbrains.savedstate)
    commonMainImplementation(libs.kotlinx.serialization.json)
    commonTestImplementation(libs.kotlin.test)
}
