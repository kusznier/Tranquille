package fr.vinetos.tranquille.presentation.denylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.vinetos.tranquille.data.DenylistImporterExporter.ImportResult
import fr.vinetos.tranquille.data.DenylistItem
import fr.vinetos.tranquille.data.DenylistUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class DenylistUiState(
    val loading: Boolean = true,
    val allItems: List<DenylistItem> = emptyList(),
    val query: String = "",
    val selection: Set<Long> = emptySet(),
    val blockDenylisted: Boolean = true,
    val busy: Boolean = false,
) {
    /** Items matching the search query, by name or by pattern (with or without wildcards). */
    val items: List<DenylistItem> by lazy {
        val q = query.trim()
        if (q.isEmpty()) {
            allItems
        } else {
            val digits = DenylistUtils.cleanNumber(q)
            allItems.filter { item ->
                item.name?.contains(q, ignoreCase = true) == true ||
                    DenylistUtils.patternToHumanReadable(item.pattern).contains(q) ||
                    (digits.isNotEmpty() && item.pattern.contains(digits))
            }
        }
    }

    val selectionMode: Boolean get() = selection.isNotEmpty()
}

sealed interface DenylistEvent {
    data class Deleted(val items: List<DenylistItem>) : DenylistEvent
    data class Imported(val result: ImportResult) : DenylistEvent
    data object ImportFailed : DenylistEvent
    data class Exported(val file: File) : DenylistEvent
    data object ExportFailed : DenylistEvent
}

class DenylistViewModel(
    private val repository: DenylistRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(DenylistUiState(blockDenylisted = repository.blockDenylisted))
    val state: StateFlow<DenylistUiState> = _state.asStateFlow()

    private val _events = Channel<DenylistEvent>(Channel.BUFFERED)
    val events: Flow<DenylistEvent> = _events.receiveAsFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val items = withContext(ioDispatcher) { repository.getAll() }
            _state.update { state ->
                val ids = items.mapTo(HashSet()) { it.id }
                state.copy(
                    loading = false,
                    allItems = items,
                    selection = state.selection.filterTo(HashSet()) { it in ids },
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun toggleSelection(id: Long) {
        _state.update {
            it.copy(selection = if (id in it.selection) it.selection - id else it.selection + id)
        }
    }

    fun selectAllVisible() {
        _state.update { it.copy(selection = it.items.mapTo(HashSet()) { item -> item.id }) }
    }

    fun clearSelection() {
        _state.update { it.copy(selection = emptySet()) }
    }

    fun setBlockDenylisted(block: Boolean) {
        repository.blockDenylisted = block
        _state.update { it.copy(blockDenylisted = block) }
    }

    /** Deletes the given items; the UI offers to undo with [restore]. */
    fun delete(ids: Collection<Long>) {
        val idSet = ids.toSet()
        val deleted = _state.value.allItems.filter { it.id in idSet }
        if (deleted.isEmpty()) return
        viewModelScope.launch {
            withContext(ioDispatcher) { repository.delete(idSet) }
            _state.update { it.copy(selection = it.selection - idSet) }
            reload()
            _events.send(DenylistEvent.Deleted(deleted))
        }
    }

    fun deleteSelected() = delete(_state.value.selection)

    /** Puts deleted items back (with new ids), keeping their names and call statistics. */
    fun restore(items: List<DenylistItem>) {
        viewModelScope.launch {
            withContext(ioDispatcher) { items.forEach { repository.insert(it.copy(id = -1L)) } }
            reload()
        }
    }

    fun import(open: () -> InputStream?) {
        runBusy {
            val result = withContext(ioDispatcher) {
                // A file that cannot be opened is reported like an unreadable one.
                runCatching { open()?.use { repository.importFrom(it) } }.getOrNull()
            }
            reload()
            _events.send(
                if (result != null) DenylistEvent.Imported(result) else DenylistEvent.ImportFailed
            )
        }
    }

    fun export(file: File) {
        runBusy {
            val ok = withContext(ioDispatcher) { repository.exportTo(file) }
            _events.send(if (ok) DenylistEvent.Exported(file) else DenylistEvent.ExportFailed)
        }
    }

    private fun runBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                block()
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
