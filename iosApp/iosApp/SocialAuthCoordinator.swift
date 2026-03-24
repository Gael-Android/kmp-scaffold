import AuthenticationServices
import ComposeApp
import CryptoKit
import FirebaseAuth
import FirebaseCore
import FirebaseFunctions
import GoogleSignIn
import KakaoSDKAuth
import KakaoSDKCommon
import KakaoSDKUser
import UIKit

final class SocialAuthCoordinator: NSObject,
    IosAuthProviderBridge,
    IosAuthNetworkBridge,
    ASAuthorizationControllerDelegate,
    ASAuthorizationControllerPresentationContextProviding {

    static let shared = SocialAuthCoordinator()

    private let functionsRegion = "asia-northeast3"
    private var pendingAppleCompletion: ((IosAuthProviderLaunchPayload?, Error?) -> Void)?
    private var currentAppleNonce: String?
    private var currentApplePresentationAnchor: ASPresentationAnchor?

    func supportedProviderNames() -> [String] {
        ["GOOGLE", "KAKAO", "APPLE"]
    }

    func enabledProviderNames() -> [String] {
        var providers: [String] = []

        if AuthEnvironment.isGoogleReady {
            providers.append("GOOGLE")
        }
        if AuthEnvironment.isKakaoReady {
            providers.append("KAKAO")
        }
        if AuthEnvironment.isFirebaseReady {
            providers.append("APPLE")
        }

        return providers
    }

    func hasFirebaseCurrentUser() -> Bool {
        Auth.auth().currentUser != nil
    }

    func hasKakaoToken() -> Bool {
        guard AuthEnvironment.isKakaoReady else { return false }
        return AuthApi.hasToken()
    }

    func restoreFirebaseCredential(
        completionHandler: @escaping (IosAuthProviderLaunchPayload?, Error?) -> Void
    ) {
        guard let providerName = restorableFirebaseProviderName() else {
            completionHandler(payload(providerName: ""), nil)
            return
        }

        fetchFirebaseIdToken { token, _ in
            if let token, !token.isEmpty {
                self.logInfo("restoreFirebaseCredential succeeded provider=\(providerName)")
                completionHandler(self.payload(providerName: providerName, token: token), nil)
                return
            }

            completionHandler(self.payload(providerName: ""), nil)
        }
    }

    func restoreKakaoCredential(
        completionHandler: @escaping (IosAuthProviderLaunchPayload?, Error?) -> Void
    ) {
        resolveKakaoAccessToken { kakaoAccessToken in
            if let kakaoAccessToken, !kakaoAccessToken.isEmpty {
                self.logInfo("restoreKakaoCredential succeeded")
                completionHandler(self.payload(providerName: "KAKAO", token: kakaoAccessToken), nil)
                return
            }

            completionHandler(self.payload(providerName: ""), nil)
        }
    }

    func launch(
        providerName: String,
        completionHandler: @escaping (IosAuthProviderLaunchPayload?, Error?) -> Void
    ) {
        logInfo("launch requested provider=\(providerName.uppercased())")
        switch providerName.uppercased() {
        case "GOOGLE":
            launchGoogle(completionHandler: completionHandler)
        case "KAKAO":
            launchKakao(completionHandler: completionHandler)
        case "APPLE":
            launchApple(completionHandler: completionHandler)
        default:
            logError("launch rejected: unsupported provider=\(providerName.uppercased())")
            completionHandler(payload(providerName: providerName, errorCode: "UNAVAILABLE"), nil)
        }
    }

    func signOut(providerName: String, completionHandler: @escaping (Error?) -> Void) {
        switch providerName.uppercased() {
        case "GOOGLE":
            GIDSignIn.sharedInstance.signOut()
            signOutFirebase()
            completionHandler(nil)

        case "KAKAO":
            UserApi.shared.logout { error in
                self.signOutFirebase()
                completionHandler(error)
            }

        case "APPLE":
            signOutFirebase()
            completionHandler(nil)

        default:
            completionHandler(nil)
        }
    }

    func unlink(providerName: String, completionHandler: @escaping (Error?) -> Void) {
        switch providerName.uppercased() {
        case "GOOGLE":
            GIDSignIn.sharedInstance.disconnect { error in
                self.deleteCurrentFirebaseUser(completionHandler: completionHandler, fallbackError: error)
            }

        case "KAKAO":
            UserApi.shared.unlink { error in
                self.deleteCurrentFirebaseUser(completionHandler: completionHandler, fallbackError: error)
            }

        case "APPLE":
            deleteCurrentFirebaseUser(completionHandler: completionHandler, fallbackError: nil)

        default:
            completionHandler(nil)
        }
    }

    func currentFirebaseSession(
        providerName: String,
        preferredToken: String,
        userIdOverride: String?,
        displayNameOverride: String?,
        isNewUserOverride: KotlinBoolean?,
        completionHandler: @escaping (IosFirebaseSessionPayload?, Error?) -> Void
    ) {
        currentFirebaseSession(
            providerName: providerName,
            preferredToken: preferredToken.isEmpty ? nil : preferredToken,
            isNewUserOverride: isNewUserOverride?.boolValue,
            userIdOverride: userIdOverride,
            displayNameOverride: displayNameOverride,
            completionHandler: completionHandler
        )
    }

    func signInWithCustomToken(
        customToken: String,
        completionHandler: @escaping (IosCustomTokenSignInPayload?, Error?) -> Void
    ) {
        guard AuthEnvironment.isFirebaseReady else {
            logError("signInWithCustomToken blocked: Firebase is not configured")
            completionHandler(customTokenSignInPayload(errorCode: "CONFIGURATION"), nil)
            return
        }

        Auth.auth().signIn(withCustomToken: customToken) { authResult, error in
            if let error {
                self.logError("Firebase customToken signIn failed", error: error)
                completionHandler(self.customTokenSignInPayload(errorCode: self.firebaseErrorCode(error)), nil)
                return
            }

            completionHandler(
                IosCustomTokenSignInPayload(
                    isNewUser: self.kotlinBoolean(authResult?.additionalUserInfo?.isNewUser),
                    errorCode: nil
                ),
                nil
            )
        }
    }

    func exchangeSocialToken(
        providerName: String,
        token: String,
        completionHandler: @escaping (IosSocialTokenExchangePayload?, Error?) -> Void
    ) {
        logInfo("exchangeSocialToken requested provider=\(providerName.uppercased()) tokenEmpty=\(token.isEmpty)")
        switch providerName.uppercased() {
        case "KAKAO":
            exchangeKakaoToken(token: token, completionHandler: completionHandler)

        default:
            completionHandler(socialTokenExchangePayload(errorCode: "INVALID_CREDENTIAL"), nil)
        }
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        if let currentApplePresentationAnchor {
            return currentApplePresentationAnchor
        }

        if let fallbackAnchor = topViewController()?.view.window {
            logInfo("presentationAnchor fallback window resolved")
            return fallbackAnchor
        }

        logError("presentationAnchor unavailable; returning empty anchor")
        return ASPresentationAnchor()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        logInfo("Apple authorization completed")
        guard let appleCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            logError("Apple authorization returned unexpected credential type")
            pendingAppleCompletion?(payload(providerName: "APPLE", errorCode: "INVALID_CREDENTIAL"), nil)
            resetAppleState()
            return
        }
        guard
            let nonce = currentAppleNonce,
            let identityTokenData = appleCredential.identityToken,
            let identityToken = String(data: identityTokenData, encoding: .utf8),
            !identityToken.isEmpty
        else {
            logError("Apple credential missing nonce or identityToken")
            pendingAppleCompletion?(payload(providerName: "APPLE", errorCode: "INVALID_CREDENTIAL"), nil)
            resetAppleState()
            return
        }

        let credential = OAuthProvider.appleCredential(
            withIDToken: identityToken,
            rawNonce: nonce,
            fullName: appleCredential.fullName
        )

        Auth.auth().signIn(with: credential) { [weak self] _, error in
            guard let self else { return }
            if let error {
                self.logError("Firebase Apple signIn failed", error: error)
                self.pendingAppleCompletion?(self.payload(providerName: "APPLE", errorCode: self.firebaseErrorCode(error)), nil)
                self.resetAppleState()
                return
            }

            self.fetchFirebaseIdToken { token, errorCode in
                self.logInfo("Firebase Apple signIn succeeded tokenEmpty=\((token ?? "").isEmpty) errorCode=\(errorCode ?? "nil")")
                self.pendingAppleCompletion?(self.payload(providerName: "APPLE", token: token ?? "", errorCode: errorCode), nil)
                self.resetAppleState()
            }
        }
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        logError("Apple authorization failed", error: error)
        pendingAppleCompletion?(payload(providerName: "APPLE", errorCode: appleErrorCode(error)), nil)
        resetAppleState()
    }
}

private extension SocialAuthCoordinator {
    func launchGoogle(
        completionHandler: @escaping (IosAuthProviderLaunchPayload?, Error?) -> Void
    ) {
        guard AuthEnvironment.isGoogleReady else {
            logError("launchGoogle blocked: configuration is incomplete")
            completionHandler(payload(providerName: "GOOGLE", errorCode: "CONFIGURATION"), nil)
            return
        }
        guard let presentingViewController = topViewController() else {
            logError("launchGoogle blocked: no presenting view controller")
            completionHandler(payload(providerName: "GOOGLE", errorCode: "UNAVAILABLE"), nil)
            return
        }
        guard let clientID = AuthEnvironment.googleClientId else {
            logError("launchGoogle blocked: missing Google client ID")
            completionHandler(payload(providerName: "GOOGLE", errorCode: "CONFIGURATION"), nil)
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientID,
            serverClientID: AuthEnvironment.googleServerClientId
        )
        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { result, error in
            if let error {
                self.logError("Google signIn failed", error: error)
                completionHandler(self.payload(providerName: "GOOGLE", errorCode: self.googleErrorCode(error)), nil)
                return
            }

            guard
                let user = result?.user,
                let idToken = user.idToken?.tokenString,
                !idToken.isEmpty
            else {
                self.logError("Google signIn returned empty idToken")
                completionHandler(self.payload(providerName: "GOOGLE", errorCode: "INVALID_CREDENTIAL"), nil)
                return
            }

            let credential = GoogleAuthProvider.credential(
                withIDToken: idToken,
                accessToken: user.accessToken.tokenString
            )

            Auth.auth().signIn(with: credential) { _, error in
                if let error {
                    self.logError("Firebase Google signIn failed", error: error)
                    completionHandler(self.payload(providerName: "GOOGLE", errorCode: self.firebaseErrorCode(error)), nil)
                    return
                }

                self.fetchFirebaseIdToken { token, errorCode in
                    self.logInfo("Firebase Google signIn succeeded tokenEmpty=\((token ?? "").isEmpty) errorCode=\(errorCode ?? "nil")")
                    completionHandler(self.payload(providerName: "GOOGLE", token: token ?? "", errorCode: errorCode), nil)
                }
            }
        }
    }

    func launchKakao(
        completionHandler: @escaping (IosAuthProviderLaunchPayload?, Error?) -> Void
    ) {
        guard AuthEnvironment.isKakaoReady else {
            logError("launchKakao blocked: configuration is incomplete")
            completionHandler(payload(providerName: "KAKAO", errorCode: "CONFIGURATION"), nil)
            return
        }

        let callback: (OAuthToken?, Error?) -> Void = { token, error in
            if let error {
                self.logError("Kakao signIn failed", error: error)
                completionHandler(self.payload(providerName: "KAKAO", errorCode: self.kakaoErrorCode(error)), nil)
                return
            }

            guard let accessToken = token?.accessToken, !accessToken.isEmpty else {
                self.logError("Kakao signIn returned empty accessToken")
                completionHandler(self.payload(providerName: "KAKAO", errorCode: "INVALID_CREDENTIAL"), nil)
                return
            }

            self.logInfo("Kakao signIn succeeded tokenEmpty=false")
            self.ensureKakaoEmailConsentIfNeeded(accessToken: accessToken) { resolvedAccessToken in
                completionHandler(self.payload(providerName: "KAKAO", token: resolvedAccessToken), nil)
            }
        }

        if UserApi.isKakaoTalkLoginAvailable() {
            UserApi.shared.loginWithKakaoTalk(completion: callback)
        } else {
            UserApi.shared.loginWithKakaoAccount(completion: callback)
        }
    }

    func ensureKakaoEmailConsentIfNeeded(
        accessToken: String,
        completion: @escaping (String) -> Void
    ) {
        UserApi.shared.me { user, error in
            if let error {
                self.logError("Kakao user lookup failed while checking email consent", error: error)
                completion(accessToken)
                return
            }

            self.logInfo(
                "Kakao user info: id=\(user?.id ?? 0) email=\(user?.kakaoAccount?.email ?? "") emailNeedsAgreement=\(String(describing: user?.kakaoAccount?.emailNeedsAgreement))"
            )

            guard user?.kakaoAccount?.emailNeedsAgreement == true else {
                completion(accessToken)
                return
            }

            self.logInfo("Requesting Kakao account_email additional consent")
            UserApi.shared.loginWithKakaoAccount(scopes: ["account_email"]) { token, error in
                if let error {
                    if let sdkError = error as? SdkError,
                       sdkError.isClientFailed,
                       sdkError.getClientError().reason == .Cancelled {
                        self.logInfo("Kakao account_email consent cancelled; proceeding with existing token")
                    } else {
                        self.logError("Kakao account_email consent failed; proceeding with existing token", error: error)
                    }
                    completion(accessToken)
                    return
                }

                let refreshedAccessToken = token?.accessToken ?? ""
                if refreshedAccessToken.isEmpty {
                    self.logInfo("Kakao account_email consent completed without refreshed token; proceeding with existing token")
                    completion(accessToken)
                    return
                }

                self.logInfo("Kakao account_email consent completed; proceeding with refreshed token")
                completion(refreshedAccessToken)
            }
        }
    }

    func launchApple(
        completionHandler: @escaping (IosAuthProviderLaunchPayload?, Error?) -> Void
    ) {
        guard AuthEnvironment.isFirebaseReady else {
            logError("launchApple blocked: Firebase is not configured")
            completionHandler(payload(providerName: "APPLE", errorCode: "CONFIGURATION"), nil)
            return
        }
        guard let presentationAnchor = topViewController()?.view.window else {
            logError("launchApple blocked: no presentation anchor/window")
            completionHandler(payload(providerName: "APPLE", errorCode: "UNAVAILABLE"), nil)
            return
        }

        pendingAppleCompletion = completionHandler
        let nonce = randomNonce()
        currentAppleNonce = nonce
        currentApplePresentationAnchor = presentationAnchor
        logInfo("launchApple started windowAvailable=true nonceLength=\(nonce.count)")

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func exchangeKakaoToken(
        token: String,
        completionHandler: @escaping (IosSocialTokenExchangePayload?, Error?) -> Void
    ) {
        guard AuthEnvironment.isFirebaseReady else {
            logError("exchangeKakaoToken blocked: Firebase is not configured")
            completionHandler(socialTokenExchangePayload(errorCode: "CONFIGURATION"), nil)
            return
        }

        Functions.functions(region: functionsRegion)
            .httpsCallable("exchangeSocialToken")
            .call([
                "provider": "KAKAO",
                "token": token,
            ]) { result, error in
                if let error {
                    self.logError("exchangeSocialToken Cloud Function failed", error: error)
                    completionHandler(self.socialTokenExchangePayload(errorCode: self.functionsErrorCode(error)), nil)
                    return
                }

                guard
                    let data = result?.data as? [String: Any],
                    let customToken = data["customToken"] as? String,
                    !customToken.isEmpty
                else {
                    self.logError("exchangeSocialToken Cloud Function returned invalid payload")
                    completionHandler(self.socialTokenExchangePayload(errorCode: "INVALID_CREDENTIAL"), nil)
                    return
                }

                completionHandler(
                    IosSocialTokenExchangePayload(
                        customToken: customToken,
                        userId: data["userId"] as? String ?? "",
                        displayName: data["displayName"] as? String ?? "",
                        isNewUser: self.kotlinBoolean(self.boolValue(data["isNewUser"])),
                        errorCode: nil
                    ),
                    nil
                )
            }
    }

    func currentFirebaseSession(
        providerName: String,
        preferredToken: String?,
        isNewUserOverride: Bool? = nil,
        userIdOverride: String? = nil,
        displayNameOverride: String? = nil,
        completionHandler: @escaping (IosFirebaseSessionPayload?, Error?) -> Void
    ) {
        guard let user = Auth.auth().currentUser else {
            logError("currentFirebaseSession failed: currentUser is nil for provider=\(providerName)")
            completionHandler(firebaseSessionPayload(errorCode: "UNAUTHORIZED"), nil)
            return
        }

        let finish: (String) -> Void = { accessToken in
            completionHandler(
                IosFirebaseSessionPayload(
                    accessToken: accessToken,
                    refreshToken: "",
                    userId: userIdOverride ?? user.uid,
                    displayName: displayNameOverride ?? user.displayName ?? user.email ?? "",
                    providerName: providerName,
                    isNewUser: isNewUserOverride ?? false,
                    errorCode: nil
                ),
                nil
            )
        }

        if let preferredToken, !preferredToken.isEmpty {
            finish(preferredToken)
            return
        }

        user.getIDTokenResult(forcingRefresh: true) { result, error in
            if let error {
                self.logError("getIDTokenResult failed for provider=\(providerName)", error: error)
                completionHandler(self.firebaseSessionPayload(errorCode: self.firebaseErrorCode(error)), nil)
                return
            }

            guard let token = result?.token, !token.isEmpty else {
                self.logError("getIDTokenResult returned empty token for provider=\(providerName)")
                completionHandler(self.firebaseSessionPayload(errorCode: "INVALID_CREDENTIAL"), nil)
                return
            }

            finish(token)
        }
    }

    func fetchFirebaseIdToken(completion: @escaping (String?, String?) -> Void) {
        guard let user = Auth.auth().currentUser else {
            logError("fetchFirebaseIdToken failed: currentUser is nil")
            completion(nil, "UNAUTHORIZED")
            return
        }

        user.getIDTokenResult(forcingRefresh: true) { result, error in
            if let error {
                self.logError("fetchFirebaseIdToken getIDTokenResult failed", error: error)
                completion(nil, self.firebaseErrorCode(error))
                return
            }

            guard let token = result?.token, !token.isEmpty else {
                self.logError("fetchFirebaseIdToken returned empty token")
                completion(nil, "INVALID_CREDENTIAL")
                return
            }

            completion(token, nil)
        }
    }

    func restorableFirebaseProviderName() -> String? {
        guard let user = Auth.auth().currentUser else { return nil }

        let providerIds = user.providerData.map(\.providerID)
        if providerIds.contains("google.com") {
            return "GOOGLE"
        }
        if providerIds.contains("apple.com") {
            return "APPLE"
        }

        return nil
    }

    func currentKakaoAccessToken() -> String? {
        guard AuthEnvironment.isKakaoReady else { return nil }
        return TokenManager.manager.getToken()?.accessToken
    }

    func resolveKakaoAccessToken(completion: @escaping (String?) -> Void) {
        guard AuthEnvironment.isKakaoReady, AuthApi.hasToken() else {
            completion(nil)
            return
        }

        UserApi.shared.accessTokenInfo { _, error in
            if let error {
                self.logError("Kakao auto login token validation failed", error: error)
                completion(nil)
                return
            }

            completion(self.currentKakaoAccessToken())
        }
    }

    func signOutFirebase() {
        do {
            try Auth.auth().signOut()
            logInfo("Firebase signOut completed")
        } catch {
            logError("Firebase signOut failed", error: error)
        }
    }

    func deleteCurrentFirebaseUser(
        completionHandler: @escaping (Error?) -> Void,
        fallbackError: Error?
    ) {
        guard let currentUser = Auth.auth().currentUser else {
            if let fallbackError {
                logError("deleteCurrentFirebaseUser fallback: currentUser is nil", error: fallbackError)
            } else {
                logError("deleteCurrentFirebaseUser skipped: currentUser is nil")
            }
            completionHandler(fallbackError)
            return
        }

        currentUser.delete { error in
            if let error {
                self.logError("Firebase user deletion failed", error: error)
            } else if let fallbackError {
                self.logError("Firebase user deletion finished with fallback error", error: fallbackError)
            } else {
                self.logInfo("Firebase user deletion completed")
            }
            completionHandler(error ?? fallbackError)
        }
    }

    func topViewController(base: UIViewController? = nil) -> UIViewController? {
        let root = base ?? UIApplication.shared
            .connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController

        if let navigationController = root as? UINavigationController {
            return topViewController(base: navigationController.visibleViewController)
        }

        if let tabBarController = root as? UITabBarController,
           let selectedViewController = tabBarController.selectedViewController {
            return topViewController(base: selectedViewController)
        }

        if let presentedViewController = root?.presentedViewController {
            return topViewController(base: presentedViewController)
        }

        return root
    }

    func payload(
        providerName: String,
        token: String = "",
        errorCode: String? = nil
    ) -> IosAuthProviderLaunchPayload {
        IosAuthProviderLaunchPayload(
            providerName: providerName,
            token: token,
            errorCode: errorCode
        )
    }

    func firebaseSessionPayload(errorCode: String) -> IosFirebaseSessionPayload {
        IosFirebaseSessionPayload(
            accessToken: "",
            refreshToken: "",
            userId: "",
            displayName: "",
            providerName: "",
            isNewUser: false,
            errorCode: errorCode
        )
    }

    func customTokenSignInPayload(errorCode: String) -> IosCustomTokenSignInPayload {
        IosCustomTokenSignInPayload(
            isNewUser: nil,
            errorCode: errorCode
        )
    }

    func socialTokenExchangePayload(errorCode: String) -> IosSocialTokenExchangePayload {
        IosSocialTokenExchangePayload(
            customToken: "",
            userId: "",
            displayName: "",
            isNewUser: nil,
            errorCode: errorCode
        )
    }

    func googleErrorCode(_ error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == kGIDSignInErrorDomain,
           nsError.code == GIDSignInError.canceled.rawValue {
            return "CANCELLED"
        }

        return "UNKNOWN"
    }

    func kakaoErrorCode(_ error: Error) -> String {
        if let sdkError = error as? SdkError, sdkError.isInvalidTokenError() {
            return "INVALID_CREDENTIAL"
        }
        if let sdkError = error as? SdkError,
           sdkError.isClientFailed,
           sdkError.getClientError().reason == .Cancelled {
            return "CANCELLED"
        }

        return "UNKNOWN"
    }

    func appleErrorCode(_ error: Error) -> String {
        if let authorizationError = error as? ASAuthorizationError {
            switch authorizationError.code {
            case .canceled:
                return "CANCELLED"
            case .invalidResponse:
                return "INVALID_CREDENTIAL"
            default:
                return "UNKNOWN"
            }
        }

        return "UNKNOWN"
    }

    func firebaseErrorCode(_ error: Error) -> String {
        let nsError = error as NSError
        guard let code = AuthErrorCode(rawValue: nsError.code) else {
            return "UNKNOWN"
        }

        switch code {
        case .networkError, .tooManyRequests:
            return "NETWORK"
        case .operationNotAllowed:
            return "CONFIGURATION"
        case .invalidCredential, .missingOrInvalidNonce, .credentialAlreadyInUse:
            return "INVALID_CREDENTIAL"
        case .userDisabled, .userTokenExpired, .invalidUserToken, .requiresRecentLogin:
            return "UNAUTHORIZED"
        default:
            return "UNKNOWN"
        }
    }

    func functionsErrorCode(_ error: Error) -> String {
        let nsError = error as NSError
        guard let code = FunctionsErrorCode(rawValue: nsError.code) else {
            return "UNKNOWN"
        }

        switch code {
        case .deadlineExceeded, .unavailable:
            return "NETWORK"
        case .unauthenticated, .permissionDenied:
            return "UNAUTHORIZED"
        case .invalidArgument, .notFound, .alreadyExists, .failedPrecondition:
            return "INVALID_CREDENTIAL"
        default:
            return "UNKNOWN"
        }
    }

    func boolValue(_ value: Any?) -> Bool? {
        value as? Bool
    }

    func kotlinBoolean(_ value: Bool?) -> KotlinBoolean? {
        guard let value else { return nil }
        return KotlinBoolean(bool: value)
    }

    func resetAppleState() {
        pendingAppleCompletion = nil
        currentAppleNonce = nil
        currentApplePresentationAnchor = nil
    }

    func randomNonce(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remainingLength = length

        while remainingLength > 0 {
            let randoms: [UInt8] = (0 ..< 16).map { _ in
                var random: UInt8 = 0
                let errorCode = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
                if errorCode != errSecSuccess {
                    fatalError("Unable to generate nonce. SecRandomCopyBytes failed with OSStatus \(errorCode)")
                }
                return random
            }

            randoms.forEach { random in
                if remainingLength == 0 {
                    return
                }

                if random < charset.count {
                    result.append(charset[Int(random)])
                    remainingLength -= 1
                }
            }
        }

        return result
    }

    func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        return hashedData.map { String(format: "%02x", $0) }.joined()
    }

    func logInfo(_ message: String) {
        NSLog("[SocialAuth][INFO] %@", message)
    }

    func logError(_ message: String, error: Error? = nil) {
        guard let error else {
            NSLog("[SocialAuth][ERROR] %@", message)
            return
        }

        let nsError = error as NSError
        NSLog(
            "[SocialAuth][ERROR] %@ | domain=%@ code=%ld description=%@",
            message,
            nsError.domain,
            nsError.code,
            nsError.localizedDescription
        )
    }
}

enum AuthEnvironment {
    static var googleClientId: String? {
        bundleValue("GIDClientID")
    }

    static var googleServerClientId: String? {
        bundleValue("GIDServerClientID")
    }

    static var kakaoNativeAppKey: String? {
        bundleValue("KAKAO_NATIVE_APP_KEY")
    }

    static var isFirebaseReady: Bool {
        IosAppBootstrap.isFirebaseConfigured
    }

    static var isGoogleReady: Bool {
        isFirebaseReady &&
            !(googleClientId?.isEmpty ?? true) &&
            !(googleServerClientId?.isEmpty ?? true)
    }

    static var isKakaoReady: Bool {
        !(kakaoNativeAppKey?.isEmpty ?? true)
    }

    private static func bundleValue(_ key: String) -> String? {
        Bundle.main.object(forInfoDictionaryKey: key) as? String
    }
}
