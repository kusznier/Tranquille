package fr.vinetos.tranquille.presentation.lookup

import dummydomain.yetanothercallblocker.sia.model.database.CommunityDatabaseItem
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
class LookupViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val queried = mutableListOf<String>()
    private val lookup = NumberLookup { number ->
        queried += number
        (if (number == "99900000101") CommunityDatabaseItem().apply { negativeRatingsCount = 5 } else null) to null
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun cleansNumberAndSearches() {
        val vm = LookupViewModel(lookup, { "PL" }, ioDispatcher = dispatcher)
        vm.onInputChange("+999 000-00 101")
        vm.search()
        assertEquals(listOf("99900000101"), queried)
        assertEquals(5, vm.state.value.result!!.community!!.negativeRatingsCount)
    }

    @Test
    fun russianLeadingEightBecomesSeven() {
        val vm = LookupViewModel(lookup, { "RU" }, ioDispatcher = dispatcher)
        vm.onInputChange("8 999 123-45-67")
        assertEquals("79991234567", vm.pureNumber())
        val other = LookupViewModel(lookup, { "PL" }, ioDispatcher = dispatcher)
        other.onInputChange("8 999 123-45-67")
        assertEquals("89991234567", other.pureNumber())
    }

    @Test
    fun emptyInputShowsError() {
        val vm = LookupViewModel(lookup, { null }, ioDispatcher = dispatcher)
        vm.onInputChange("abc")
        assertFalse(vm.requireNumber())
        assertTrue(vm.state.value.showNotANumber)
        vm.search()
        assertTrue(queried.isEmpty())
    }

    @Test
    fun numberFromClipboardIsLookedUpRightAway() {
        LookupViewModel(lookup, { null }, initialInput = "+99900000101", ioDispatcher = dispatcher)
        assertEquals(listOf("99900000101"), queried)
    }

    @Test
    fun clearResetsEverything() {
        val vm = LookupViewModel(lookup, { null }, initialInput = "123", ioDispatcher = dispatcher)
        vm.clear()
        assertEquals("", vm.state.value.input)
        assertNull(vm.state.value.result)
    }
}
