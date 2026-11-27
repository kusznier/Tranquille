package fr.vinetos.tranquille.presentation.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dummydomain.yetanothercallblocker.sia.model.database.CommunityDatabaseItem
import dummydomain.yetanothercallblocker.sia.model.database.FeaturedDatabaseItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Offline lookup in the number databases. */
fun interface NumberLookup {
    fun lookup(pureNumber: String): Pair<CommunityDatabaseItem?, FeaturedDatabaseItem?>
}

data class LookupResult(
    val community: CommunityDatabaseItem?,
    val featured: FeaturedDatabaseItem?,
)

data class LookupUiState(
    val input: String = "",
    val showNotANumber: Boolean = false,
    val searching: Boolean = false,
    val result: LookupResult? = null,
)

class LookupViewModel(
    private val lookup: NumberLookup,
    private val countryCode: () -> String?,
    initialInput: String = "",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(LookupUiState(input = initialInput))
    val state: StateFlow<LookupUiState> = _state.asStateFlow()

    private var queryJob: Job? = null

    init {
        if (pureNumber().isNotEmpty()) search()
    }

    /** Digits only; same rules as before, including the Russian 8 -> 7 prefix fix. */
    fun pureNumber(): String {
        var pure = _state.value.input.replace(Regex("[^\\d]"), "")
        if (pure.length == 11 && pure[0] == '8' && countryCode() == "RU") {
            pure = "7" + pure.substring(1)
        }
        return pure
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input, showNotANumber = false, result = null) }
    }

    /** Validates the input; shows the error and returns false when it has no digits. */
    fun requireNumber(): Boolean {
        val ok = pureNumber().isNotEmpty()
        if (!ok) _state.update { it.copy(showNotANumber = true) }
        return ok
    }

    fun search() {
        if (!requireNumber()) return
        val number = pureNumber()
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            _state.update { it.copy(searching = true, result = null) }
            val (community, featured) = withContext(ioDispatcher) { lookup.lookup(number) }
            _state.update { it.copy(searching = false, result = LookupResult(community, featured)) }
        }
    }

    fun clear() {
        queryJob?.cancel()
        _state.value = LookupUiState()
    }
}
