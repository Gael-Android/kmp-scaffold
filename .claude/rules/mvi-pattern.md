# MVI Architecture Pattern (Required)

**Every feature must follow a 4-file structure:**
1. **Contract**: Defines State, Action, and Event
2. **ViewModel**: Business logic and state management
3. **Route**: Navigation and event handling
4. **Screen**: UI rendering (Stateless)

## 1. Contract (State + Action + Event)

```kotlin
package com.example.feature

// State: immutable data representing UI state
data class FeatureState(
    val data: String = "",
    val isLoading: Boolean = false,
)

// Action: user behavior from the UI perspective (what happened)
// Does not define how it is handled (ex. OnClick, OnSwipe)
sealed interface FeatureAction {
    data object OnClick : FeatureAction
    data class OnTextChange(val text: String) : FeatureAction
}

// Event: one-time event sent from ViewModel to UI
// Defines actions the UI should perform (ex. NavigateTo, ShowDialog)
sealed interface FeatureEvent {
    data class ShowToast(val message: String) : FeatureEvent
    data object NavigateUp : FeatureEvent
    data class NavigateTo(val route: String) : FeatureEvent
}
```

## 2. ViewModel (Business Logic)

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // private val useCase: UseCase,
) : ViewModel() {

    private val _stateFlow = MutableStateFlow(FeatureState())
    val stateFlow: StateFlow<FeatureState> = _stateFlow
        .onStart {
            // Initial data load
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = FeatureState()
        )

    private val _eventChannel = Channel<FeatureEvent>(Channel.BUFFERED)
    val eventFlow = _eventChannel.receiveAsFlow()
    
    // Event sending helper
    private suspend fun sendEvent(event: FeatureEvent) {
        _eventChannel.send(event)
    }
}
```

## 3. Route (Navigation and Event Handling)

```kotlin
@Composable
fun FeatureRoute(
    navigateUp: () -> Unit,
    viewModel: FeatureViewModel = hiltViewModel(),
) {
    // Subscribe to state
    val uiState by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle one-time events (core/presentation ObserveAsEvents)
    ObserveAsEvents(flow = viewModel.eventFlow) { event ->
        when (event) {
            FeatureEvent.NavigateUp -> navigateUp()
            is FeatureEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is FeatureEvent.NavigateTo -> {
                // Handle navigation
            }
        }
    }

    // Action handler
    val actionsHandler: (FeatureAction) -> Unit = { action ->
        when (action) {
            FeatureAction.OnClick -> viewModel.handleClick()
            is FeatureAction.OnTextChange -> viewModel.updateText(action.text)
        }
    }

    // Render UI
    FeatureScreen(
        state = uiState,
        onAction = actionsHandler
    )
}
```

### ObserveAsEvents (Required in core/presentation)

If `ObserveAsEvents` does not exist, create it in `core/presentation` with this implementation:

```kotlin
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, key1, key2) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}
```

## 4. Screen (Stateless UI)

```kotlin
@Composable
fun FeatureScreen(
    state: FeatureState,
    onAction: (FeatureAction) -> Unit,
) {
    // UI implementation (pure Composable, no state)
    // Render UI only from state
    // Send user input only through onAction
}

// Preview (Required)
@Preview
@Composable
private fun FeatureScreenPreview(
    @PreviewParameter(FeatureStatePreviewParameterProvider::class)
    state: FeatureState,
) {
    FeatureScreen(
        state = state,
        onAction = {}
    )
}

class FeatureStatePreviewParameterProvider : PreviewParameterProvider<FeatureState> {
    override val values: Sequence<FeatureState>
        get() = sequenceOf(
            FeatureState(),
            FeatureState(data = "Sample", isLoading = true),
        )
}
```

## Core MVI Principles

- **Unidirectional data flow**: Action -> ViewModel -> State -> UI
- **State is immutable**: always create a new State object
- **Screen is stateless**: only receives state and onAction
- **Event is one-time**: implement with Channel (not State)
- **Route event collection**: collect one-time events with `ObserveAsEvents` in `core/presentation`; if missing, create it with the canonical implementation above
- **Action expresses intent only**: represent what happened, and handle how in ViewModel
- **Preview is required**: preview multiple states with PreviewParameterProvider
- **Back handling is an Action**: UI dispatches `BackPressed`, reducer/ViewModel decides policy, navigator executes

## Navigation + MVI Responsibility Split

- UI responsibility: capture user/system back input and dispatch Action only
- Store/ViewModel responsibility: decide navigation policy (`Pop`, `SwitchTab`, `Exit`)
- Navigator responsibility: apply navigation effects to BackStack
