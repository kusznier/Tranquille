package fr.vinetos.tranquille.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {

    private class FakeStore : SettingsStore {
        var state = SettingsUiState()
        val changes = mutableListOf<SettingsChange>()
        override fun load() = state
        override fun write(change: SettingsChange) {
            changes += change
            if (change is SettingsChange.BlockHiddenNumbers) state = state.copy(blockHiddenNumbers = change.value)
        }
    }

    @Test
    fun changeIsWrittenAndStateReloaded() {
        val store = FakeStore()
        val vm = SettingsViewModel(store)
        vm.change(SettingsChange.BlockHiddenNumbers(true))
        assertEquals(listOf(SettingsChange.BlockHiddenNumbers(true)), store.changes)
        assertTrue(vm.state.value.blockHiddenNumbers)
    }

    @Test
    fun reloadPicksUpExternalChanges() {
        val store = FakeStore()
        val vm = SettingsViewModel(store)
        store.state = store.state.copy(callScreeningHeld = true)
        vm.reload()
        assertTrue(vm.state.value.callScreeningHeld)
    }

    @Test
    fun countryCodeValidation() {
        assertTrue(SettingsViewModel.isValidCountryCode(""))
        assertTrue(SettingsViewModel.isValidCountryCode("PL"))
        assertTrue(SettingsViewModel.isValidCountryCode("pl"))
        assertFalse(SettingsViewModel.isValidCountryCode("POL"))
        assertFalse(SettingsViewModel.isValidCountryCode("4"))
    }

    @Test
    fun onlineReviewsForBlockedCallsAreOffByDefault() {
        assertFalse(SettingsUiState().reviewsForBlockedCalls)
    }
}
