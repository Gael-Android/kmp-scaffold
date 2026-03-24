import ComposeApp
import FirebaseCore
import GoogleSignIn
import KakaoSDKAuth
import KakaoSDKCommon
import SwiftUI
import UIKit

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        NSLog("[IosAppBootstrap][INFO] didFinishLaunching invoked")
        IosAppBootstrap.configure()
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        if GIDSignIn.sharedInstance.handle(url) {
            return true
        }

        if AuthApi.isKakaoTalkLoginUrl(url) {
            return AuthController.handleOpenUrl(url: url)
        }

        return false
    }
}

enum IosAppBootstrap {
    private static var isConfigured = false
    static private(set) var isFirebaseConfigured = false

    static func configure() {
        guard !isConfigured else { return }

        if let kakaoNativeAppKey = AuthEnvironment.kakaoNativeAppKey, !kakaoNativeAppKey.isEmpty {
            KakaoSDK.initSDK(appKey: kakaoNativeAppKey)
            NSLog("[IosAppBootstrap][INFO] Kakao SDK configured")
        } else {
            NSLog("[IosAppBootstrap][INFO] Kakao SDK skipped: KAKAO_NATIVE_APP_KEY is empty")
        }

        if let googleServiceInfoPath = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let options = FirebaseOptions(contentsOfFile: googleServiceInfoPath) {
            FirebaseApp.configure(options: options)
            isFirebaseConfigured = true
            NSLog("[IosAppBootstrap][INFO] Firebase configured from GoogleService-Info.plist")
        } else {
            NSLog("[IosAppBootstrap][ERROR] Firebase configuration failed: GoogleService-Info.plist missing or invalid")
        }

        IosAuthBridgeRegistrationKt.registerIosAuthBridges(
            providerBridge: SocialAuthCoordinator.shared,
            networkBridge: SocialAuthCoordinator.shared
        )
        NSLog("[IosAppBootstrap][INFO] iOS auth bridges registered")
        isConfigured = true
    }
}
