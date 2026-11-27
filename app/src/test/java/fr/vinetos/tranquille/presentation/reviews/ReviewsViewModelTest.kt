package fr.vinetos.tranquille.presentation.reviews

import dummydomain.yetanothercallblocker.sia.model.CommunityReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun review(id: Int, rating: CommunityReview.Rating) = CommunityReview().apply {
        this.id = id
        this.rating = rating
    }

    @Test
    fun countsRatingsAndLoadsOncePerNumber() {
        var loads = 0
        val vm = ReviewsViewModel(ReviewsLoader {
            loads++
            listOf(
                review(1, CommunityReview.Rating.NEGATIVE),
                review(2, CommunityReview.Rating.NEGATIVE),
                review(3, CommunityReview.Rating.POSITIVE),
            )
        }, dispatcher)

        vm.load("999")
        vm.load("999")
        val state = vm.state.value as ReviewsUiState.Loaded
        assertEquals(1, loads)
        assertEquals(2, state.negative)
        assertEquals(0, state.neutral)
        assertEquals(1, state.positive)
    }

    @Test
    fun failureShowsErrorAndRetriesOnNextLoad() {
        var result: List<CommunityReview>? = null
        var loads = 0
        val vm = ReviewsViewModel(ReviewsLoader { loads++; result }, dispatcher)
        vm.load("999")
        assertEquals(ReviewsUiState.Error, vm.state.value)

        result = emptyList()
        vm.load("999")
        assertEquals(2, loads)
        assertEquals(ReviewsUiState.Loaded(emptyList()), vm.state.value)
    }
}
