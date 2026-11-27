package fr.vinetos.tranquille.presentation.dbmanagement

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DbManagementViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeDb : DbMaintenance {
        val calls = mutableListOf<String>()
        override fun info() = "DB info"
        override fun startUpdate() { calls += "update" }
        override fun resetAll() { calls += "resetAll" }
        override fun resetUpdates() { calls += "resetUpdates" }
        override fun effectiveVersion() = 42
    }

    @Test
    fun showsInfoOnStart() {
        val vm = DbManagementViewModel(FakeDb(), dispatcher)
        assertEquals(DbMessage.Info("DB info"), vm.state.value.message)
    }

    @Test
    fun resetNeedsConfirmation() {
        val db = FakeDb()
        val vm = DbManagementViewModel(db, dispatcher)

        vm.askReset(PendingReset.ALL)
        vm.cancelReset()
        assertEquals(emptyList<String>(), db.calls)
        assertNull(vm.state.value.pendingReset)

        vm.askReset(PendingReset.UPDATES)
        vm.confirmReset()
        assertEquals(listOf("resetUpdates"), db.calls)
        assertEquals(DbMessage.UpdatesRemoved, vm.state.value.message)
    }

    @Test
    fun updateReportsNewVersion() {
        val db = FakeDb()
        val vm = DbManagementViewModel(db, dispatcher)
        vm.update()
        vm.onUpdateFinished()
        assertEquals(listOf("update"), db.calls)
        assertEquals(DbMessage.Updated(42), vm.state.value.message)
    }
}
