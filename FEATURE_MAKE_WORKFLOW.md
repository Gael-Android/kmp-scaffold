# Feature Make Workflow

> MCP 디자인 소스를 입력받아 → UI 구현 + 테스트 생성 → DI/Navigation 연결 → 테스트 통과 → 커밋/푸시까지 자동화하는 Orchestrator 기반 워크플로우.

---

## 전체 흐름

```
[MCP 입력 감지]
       │
       ├─ Figma MCP ──┐
       └─ Stitch MCP ─┤
                      │
          ┌───────────┴───────────┐  (병렬 실행)
          │                       │
    [에이전트 A]             [에이전트 B]
    implementer              tester
    UI 구현 스킬             테스트 생성 스킬
    (MVI 4-file)           (Android Instrumentation)
          │                       │
          └───────────┬───────────┘
                      │ (둘 다 완료 후)
                      ▼
             [에이전트 C]
             implementer
         DI + Navigation 연결
         (Koin 모듈 등록, Navigation3 엔트리)
                      │
                      ▼
             [에이전트 D]
             tester → reviewer → implementer
         테스트 실행 → 실패 시 코드 수정 반복
         (모든 테스트 통과할 때까지)
                      │
                      ▼
             [Orchestrator]
             git-review-commit-push
```

---

## 역할 정의

| 역할 | Agent (`~/.codex/agents/`) | 책임 |
|------|---------------------------|------|
| **Orchestrator** | `orchestrator` | 전체 흐름 관리, MCP 소스 판별, 에이전트 실행 조율 |
| **에이전트 A** | `implementer` | MVI 4-file 구조 UI 구현 |
| **에이전트 B** | `tester` | Android Instrumentation + Compose 공통 테스트 생성 |
| **에이전트 C** | `implementer` | Koin DI 모듈 등록 + Navigation3 엔트리 연결 |
| **에이전트 D** | `tester` → `reviewer` → `implementer` | 테스트 실행, 실패 분석, 코드 수정 루프 |

### Agent 상세 (`~/.codex/agents/`)

| Agent 파일 | 모델 | 역할 특성 |
|-----------|------|-----------|
| `orchestrator.toml` | `gpt-5.3-codex` (reasoning: high) | 파이프라인 분해·조율, 직접 코드 작성 최소화 |
| `implementer.toml` | `gpt-5.3-codex` (reasoning: medium) | 프로덕션 코드 작성, 기존 아키텍처 준수, 최소 diff |
| `tester.toml` | `gpt-5.3-codex` (reasoning: medium) | 테스트 생성·실행, edge case 탐지, 커버리지 최소화 |
| `reviewer.toml` | `gpt-5.3-codex` (reasoning: high) | 보안·경쟁조건·아키텍처 위반·잘못된 로직 검토 |
| `explorer.toml` | `gpt-5.3-codex-spark` (reasoning: medium, read-only) | 코드베이스 탐색, 의존성 추적, 엔트리포인트 파악 |

---

## 단계별 상세

### Step 0 — MCP 소스 감지 (Orchestrator)

**Agent:** `orchestrator`

Orchestrator는 주입된 외부 MCP 데이터를 판별한다.

| 조건 | 판별 방법 |
|------|-----------|
| **Figma MCP** | `mcp__figma-local__*` 도구 호출 결과가 있거나, 사용자가 Figma URL/node-id를 제공 |
| **Stitch MCP** | Stitch 스크린 JSON 또는 `mcp__stitch__get_screen` 출력 데이터가 제공됨 |

> MCP 소스가 명확하지 않으면 `explorer` 에이전트로 기존 화면 컨벤션을 파악한 뒤 사용자에게 확인 요청.

---

### Step 1 — UI 구현 + 테스트 생성 (병렬)

MCP 소스 판별 완료 즉시 **에이전트 A와 에이전트 B를 동시에 실행**한다.

---

#### 에이전트 A — UI 구현

**Agent:** `implementer`

| MCP 소스 | 사용 Skill (`~/.codex/skills/`) |
|----------|---------------------------------|
| Figma | `figma-mcp-cmp-mvi` |
| Stitch | `stitch-mcp-cmp-mvi` |

**Skill 동작 요약:**

| Skill | 트리거 조건 | 핵심 동작 |
|-------|------------|-----------|
| `figma-mcp-cmp-mvi` | Figma URL/node-id 또는 `get_design_context` 결과 | `get_design_context` → 섹션/텍스트/액션 파악 → MVI 4-file 생성 |
| `stitch-mcp-cmp-mvi` | Stitch URL 또는 `get_project`/`get_screen` 결과 | HTML/스크린샷 다운로드 → 구조 파싱 → MVI 4-file 생성 |

**두 스킬 공통 선행 조건:**
- `core/designsystem` 기존 토큰/컴포넌트를 먼저 확인하고 재사용
- 재사용 불가 시 `core/designsystem`에 먼저 추가한 뒤 사용

**산출물 (MVI 4-file 구조):**

```
feature/<name>/presentation/
  └── <Screen>Contract.kt   # State / Action / Event
  └── <Screen>ViewModel.kt  # 비즈니스 로직, StateFlow, Channel
  └── <Screen>Route.kt      # 상태 수집, 이벤트 처리, 네비게이션 콜백
  └── <Screen>Screen.kt     # Stateless UI, Preview 포함
```

**준수 규칙:**
- 단방향 데이터 흐름: `Action → ViewModel → State/Event → UI`
- `_stateFlow.update { it.copy(...) }` 사용 (thread-safe)
- `Channel<Event>(Channel.BUFFERED)` + `receiveAsFlow()` 로 일회성 이벤트 처리
- Screen은 상태를 직접 소유하지 않음 (Stateless)
- `@Preview` + `PreviewParameterProvider` 필수

---

#### 에이전트 B — 테스트 생성

**Agent:** `tester`

| MCP 소스 | 사용 Skill (`~/.codex/skills/`) |
|----------|---------------------------------|
| Figma | `figma-to-androidtest` |
| Stitch | `stitch-to-androidtest` |

**Skill 동작 요약:**

| Skill | 핵심 동작 |
|-------|-----------|
| `figma-to-androidtest` | Figma 노드 구조 파싱 → 인터랙션 추론 → commonTest + androidTest 생성 |
| `stitch-to-androidtest` | Stitch HTML/스크린샷 파싱 → 인터랙션 추론 → commonTest + androidTest 생성 |

**산출물:**

```
feature/<name>/presentation/src/commonMain/.../TestTags.kt  # testTag 상수 (공통 참조용)
feature/<name>/presentation/src/commonTest/.../<Screen>ScreenTest.kt  # Compose 단위 테스트
androidApp/src/androidTest/.../<Screen>InstrumentedTest.kt             # Android 계측 테스트
```

**스코프 분리 기준:**

| 스코프 | 대상 |
|--------|------|
| `commonTest` | 단일 화면 내 상태/입력/버튼 활성화 검증 |
| `androidTest` | 화면 전환, Navigation 상태, Activity 라이프사이클 |

**준수 규칙:**
- 각 `@Test` 마다 한국어 주석 필수 (`// [Given/When/Then] ...`)
- testTag 문자열은 테스트 파일에 하드코딩 금지 → `TestTags.kt` 상수 참조
- `onNodeWithTag(...)` 기반 시맨틱 접근
- `commonTest`에서 `createAndroidComposeRule` 사용 금지 (반대도 동일)

---

### Step 2 — DI + Navigation 연결 (에이전트 C)

에이전트 A와 B가 **모두 완료된 후** 실행한다.

**Agent:** `implementer`
**사전 탐색:** `explorer` (기존 KoinInit.kt, NavDisplay 엔트리 구조 파악)

---

#### 2-1. Koin 모듈 등록

참고 규칙: `~/.claude/rules/koin-migration.md`

```
feature/<name>/presentation/src/commonMain/.../di/
  └── <Name>PresentationModule.kt
```

```kotlin
// 예시
val <name>PresentationModule: Module = module {
    viewModelOf(::<Name>ViewModel)
}
```

`composeApp/src/commonMain/.../KoinInit.kt` 의 `appKoinModules` 에 추가:

```kotlin
val appKoinModules: List<Module> = listOf(
    ...,
    <name>PresentationModule,  // 추가
)
```

**체크리스트:**
- [ ] `di/` 패키지 아래에 `<Name>PresentationModule.kt` 생성
- [ ] `viewModelOf(::XxxViewModel)` 으로 ViewModel 등록
- [ ] `appKoinModules` 리스트에 모듈 추가
- [ ] `viewModel { ... }` 직접 인스턴스 생성 코드가 없는지 확인

---

#### 2-2. Navigation3 엔트리 등록

참고 규칙: `~/.claude/rules/navigation3-architecture.md`

**라우트 키 정의 (`feature/<name>/presentation`):**

```kotlin
// 인자 없는 화면
data object <Name>Route : NavKey

// 인자 있는 화면
data class <Name>Route(val id: String) : NavKey
```

**NavDisplay 엔트리 등록 (reducer-driven 방식, 이 프로젝트 기본):**

```kotlin
is <Name>Route -> NavEntry(key) {
    <Name>Route(
        viewModel = koinViewModel(),
        onNavigateBack = { handleAction(NavigationAction.BackPressed) },
    )
}
```

**체크리스트:**
- [ ] 라우트 키가 `NavKey` 구현
- [ ] `NavDisplay` 엔트리에 `koinViewModel()` 으로 ViewModel 주입
- [ ] 직접 `backStack.add(...)` 조작 없이 `NavigationAction` 을 통해 네비게이션 처리
- [ ] `rememberViewModelStoreNavEntryDecorator` 포함 확인

---

### Step 3 — 테스트 실행 및 수정 (에이전트 D)

에이전트 C 완료 후 실행한다.

**Agent 순서:** `tester` → (실패 시) `reviewer` → `implementer` → 다시 `tester`

#### 실행 명령

```bash
# 공통 테스트 (commonTest)
./gradlew :feature:<name>:presentation:allTests

# Android 계측 테스트 (androidTest)
./gradlew :androidApp:connectedDebugAndroidTest

# 특정 테스트 클래스 필터
./gradlew :feature:<name>:presentation:allTests --tests "com.crazyenough.unknown.feature.<name>.*"
```

#### 수정 루프

```
[tester] 테스트 실행
    │
    ├─ 전부 통과 ──→ Step 4 진행
    │
    └─ 실패 있음
           │
           ▼
    [reviewer] 실패 원인 분석
       - 컴파일 에러 (import 누락, 타입 불일치)
       - 로직 에러 (State 초깃값, Action 핸들링)
       - 시맨틱 미매칭 (testTag 불일치, 텍스트 불일치)
       - DI/Navigation 미연결
           │
           ▼
    [implementer] 최소 범위 코드 수정
       (테스트 코드는 수정하지 않음 — 테스트가 spec이다)
           │
           └──→ [tester] 다시 테스트 실행 (반복)
```

**수정 원칙:**
- 테스트 코드는 명세(spec)이므로 구현 코드를 수정
- 한 번에 한 실패씩 처리 (산탄총식 수정 금지)
- 빌드가 통과하지 않으면 DI/Navigation 설정 재확인
- 동일 실패 3회 이상 반복 시 Orchestrator가 사용자에게 에스컬레이션

---

### Step 4 — 커밋 & 푸시 (Orchestrator)

모든 테스트 통과 후 Orchestrator가 직접 수행한다.

**Skill:** `git-review-commit-push` (`~/.codex/skills/git-review-commit-push/`)

```
/git-review-commit-push
```

**Skill 동작 순서:**
1. `git status --short` 로 변경 파일 목록 확인
2. `git diff --stat` 로 diff 내용 리뷰
3. 의도한 파일만 스테이징 (`git add`)
4. `git diff --cached --stat` 로 스테이징 검증
5. 커밋 메시지 작성 후 커밋
6. remote/branch 확인 후 push
7. `git log -n 3 --oneline` 으로 결과 검증

**커밋 메시지 형식:**

```
feat: implement <FeatureName> screen with MVI + tests

- Add <Name>Contract / ViewModel / Route / Screen (MVI 4-file)
- Add <Name>PresentationModule Koin DI registration
- Register <Name>Route in NavDisplay
- Add Android instrumentation tests for <FeatureName>
- All tests passing
```

**체크리스트:**
- [ ] 스테이징 대상 파일 목록 확인 (불필요한 파일 제외)
- [ ] `local.properties`, `.env` 등 민감 파일 미포함 확인
- [ ] 커밋 타입이 `feat` 인지 확인
- [ ] force push 사용 금지 (명시적 요청 없는 한)
- [ ] 푸시 전 remote가 `origin` 인지 확인

---

## 실패 시 에스컬레이션

| 상황 | 담당 Agent | 조치 |
|------|-----------|------|
| MCP 소스 판별 불가 | `orchestrator` | `explorer`로 기존 화면 탐색 후 사용자 확인 요청 |
| 에이전트 A 실패 (UI 생성) | `orchestrator` | MCP 데이터 재확인 후 `implementer` 재시도 |
| 에이전트 B 실패 (테스트 생성) | `orchestrator` | UI 파일 완성 여부 확인 후 `tester` 단독 재시도 |
| 테스트 3회 이상 동일 실패 | `orchestrator` | 실패 로그 전달 후 사용자 판단 요청 |
| 빌드 자체가 깨진 경우 | `reviewer` → `implementer` | DI/Navigation 설정 재검토 후 Step 2부터 재실행 |
| push 실패 (remote 이슈) | `orchestrator` | remote/branch 상태 보고 후 사용자 확인 요청 |

---

## 스킬 & 에이전트 참조 요약

### Skills (`~/.codex/skills/`)

| Skill | 단계 | 용도 |
|-------|------|------|
| `figma-mcp-cmp-mvi` | Step 1-A | Figma → MVI 4-file UI 구현 |
| `stitch-mcp-cmp-mvi` | Step 1-A | Stitch → MVI 4-file UI 구현 |
| `figma-to-androidtest` | Step 1-B | Figma → commonTest + androidTest 생성 |
| `stitch-to-androidtest` | Step 1-B | Stitch → commonTest + androidTest 생성 |
| `git-review-commit-push` | Step 4 | diff 리뷰 → 커밋 → push 자동화 |

### Agents (`~/.codex/agents/`)

| Agent | 단계 | 용도 |
|-------|------|------|
| `orchestrator` | 전체 | 파이프라인 조율, 에스컬레이션 판단 |
| `explorer` | Step 0, Step 2 사전 | 코드베이스 탐색, 기존 컨벤션 파악 (read-only) |
| `implementer` | Step 1-A, Step 2, Step 3 수정 | 프로덕션 코드 작성, 최소 diff 원칙 |
| `tester` | Step 1-B, Step 3 실행 | 테스트 생성 및 실행 |
| `reviewer` | Step 3 실패 분석 | 실패 원인 분석, 보안/아키텍처 위반 검토 |

---

## 참고 문서

| 문서 | 내용 |
|------|------|
| `CLAUDE.md` | 프로젝트 전체 가이드라인 |
| `~/.claude/rules/mvi-pattern.md` | MVI 4-file 패턴 상세 |
| `~/.claude/rules/koin-migration.md` | Koin DI 초기화 및 모듈 등록 |
| `~/.claude/rules/navigation3-architecture.md` | Navigation3 라우트/백스택/엔트리 규칙 |
| `~/.claude/rules/git-conventions.md` | 커밋 메시지 형식 |
| `~/.codex/skills/figma-mcp-cmp-mvi/SKILL.md` | Figma UI 구현 스킬 상세 |
| `~/.codex/skills/stitch-mcp-cmp-mvi/SKILL.md` | Stitch UI 구현 스킬 상세 |
| `~/.codex/skills/figma-to-androidtest/SKILL.md` | Figma 테스트 생성 스킬 상세 |
| `~/.codex/skills/stitch-to-androidtest/SKILL.md` | Stitch 테스트 생성 스킬 상세 |
| `~/.codex/skills/git-review-commit-push/SKILL.md` | Git 커밋/푸시 스킬 상세 |
