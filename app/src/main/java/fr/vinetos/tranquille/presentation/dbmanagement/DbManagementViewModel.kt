package fr.vinetos.tranquille.presentation.dbmanagement

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

/** Database maintenance operations, unchanged from the old screen. */
interface DbMaintenance {
    fun info(): String
    fun startUpdate()
    fun resetAll()
    fun resetUpdates()
    fun effectiveVersion(): Int
}

sealed interface DbMessage {
    data class Info(val text: String) : DbMessage
    data class Updated(val version: Int) : DbMessage
    data object DatabaseRemoved : DbMessage
    data object UpdatesRemoved : DbMessage
}

enum class PendingReset { ALL, UPDATES }

data class DbManagementUiState(
    val message: DbMessage? = null,
    val pendingReset: PendingReset? = null,
)

class DbManagementViewModel(
    private val db: DbMaintenance,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(DbManagementUiState())
    val state: StateFlow<DbManagementUiState> = _state.asStateFlow()

    init {
        showInfo()
    }

    fun showInfo() {
        _state.update { it.copy(message = null) }
        viewModelScope.launch {
            val info = withContext(ioDispatcher) { db.info() }
            _state.update { it.copy(message = DbMessage.Info(info)) }
        }
    }

    fun update() {
        _state.update { it.copy(message = null) }
        db.startUpdate()
    }

    fun onUpdateFinished() {
        _state.update { it.copy(message = DbMessage.Updated(db.effectiveVersion())) }
    }

    /** Resets are destructive, so they need a confirmation first. */
    fun askReset(reset: PendingReset) = _state.update { it.copy(pendingReset = reset) }

    fun cancelReset() = _state.update { it.copy(pendingReset = null) }

    fun confirmReset() {
        val reset = _state.value.pendingReset ?: return
        _state.update { it.copy(pendingReset = null, message = null) }
        viewModelScope.launch {
            withContext(ioDispatcher) {
                if (reset == PendingReset.ALL) db.resetAll() else db.resetUpdates()
            }
            _state.update {
                it.copy(message = if (reset == PendingReset.ALL) DbMessage.DatabaseRemoved else DbMessage.UpdatesRemoved)
            }
        }
    }
}
