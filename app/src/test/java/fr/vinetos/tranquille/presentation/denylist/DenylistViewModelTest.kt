package fr.vinetos.tranquille.presentation.denylist

import fr.vinetos.tranquille.data.DenylistImporterExporter.ImportResult
import fr.vinetos.tranquille.data.DenylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class DenylistViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private fun item(id: Long, name: String?, pattern: String, calls: Long = 0) =
        DenylistItem(id, name, pattern, Date(1000), false, calls, null)

    private val sample = listOf(
        item(1, "Solar offers", "+99900000101", calls = 3),
        item(2, null, "+999123%"),
        item(3, "Bank", "+99800000555"),
    )

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsAndSearchesByNameOrNumber() {
        val vm = DenylistViewModel(FakeDenylistRepository(sample), dispatcher)
        assertEquals(3, vm.state.value.items.size)
        assertFalse(vm.state.value.loading)

        vm.onQueryChange("solar")
        assertEquals(listOf(1L), vm.state.value.items.map { it.id })

        // wildcard patterns are searched in their human-readable form
        vm.onQueryChange("+999123*")
        assertEquals(listOf(2L), vm.state.value.items.map { it.id })

        // digits match regardless of spaces typed by the user
        vm.onQueryChange("998 000")
        assertEquals(listOf(3L), vm.state.value.items.map { it.id })
    }

    @Test
    fun deleteSelectedThenUndoKeepsData() = runTest(dispatcher) {
        val repo = FakeDenylistRepository(sample)
        val vm = DenylistViewModel(repo, dispatcher)

        vm.toggleSelection(1)
        vm.toggleSelection(3)
        assertTrue(vm.state.value.selectionMode)
        vm.deleteSelected()

        val event = vm.events.first() as DenylistEvent.Deleted
        assertEquals(setOf(1L, 3L), event.items.map { it.id }.toSet())
        assertEquals(listOf(2L), vm.state.value.allItems.map { it.id })
        assertFalse(vm.state.value.selectionMode)

        vm.restore(event.items)
        val restored = vm.state.value.allItems.first { it.name == "Solar offers" }
        assertEquals(3, vm.state.value.allItems.size)
        assertEquals(3L, restored.numberOfCalls)
        assertEquals("+99900000101", restored.pattern)
    }

    @Test
    fun selectAllSelectsOnlyVisibleItems() {
        val vm = DenylistViewModel(FakeDenylistRepository(sample), dispatcher)
        vm.onQueryChange("+999")
        vm.selectAllVisible()
        assertEquals(setOf(1L, 2L), vm.state.value.selection)
        vm.clearSelection()
        assertFalse(vm.state.value.selectionMode)
    }

    @Test
    fun importReportsCountsOrFailure() = runTest(dispatcher) {
        val repo = FakeDenylistRepository(sample)
        val vm = DenylistViewModel(repo, dispatcher)

        repo.importResult = ImportResult(added = 5, updated = 1, skipped = 2)
        vm.import { ByteArrayInputStream(ByteArray(0)) }
        assertEquals(DenylistEvent.Imported(ImportResult(5, 1, 2)), vm.events.first())
        assertFalse(vm.state.value.busy)

        repo.importResult = null
        vm.import { ByteArrayInputStream(ByteArray(0)) }
        assertEquals(DenylistEvent.ImportFailed, vm.events.first())

        vm.import { throw java.io.FileNotFoundException() }
        assertEquals(DenylistEvent.ImportFailed, vm.events.first())
    }

    @Test
    fun blockSwitchWritesSetting() {
        val repo = FakeDenylistRepository(sample)
        val vm = DenylistViewModel(repo, dispatcher)
        vm.setBlockDenylisted(false)
        assertFalse(repo.blockDenylisted)
        assertFalse(vm.state.value.blockDenylisted)
    }
}
