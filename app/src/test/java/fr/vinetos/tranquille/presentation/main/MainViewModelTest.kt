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

    private class FakeRepo(
        var status: ProtectionStatus,
        var rows: List<CallLogRow> = emptyList(),
        var onboardingDone: Boolean = true,
    ) : MainRepository {
        override fun loadStatus() = status
        override fun loadCallLog() = rows
        override fun setBlockByRating(block: Boolean) {
            status = status.copy(blockByRating = block)
        }
        override fun isOnboardingDone() = onboardingDone
        override fun setOnboardingDone() {
            onboardingDone = true
        }
        override fun setAutoUpdates(enabled: Boolean) {
            status = status.copy(autoUpdates = enabled)
        }
    }

    private val ok = ProtectionStatus(dbVersion = 1)
    private val fresh = ProtectionStatus(dbVersion = null, callScreeningHeld = false, permissionsMissing = true)

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

    @Test
    fun firstRunShowsAllSetupStepsAndFinishingMarksItDone() {
        val repo = FakeRepo(fresh, onboardingDone = false)
        val vm = MainViewModel(repo, dispatcher)
        assertTrue(vm.state.value.onboardingPending)
        vm.refresh()
        val onboarding = vm.state.value.onboarding!!
        assertFalse(vm.state.value.onboardingPending)
        assertEquals(OnboardingStep.entries, onboarding.steps)
        assertEquals(OnboardingStep.CALL_SCREENING, onboarding.step)

        vm.nextOnboardingStep()
        assertEquals(OnboardingStep.PERMISSIONS, vm.state.value.onboarding!!.step)
        assertTrue(vm.previousOnboardingStep())
        assertEquals(OnboardingStep.CALL_SCREENING, vm.state.value.onboarding!!.step)
        assertFalse(vm.previousOnboardingStep())

        vm.nextOnboardingStep()
        vm.nextOnboardingStep()
        assertTrue(vm.state.value.onboarding!!.isLast)
        assertFalse(repo.onboardingDone)
        vm.nextOnboardingStep()
        assertNull(vm.state.value.onboarding)
        assertTrue(repo.onboardingDone)
    }

    @Test
    fun setupStepsFollowTheStatus() {
        val repo = FakeRepo(fresh, onboardingDone = false)
        val vm = MainViewModel(repo, dispatcher)
        vm.refresh()
        assertFalse(vm.state.value.status.isDone(OnboardingStep.CALL_SCREENING))
        repo.status = repo.status.copy(callScreeningHeld = true, permissionsMissing = false)
        vm.refreshStatus()
        // The setup stays open after the first decision, even when the role is granted.
        assertEquals(OnboardingStep.CALL_SCREENING, vm.state.value.onboarding!!.step)
        assertTrue(vm.state.value.status.isDone(OnboardingStep.CALL_SCREENING))
        assertTrue(vm.state.value.status.isDone(OnboardingStep.PERMISSIONS))
        assertFalse(vm.state.value.status.isDone(OnboardingStep.DATABASE))
        vm.setAutoUpdates(true)
        assertTrue(vm.state.value.status.autoUpdates)
    }

    @Test
    fun noCallScreeningStepWithoutTheRole() {
        val vm = MainViewModel(FakeRepo(fresh.copy(screeningRoleSupported = false), onboardingDone = false), dispatcher)
        vm.refresh()
        assertEquals(listOf(OnboardingStep.PERMISSIONS, OnboardingStep.DATABASE), vm.state.value.onboarding!!.steps)
    }

    @Test
    fun updateFromOlderVersionWithEverythingSetUpSkipsTheSetup() {
        val repo = FakeRepo(ok.copy(permissionsMissing = true), onboardingDone = false)
        val vm = MainViewModel(repo, dispatcher)
        vm.refresh()
        assertNull(vm.state.value.onboarding)
        assertFalse(vm.state.value.onboardingPending)
        assertTrue(repo.onboardingDone)
    }

    @Test
    fun finishedSetupIsNotShownAgain() {
        val vm = MainViewModel(FakeRepo(fresh, onboardingDone = true), dispatcher)
        assertFalse(vm.state.value.onboardingPending)
        vm.refresh()
        assertNull(vm.state.value.onboarding)
    }
}
