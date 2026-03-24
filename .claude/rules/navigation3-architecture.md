# Navigation3 Architecture Rules (Team Standard)

> This document defines the canonical implementation pattern for Navigation3 in CMP and Android projects.
> Navigation3 is state-driven. Do not treat it as a graph DSL.

## 1. Route Definition

### Rules
- All routes must implement `NavKey`
- `@Serializable` is required only when using `rememberNavBackStack` (state restoration).
  When using `mutableStateListOf` for the backstack, `@Serializable` is not needed.
- Top-level destinations implement `TopLevelRoute` (extends `NavKey`); child screens implement `NavKey` directly
- Nested navigation should use a sealed hierarchy
- Route arguments must be defined as `data class`; argument-less routes as `data object`
- Never use string-based routing

### Canonical Snippet (with state restoration)
```kotlin
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Auth : Route

    @Serializable
    data object Todo : Route

    @Serializable
    sealed interface AuthChild : Route {
        @Serializable data object Login : AuthChild
        @Serializable data object Register : AuthChild
    }

    @Serializable
    sealed interface TodoChild : Route {
        @Serializable data object TodoList : TodoChild
        @Serializable data class TodoDetail(val id: String) : TodoChild
    }
}
```

### Canonical Snippet (without state restoration)
```kotlin
import androidx.navigation3.runtime.NavKey
import com.crazyenough.unknown.core.navigation.TopLevelRoute

data object Home : TopLevelRoute
data object Settings : NavKey
data class ItemDetail(val id: String) : NavKey
```

### Anti-Patterns
- `"todo_detail/{id}"` style routing
- Enums used as route keys instead of a sealed hierarchy
- Passing screen arguments through `ViewModel` instead of route keys

## 2. BackStack Configuration

### Rules
- Explicitly set the start destination
- Two valid approaches exist:

**Option A: `rememberNavBackStack` (state restoration across config changes)**
- Requires `@Serializable` routes and `SavedStateConfiguration`
- All `NavKey` subclasses must be registered in `polymorphic`

**Option B: `mutableStateListOf` (simpler, no serialization needed)**
- Backstack is a plain `SnapshotStateList<NavKey>` managed by the developer
- No state restoration on config change (acceptable when reducer owns the truth)
- Used by the AndroidX official tests and this project's reducer-based approach

### Canonical Snippet (Option A — with state restoration)
```kotlin
val backStack = rememberNavBackStack(
    configuration = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(Route.Auth::class, Route.Auth.serializer())
                subclass(Route.Todo::class, Route.Todo.serializer())
                subclass(Route.TodoChild.TodoList::class, Route.TodoChild.TodoList.serializer())
                subclass(Route.TodoChild.TodoDetail::class, Route.TodoChild.TodoDetail.serializer())
            }
        }
    },
    Route.TodoChild.TodoList
)
```

### Canonical Snippet (Option B — reducer-driven)
```kotlin
val backStack = remember { mutableStateListOf<NavKey>(startRoute) }
```

### Anti-Patterns
- Forgetting to register a route subtype in `polymorphic` (when using Option A)
- Mixing both approaches in the same navigation scope

## 3. NavDisplay and Entry Registration

### Rules
- Always use `NavDisplay` (never `NavHost`)
- Include `rememberSaveableStateHolderNavEntryDecorator` for state preservation
  (included by default; `rememberViewModelStoreNavEntryDecorator` when ViewModel scoping is needed)
- Two valid approaches for entry registration:
  - `entryProvider` DSL with reified `entry<T>` — type-safe, preferred for `@Serializable` routes
  - `when(key)` lambda — simpler, works with any `NavKey`, used in AndroidX official tests
- Never mix Graph DSL mental model with Navigation3

### Canonical Snippet (entryProvider DSL)
```kotlin
NavDisplay(
    backStack = backStack,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
        entry<Route.TodoChild.TodoList> {
            TodoListScreen()
        }
        entry<Route.TodoChild.TodoDetail> { key ->
            TodoDetailScreen(id = key.id)
        }
    }
)
```

### Canonical Snippet (when-lambda — reducer-driven)
```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { handleAction(NavigationAction.BackPressed) },
) { key ->
    when (key) {
        HomeRoute -> NavEntry(key) { HomeScreen(onAction = ::handleAction) }
        is ItemDetail -> NavEntry(key) { DetailScreen(id = key.id) }
        else -> NavEntry(key) { Text("Unknown screen") }
    }
}
```

### Anti-Patterns
- Omitting required decorators when ViewModel scoping is needed
- Creating/injecting `ViewModel` outside NavEntry scope

## 4. Nested Navigation

### Rules
- Nested graphs must own independent back stacks
- Parent graph treats nested graph as a single route
- Shared `ViewModel` must be scoped at the graph boundary

### Canonical Snippet
```kotlin
entry<Route.Auth> {
    AuthNavigation()
}

@Composable
fun AuthNavigation() {
    val authBackStack = rememberNavBackStack(
        configuration = SavedStateConfiguration { /* ... */ },
        Route.AuthChild.Login
    )
    NavDisplay(backStack = authBackStack, /* ... */)
}
```

### Anti-Patterns
- Sharing the parent back stack across nested flows
- Creating a root `ViewModel` and passing it through every nested screen
- Mixing nested and flat routing models in one flow

## 5. Bottom Navigation

### Rules
- Back behavior:
  At top-level root, switch to `startRoute`; otherwise pop current stack
- The reducer (`navigationReduce`) handles this logic as a pure function
- The caller provides `isAtStackRoot` to let the reducer decide Pop vs SwitchTopLevel

### Project Pattern (single backstack + reducer)
This project uses a single `SnapshotStateList<NavKey>` and a pure reducer function.
Tab switches clear the stack and push the new top-level route.

```kotlin
val backStack = remember { mutableStateListOf<NavKey>(startRoute) }
var navState by remember { mutableStateOf(NavigationState(startRoute, startRoute)) }

fun handleAction(action: NavigationAction) {
    val isAtStackRoot = backStack.size <= 1
    val (newState, effect) = navigationReduce(navState, action, isAtStackRoot)
    navState = newState

    when (effect) {
        is NavigationEffect.Push -> backStack.add(effect.route)
        is NavigationEffect.Pop -> backStack.removeLastOrNull()
        is NavigationEffect.SwitchTopLevel -> {
            backStack.clear()
            backStack.add(effect.route)
        }
        null -> Unit
    }
}
```

### Alternative (multi-backstack)
For preserving per-tab stack state across tab switches, maintain
a `Map<TopLevelRoute, List<NavKey>>` and swap the active backstack on tab change.

### Anti-Patterns
- Delegating all back behavior implicitly to system default
- Bypassing the reducer to manipulate backstack directly from UI

## 6. Immutable State and Reducer Model

### Rules
- Navigation state must be immutable at the app/store boundary
- All navigation decisions must go through reducer logic
- Reducer returns state plus effect intent (`Push`, `Pop`, `SwitchTopLevel`)
- The caller applies effects to the backstack; Compose only renders state
- This does not conflict with Compose when mutation is isolated to effect handling

### Canonical Snippet (matches `core/navigation`)
```kotlin
@Immutable
data class NavigationState(
    val startRoute: TopLevelRoute,
    val currentTopLevelRoute: TopLevelRoute,
)

sealed interface NavigationAction {
    data class Navigate(val route: NavKey) : NavigationAction
    data object BackPressed : NavigationAction
    data class SwitchTab(val route: TopLevelRoute) : NavigationAction
}

sealed interface NavigationEffect {
    data class Push(val route: NavKey) : NavigationEffect
    data object Pop : NavigationEffect
    data class SwitchTopLevel(val route: TopLevelRoute) : NavigationEffect
}

fun navigationReduce(
    state: NavigationState,
    action: NavigationAction,
    isAtStackRoot: Boolean,
): Pair<NavigationState, NavigationEffect?>
```

### Anti-Patterns
- Mutating navigation state from Composable functions
- Letting reducers call Android navigation APIs directly
- Bypassing the reducer to manipulate backstack directly

## 7. Module Boundary (core-navigation)

### Rules
- Put navigation contracts and engine code in `core/navigation` (`commonMain`)
- Keep feature route definitions and entry registration in feature `presentation`
- `core/navigation` must not depend on any feature module
- Features depend on `core/navigation`, not vice versa

### Canonical Snippet
```text
core/navigation
  - NavigationState
  - NavigationAction / NavigationEffect / reducer
  - Navigator interface
  - SavedState + serializer helpers

feature/todo/presentation
  - TodoRoute keys
  - Todo entryProvider registrations
  - TodoRoute composables
```

### Anti-Patterns
- Putting all navigation code in each feature without shared contracts
- Placing feature-specific routes in `core/navigation`
- Creating cyclic dependencies between feature modules through navigation

## 8. Back Responsibility in MVI

### Rules
- UI captures hardware/back gesture and dispatches `Action.BackPressed`
- Store/reducer owns the back policy decision
- Navigator executes emitted effect (`Pop`, `SwitchTab`, `ExitApp`)
- UI must never directly mutate back stacks

### Canonical Snippet
```kotlin
@Composable
fun TodoRoute(store: TodoStore) {
    BackHandler {
        store.dispatch(TodoAction.BackPressed)
    }
}

// reducer decides:
// BackPressed -> Pop | SwitchTopLevel(startRoute) | ExitApp
```

### Anti-Patterns
- Calling `popBackStack()` directly from UI widgets
- Handling back policy in multiple layers with conflicting logic
- Mixing system back and custom back without a single reducer policy

## 9. Architectural Philosophy

Navigation3 is:
`Route -> BackStack -> Decorated Entries -> Rendering`

It is:
- State-driven
- Serializable-based
- BackStack-centric

It is not:
- XML graph replacement
- String route mapper
- Fragment-style navigation

## Final Principle

Never think in terms of a "navigation tree".
Think in terms of back stack state evolution.
Navigation is state mutation plus rendering projection.
