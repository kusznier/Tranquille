package fr.vinetos.tranquille.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import fr.vinetos.tranquille.R

/** Top-level destinations on the bottom navigation bar (U8). */
enum class MainTab(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    CALLS(R.string.nav_calls, R.drawable.ic_history_24dp),
    DENYLIST(R.string.title_denylist_activity, R.drawable.ic_block_24dp),
    SETTINGS(R.string.title_settings_activity, R.drawable.ic_settings_24dp),
}

/**
 * The bottom navigation bar around the selected tab. Each tab keeps its own
 * Scaffold and top bar; [content] gets a modifier that keeps it above the bar.
 */
@Composable
fun MainTabs(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    content: @Composable (MainTab, Modifier) -> Unit,
) {
    Scaffold(
        // Tabs handle the status bar in their own top bars; the bar handles the navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { onSelect(tab) },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        content(selected, Modifier.padding(padding).consumeWindowInsets(padding))
    }
}
