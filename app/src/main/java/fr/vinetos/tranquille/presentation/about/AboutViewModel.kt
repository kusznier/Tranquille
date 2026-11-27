package fr.vinetos.tranquille.presentation.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Read-only view of the number database state shown on the About screen. */
interface DbStatusSource {
    /** Effective database version, or null when the database is not operational. */
    fun dbVersion(): Int?

    /** Time of the last update check in millis, 0 if never. */
    fun lastUpdateCheckTime(): Long

    /** Whether a database update is running right now. */
    fun isUpdating(): Boolean
}

data class AboutUiState(
    val versionName: String,
    val dbVersion: Int? = null,
    val lastUpdateCheckTime: Long = 0,
    val checkingForUpdates: Boolean = false,
) {
    val canCheckForUpdates: Boolean get() = dbVersion != null && !checkingForUpdates
}

class AboutViewModel(
    versionName: String,
    private val source: DbStatusSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(AboutUiState(versionName = versionName))
    val state: StateFlow<AboutUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(checkingForUpdates = source.isUpdating()) }
        refresh()
    }

    /** Re-reads the database state (after a download or an update). */
    fun refresh() {
        viewModelScope.launch {
            val version = withContext(ioDispatcher) { source.dbVersion() }
            val lastCheck = source.lastUpdateCheckTime()
            _state.update { it.copy(dbVersion = version, lastUpdateCheckTime = lastCheck) }
        }
    }

    fun onUpdateStarted() {
        _state.update { it.copy(checkingForUpdates = true) }
    }

    fun onUpdateFinished() {
        _state.update { it.copy(checkingForUpdates = false) }
        refresh()
    }
}
