package fr.vinetos.tranquille.presentation.denylist

import fr.vinetos.tranquille.data.DenylistItem
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
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class EditDenylistItemViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: DenylistRepository, id: Long? = null, name: String? = null, pattern: String? = null) =
        EditDenylistItemViewModel(repo, id, name, pattern, dispatcher)

    @Test
    fun addsNewItemWithoutName() {
        val repo = FakeDenylistRepository()
        val vm = vm(repo)
        assertNull(vm.state.value.patternError)

        vm.onPatternChange("+999 000 *")
        vm.save()

        assertTrue(vm.state.value.finished)
        val saved = repo.items.single()
        assertEquals("", saved.name)
        assertEquals("+999000%", saved.pattern)
    }

    @Test
    fun emptyOrIncorrectPatternIsNotSaved() {
        val repo = FakeDenylistRepository()
        val vm = vm(repo)

        vm.save()
        assertEquals(PatternError.EMPTY, vm.state.value.patternError)
        assertFalse(vm.state.value.finished)

        vm.onPatternChange("abc")
        assertEquals(PatternError.INCORRECT, vm.state.value.patternError)
        vm.save()
        assertFalse(vm.state.value.finished)
        assertTrue(repo.items.isEmpty())
    }

    @Test
    fun prefillsFromNumberAndSkipsExactDuplicate() {
        val repo = FakeDenylistRepository(listOf(DenylistItem(7, "Spam", "+99912345", Date(), false, 0, null)))
        val vm = vm(repo, name = "Spam", pattern = "+99912345")
        assertEquals("+99912345", vm.state.value.pattern)

        vm.save()
        assertTrue(vm.state.value.finished)
        assertEquals(1, repo.items.size)
    }

    @Test
    fun editsExistingItemOnlyWhenChanged() {
        val existing = DenylistItem(4, "Old", "+999000%", Date(5), false, 9, Date(6))
        val repo = FakeDenylistRepository(listOf(existing))

        val unchanged = vm(repo, id = 4)
        assertEquals("+999000*", unchanged.state.value.pattern)
        unchanged.save()
        assertTrue(repo.updates.isEmpty())

        val edited = vm(repo, id = 4)
        edited.onNameChange("New")
        edited.save()
        val updated = repo.updates.single()
        assertEquals("New", updated.name)
        assertEquals(9L, updated.numberOfCalls)
        assertEquals(4L, updated.id)
    }

    @Test
    fun deletesExistingItemAndReportsMissingOnes() {
        val repo = FakeDenylistRepository(listOf(DenylistItem(4, "X", "+999", Date(), false, 0, null)))
        val vm = vm(repo, id = 4)
        vm.delete()
        assertTrue(vm.state.value.finished)
        assertTrue(repo.items.isEmpty())

        assertTrue(vm(repo, id = 99).state.value.missing)
    }
}
