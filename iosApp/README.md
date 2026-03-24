# iOS Auth Setup

`iosApp/Configuration/Config.xcconfig`와 `iosApp/iosApp/GoogleService-Info.plist`는 스캐폴드 플레이스홀더 상태입니다. 실제 Firebase 프로젝트와 bundle id를 연결하기 전에는 iOS Firebase 기반 인증이 동작하지 않습니다.

추가로 아래 값은 직접 채워야 iOS 소셜 로그인이 활성화됩니다.

- `GOOGLE_CLIENT_ID`
- `GOOGLE_SERVER_CLIENT_ID`
- `GOOGLE_REVERSED_CLIENT_ID`
- `KAKAO_NATIVE_APP_KEY`

추가 준비 항목:

- `iosApp/iosApp/GoogleService-Info.plist`를 Firebase Console에서 내려받은 실제 값으로 교체
- Apple Developer에서 `Sign in with Apple` capability 활성화
- Google OAuth iOS client와 reversed client ID가 `com.crazyenough.unknown`과 일치하는지 확인
- Kakao 개발자 콘솔에 `kakao{KAKAO_NATIVE_APP_KEY}://oauth` redirect scheme 등록

검증 시나리오:

- Google 로그인 성공, 취소, 실패
- Kakao 로그인 성공, 취소, 실패
- Apple 로그인 성공, 취소, 실패
- 로그인 성공 후 앱 인증 상태 반영 및 홈 이동
