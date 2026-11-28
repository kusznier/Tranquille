package fr.vinetos.tranquille.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.vinetos.tranquille.CallLogRow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the status card on the main screen needs to know. */
data class ProtectionStatus(
    /** Holds the call screening role (Android 10+) or is the default dialer. */
    val callScreeningHeld: Boolean = true,
    /** At least one blocking option is enabled. */
    val blockingEnabled: Boolean = true,
    val blockByRating: Boolean = false,
    /** Effective database version, null when the main database is missing. */
    val dbVersion: Int? = null,
    val dbDownloading: Boolean = false,
    val lastUpdateTime: Long = 0,
    val denylistCount: Int = 0,
    /** Notifications are wanted by the settings but not allowed by the system. */
    val notificationsBlocked: Boolean = false,
    val hasCallLogPermission: Boolean = true,
    /** Some runtime permission the current settings need is not granted. */
    val permissionsMissing: Boolean = false,
    /** The call screening role exists on this Android version (10+). */
    val screeningRoleSupported: Boolean = true,
    val autoUpdates: Boolean = false,
)

/** The single most important problem, if any, in the order the card shows them. */
enum class StatusProblem { NO_DATABASE, SCREENING_OFF, BLOCKING_OFF, NOTIFICATIONS_OFF }

val ProtectionStatus.problem: StatusProblem?
    get() = when {
        dbVersion == null -> StatusProblem.NO_DATABASE
        !callScreeningHeld -> StatusProblem.SCREENING_OFF
        !blockingEnabled -> StatusProblem.BLOCKING_OFF
        notificationsBlocked -> StatusProblem.NOTIFICATIONS_OFF
        else -> null
    }

/** Steps of the first-run setup (U7), in the order they are shown. */
enum class OnboardingStep { CALL_SCREENING, PERMISSIONS, DATABASE }

data class OnboardingState(val steps: List<OnboardingStep>, val index: Int = 0) {
    val step: OnboardingStep get() = steps[index]
    val isLast: Boolean get() = index == steps.lastIndex
}

/** Whether the thing a step asks for is already in place. */
fun ProtectionStatus.isDone(step: OnboardingStep): Boolean = when (step) {
    OnboardingStep.CALL_SCREENING -> callScreeningHeld
    OnboardingStep.PERMISSIONS -> !permissionsMissing
    OnboardingStep.DATABASE -> dbVersion != null
}

data class MainUiState(
    val loading: Boolean = true,
    val status: ProtectionStatus = ProtectionStatus(),
    val callLog: List<CallLogRow> = emptyList(),
    /** First-run setup is shown instead of the main screen while this is set. */
    val onboarding: OnboardingState? = null,
    /** Not yet known whether the first-run setup is needed; nothing is shown meanwhile. */
    val onboardingPending: Boolean = false,
)

interface MainRepository {
    fun loadStatus(): ProtectionStatus
    fun loadCallLog(): List<CallLogRow>
    fun setBlockByRating(block: Boolean)
    fun isOnboardingDone(): Boolean
    fun setOnboardingDone()
    fun setAutoUpdates(enabled: Boolean)
}

class MainViewModel(
    private val repository: MainRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState(onboardingPending = !repository.isOnboardingDone()))
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    /** Reloads the status and the call log; called on start and after calls or updates. */
    fun refresh() {
        viewModelScope.launch {
            applyStatus(withContext(ioDispatcher) { repository.loadStatus() })
            val callLog = withContext(ioDispatcher) { repository.loadCallLog() }
            _state.update { it.copy(loading = false, callLog = callLog) }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            applyStatus(withContext(ioDispatcher) { repository.loadStatus() })
        }
    }

    private fun applyStatus(status: ProtectionStatus) {
        if (!_state.value.onboardingPending) {
            _state.update { it.copy(status = status) }
            return
        }
        // Decide once whether to show the first-run setup. Someone updating from an
        // older version already has the database and the role, so it is skipped.
        val steps = buildList {
            if (status.screeningRoleSupported) add(OnboardingStep.CALL_SCREENING)
            add(OnboardingStep.PERMISSIONS)
            add(OnboardingStep.DATABASE)
        }
        val setUp = status.dbVersion != null && status.callScreeningHeld
        if (setUp) repository.setOnboardingDone()
        _state.update {
            it.copy(
                status = status,
                onboardingPending = false,
                onboarding = if (setUp) null else OnboardingState(steps),
            )
        }
    }

    /** Moves to the next setup step, or closes the setup after the last one. */
    fun nextOnboardingStep() {
        val onboarding = _state.value.onboarding ?: return
        if (onboarding.isLast) finishOnboarding()
        else _state.update { it.copy(onboarding = onboarding.copy(index = onboarding.index + 1)) }
    }

    /** Goes back one step; returns false on the first step so the back press can leave the app. */
    fun previousOnboardingStep(): Boolean {
        val onboarding = _state.value.onboarding ?: return false
        if (onboarding.index == 0) return false
        _state.update { it.copy(onboarding = onboarding.copy(index = onboarding.index - 1)) }
        return true
    }

    fun finishOnboarding() {
        repository.setOnboardingDone()
        _state.update { it.copy(onboarding = null) }
    }

    fun setAutoUpdates(enabled: Boolean) {
        viewModelScope.launch {
            withContext(ioDispatcher) { repository.setAutoUpdates(enabled) }
            applyStatus(withContext(ioDispatcher) { repository.loadStatus() })
        }
    }

    fun setBlockByRating(block: Boolean) {
        repository.setBlockByRating(block)
        refreshStatus()
    }
}
