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
    commonMainImplementation(projects.core.domain)
    commonMainImplementation(projects.core.presentation)
    commonMainImplementation(projects.core.navigation)
    commonMainImplementation(projects.feature.auth.domain)
    commonMainImplementation(libs.touchlab.kermit)
    androidMainImplementation(libs.androidx.activity.ktx)
    androidMainImplementation(libs.androidx.credentials)
    androidMainImplementation(libs.androidx.credentials.play.services.auth)
    androidMainImplementation(libs.googleid)
    androidMainImplementation(libs.play.services.auth)
    androidMainImplementation(platform(libs.firebase.bom))
    androidMainImplementation(libs.firebase.auth)
    androidMainImplementation(libs.kakao.user)
}
