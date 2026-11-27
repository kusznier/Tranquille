package fr.vinetos.tranquille.presentation.about

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
class AboutViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeSource(
        var version: Int? = 100,
        var lastCheck: Long = 0,
        var updating: Boolean = false,
    ) : DbStatusSource {
        override fun dbVersion() = version
        override fun lastUpdateCheckTime() = lastCheck
        override fun isUpdating() = updating
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsDatabaseState() {
        val vm = AboutViewModel("1.0", FakeSource(version = 4213, lastCheck = 5), dispatcher)
        val state = vm.state.value
        assertEquals(4213, state.dbVersion)
        assertEquals(5L, state.lastUpdateCheckTime)
        assertTrue(state.canCheckForUpdates)
    }

    @Test
    fun cannotCheckWithoutDatabase() {
        val vm = AboutViewModel("1.0", FakeSource(version = null), dispatcher)
        assertNull(vm.state.value.dbVersion)
        assertFalse(vm.state.value.canCheckForUpdates)
    }

    @Test
    fun updateLifecycle() {
        val source = FakeSource(version = 1)
        val vm = AboutViewModel("1.0", source, dispatcher)

        vm.onUpdateStarted()
        assertTrue(vm.state.value.checkingForUpdates)
        assertFalse(vm.state.value.canCheckForUpdates)

        source.version = 2
        source.lastCheck = 99
        vm.onUpdateFinished()
        assertFalse(vm.state.value.checkingForUpdates)
        assertEquals(2, vm.state.value.dbVersion)
        assertEquals(99L, vm.state.value.lastUpdateCheckTime)
    }

    @Test
    fun startsInCheckingStateWhenUpdateRuns() {
        val vm = AboutViewModel("1.0", FakeSource(updating = true), dispatcher)
        assertTrue(vm.state.value.checkingForUpdates)
    }
}
