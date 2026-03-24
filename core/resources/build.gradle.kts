import org.jetbrains.compose.resources.ResourcesExtension

plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    androidLibrary {
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "resources.generated.resources"
    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
}
