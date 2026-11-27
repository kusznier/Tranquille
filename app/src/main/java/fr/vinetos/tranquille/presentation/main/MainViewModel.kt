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

data class MainUiState(
    val loading: Boolean = true,
    val status: ProtectionStatus = ProtectionStatus(),
    val callLog: List<CallLogRow> = emptyList(),
)

interface MainRepository {
    fun loadStatus(): ProtectionStatus
    fun loadCallLog(): List<CallLogRow>
    fun setBlockByRating(block: Boolean)
}

class MainViewModel(
    private val repository: MainRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    /** Reloads the status and the call log; called on start and after calls or updates. */
    fun refresh() {
        viewModelScope.launch {
            val status = withContext(ioDispatcher) { repository.loadStatus() }
            _state.update { it.copy(status = status) }
            val callLog = withContext(ioDispatcher) { repository.loadCallLog() }
            _state.update { it.copy(loading = false, callLog = callLog) }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val status = withContext(ioDispatcher) { repository.loadStatus() }
            _state.update { it.copy(status = status) }
        }
    }

    fun setBlockByRating(block: Boolean) {
        repository.setBlockByRating(block)
        refreshStatus()
    }
}
