package fr.vinetos.tranquille.presentation.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dummydomain.yetanothercallblocker.sia.model.CommunityReview
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Loads online reviews; this sends the number to the reviews service (unchanged protocol). */
fun interface ReviewsLoader {
    fun load(number: String): List<CommunityReview>?
}

sealed interface ReviewsUiState {
    data object Loading : ReviewsUiState
    data object Error : ReviewsUiState
    data class Loaded(val reviews: List<CommunityReview>) : ReviewsUiState {
        val negative get() = reviews.count { it.rating == CommunityReview.Rating.NEGATIVE }
        val neutral get() = reviews.count { it.rating == CommunityReview.Rating.NEUTRAL }
        val positive get() = reviews.count { it.rating == CommunityReview.Rating.POSITIVE }
    }
}

class ReviewsViewModel(
    private val loader: ReviewsLoader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewsUiState>(ReviewsUiState.Loading)
    val state: StateFlow<ReviewsUiState> = _state.asStateFlow()

    var number: String? = null
        private set

    /** Loads reviews for [number] unless they are already loaded for it. */
    fun load(number: String) {
        if (number == this.number && _state.value !is ReviewsUiState.Error) return
        this.number = number
        _state.value = ReviewsUiState.Loading
        viewModelScope.launch {
            val reviews = withContext(ioDispatcher) { loader.load(number) }
            _state.value = if (reviews != null) ReviewsUiState.Loaded(reviews) else ReviewsUiState.Error
        }
    }
}
