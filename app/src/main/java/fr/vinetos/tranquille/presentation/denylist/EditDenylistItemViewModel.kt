package fr.vinetos.tranquille.presentation.denylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.vinetos.tranquille.data.DenylistItem
import fr.vinetos.tranquille.data.DenylistUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

enum class PatternError { EMPTY, INCORRECT }

data class EditDenylistItemUiState(
    val loading: Boolean = false,
    /** The item being edited, or null when adding a new one. */
    val item: DenylistItem? = null,
    val name: String = "",
    /** Pattern as the user types it, with "*" and "#" wildcards. */
    val pattern: String = "",
    /** Set once the user tried to save or edited the pattern. */
    val showErrors: Boolean = false,
    /** True after the item was saved or deleted: the screen should close. */
    val finished: Boolean = false,
    /** True if the item to edit no longer exists. */
    val missing: Boolean = false,
) {
    val isNew: Boolean get() = item == null

    val cleanPattern: String
        get() = DenylistUtils.cleanPattern(DenylistUtils.patternFromHumanReadable(pattern))

    /** Same rule as before: an empty pattern is fine for a new item until it is saved. */
    val patternError: PatternError?
        get() = when {
            pattern.isEmpty() && isNew && !showErrors -> null
            pattern.isEmpty() -> PatternError.EMPTY
            !DenylistUtils.isValidPattern(cleanPattern) -> PatternError.INCORRECT
            else -> null
        }
}

class EditDenylistItemViewModel(
    private val repository: DenylistRepository,
    itemId: Long?,
    initialName: String?,
    initialPattern: String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(EditDenylistItemUiState())
    val state: StateFlow<EditDenylistItemUiState> = _state.asStateFlow()

    init {
        if (itemId != null) {
            _state.update { it.copy(loading = true) }
            viewModelScope.launch {
                val item = withContext(ioDispatcher) { repository.findById(itemId) }
                _state.update {
                    if (item == null) {
                        it.copy(loading = false, missing = true)
                    } else {
                        it.copy(
                            loading = false,
                            item = item,
                            name = item.name.orEmpty(),
                            pattern = DenylistUtils.patternToHumanReadable(item.pattern),
                        )
                    }
                }
            }
        } else {
            _state.update {
                it.copy(
                    name = initialName.orEmpty(),
                    pattern = initialPattern?.takeIf { p -> p.isNotEmpty() }
                        ?.let(DenylistUtils::patternToHumanReadable).orEmpty(),
                )
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }

    fun onPatternChange(pattern: String) = _state.update { it.copy(pattern = pattern) }

    /**
     * Saves the item. A new item needs a valid pattern; the name is optional.
     * An existing item is only written when something changed.
     */
    fun save() {
        val current = _state.value
        val withErrors = current.copy(showErrors = true)
        if (withErrors.patternError != null) {
            _state.value = withErrors
            return
        }

        val name = current.name
        val pattern = current.cleanPattern
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val item = current.item
                if (item != null) {
                    val invalid = !DenylistUtils.isValidPattern(pattern)
                    if (name != item.name.orEmpty() || pattern != item.pattern || invalid != item.invalid) {
                        repository.update(item.copy(name = name, pattern = pattern, invalid = invalid))
                    }
                } else if (repository.findByNameAndPattern(name, pattern) == null) {
                    repository.insert(
                        DenylistItem(
                            id = -1L,
                            name = name,
                            pattern = pattern,
                            creationDate = Date(),
                            invalid = false,
                            numberOfCalls = 0,
                            lastCallDate = null,
                        )
                    )
                }
            }
            _state.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val item = _state.value.item ?: return
        viewModelScope.launch {
            withContext(ioDispatcher) { repository.delete(listOf(item.id)) }
            _state.update { it.copy(finished = true) }
        }
    }
}
