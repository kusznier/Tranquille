package fr.vinetos.tranquille.presentation.main

import fr.vinetos.tranquille.CallLogRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeRepo(var status: ProtectionStatus, var rows: List<CallLogRow> = emptyList()) : MainRepository {
        override fun loadStatus() = status
        override fun loadCallLog() = rows
        override fun setBlockByRating(block: Boolean) {
            status = status.copy(blockByRating = block)
        }
    }

    private val ok = ProtectionStatus(dbVersion = 1)

    @Test
    fun problemsAreShownInPriorityOrder() {
        assertNull(ok.problem)
        assertEquals(StatusProblem.NOTIFICATIONS_OFF, ok.copy(notificationsBlocked = true).problem)
        assertEquals(
            StatusProblem.BLOCKING_OFF,
            ok.copy(notificationsBlocked = true, blockingEnabled = false).problem,
        )
        assertEquals(
            StatusProblem.SCREENING_OFF,
            ok.copy(blockingEnabled = false, callScreeningHeld = false).problem,
        )
        assertEquals(
            StatusProblem.NO_DATABASE,
            ok.copy(dbVersion = null, callScreeningHeld = false).problem,
        )
    }

    @Test
    fun refreshLoadsStatusAndCallLog() {
        val vm = MainViewModel(FakeRepo(ok.copy(denylistCount = 3)), dispatcher)
        assertTrue(vm.state.value.loading)
        vm.refresh()
        assertFalse(vm.state.value.loading)
        assertEquals(3, vm.state.value.status.denylistCount)
    }

    @Test
    fun blockByRatingSwitchUpdatesStatus() {
        val repo = FakeRepo(ok)
        val vm = MainViewModel(repo, dispatcher)
        vm.setBlockByRating(true)
        assertTrue(vm.state.value.status.blockByRating)
        assertTrue(repo.status.blockByRating)
    }
}
