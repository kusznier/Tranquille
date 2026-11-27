package fr.vinetos.tranquille.presentation.denylist

import android.text.format.DateUtils
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.data.DenylistItem
import fr.vinetos.tranquille.data.DenylistUtils
import fr.vinetos.tranquille.ui.components.TranquilleTopBar
import fr.vinetos.tranquille.ui.theme.TranquilleTheme
import java.util.Date

@Composable
fun DenylistScreen(
    state: DenylistUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateUp: () -> Unit,
    onQueryChange: (String) -> Unit,
    onBlockDenylistedChange: (Boolean) -> Unit,
    onItemClick: (DenylistItem) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Scaffold(
        topBar = {
            if (state.selectionMode) {
                SelectionTopBar(
                    count = state.selection.size,
                    onClearSelection = onClearSelection,
                    onSelectAll = onSelectAll,
                    onDelete = onDeleteSelected,
                )
            } else {
                TranquilleTopBar(
                    title = stringResource(R.string.title_denylist_activity),
                    onNavigateUp = onNavigateUp,
                    actions = { OverflowMenu(onImport = onImport, onExport = onExport) },
                )
            }
        },
        floatingActionButton = {
            if (!state.selectionMode) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(painterResource(R.drawable.ic_plus_24dp), contentDescription = null) },
                    text = { Text(stringResource(R.string.denylist_add)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "header") {
                Column {
                    if (state.busy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    BlockSwitch(state.blockDenylisted, onBlockDenylistedChange)
                    SearchField(state.query, onQueryChange)
                }
            }

            if (!state.loading && state.items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(
                            if (state.allItems.isEmpty()) R.string.denylist_empty else R.string.denylist_no_matches
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
                    )
                }
            }

            items(state.items, key = { it.id }) { item ->
                DenylistRow(
                    item = item,
                    selected = item.id in state.selection,
                    selectionMode = state.selectionMode,
                    onClick = {
                        if (state.selectionMode) onToggleSelection(item.id) else onItemClick(item)
                    },
                    onLongClick = { onToggleSelection(item.id) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
) {
    TranquilleTopBar(
        title = pluralStringResource(R.plurals.selected_count, count, count),
        onNavigateUp = null,
        actions = {
            IconButton(onClick = onClearSelection) {
                Icon(painterResource(R.drawable.ic_clear_24dp), stringResource(R.string.denylist_clear_selection))
            }
            IconButton(onClick = onSelectAll) {
                Icon(painterResource(R.drawable.ic_select_all_24dp), stringResource(R.string.denylist_select_all))
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete_24dp), stringResource(R.string.denylist_delete))
            }
        },
    )
}

@Composable
private fun OverflowMenu(onImport: () -> Unit, onExport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more_vert_24dp), stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.denylist_import)) },
                onClick = { expanded = false; onImport() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.denylist_export)) },
                onClick = { expanded = false; onExport() },
            )
        }
    }
}

@Composable
private fun BlockSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.block_denylisted)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.denylist_search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search_24dp), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_clear_24dp), stringResource(R.string.denylist_clear_search))
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .testTag("denylistSearch"),
    )
}

@Composable
private fun DenylistRow(
    item: DenylistItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pattern = DenylistUtils.patternToHumanReadable(item.pattern)
    val name = item.name?.takeIf { it.isNotEmpty() }
    val stats = if (item.numberOfCalls > 0) {
        val date = item.lastCallDate?.let { DateUtils.getRelativeTimeSpanString(it.time).toString() }
            ?: stringResource(R.string.denylist_item_date_no_info)
        pluralStringResource(
            R.plurals.denylist_item_stats, item.numberOfCalls.toInt(), item.numberOfCalls, date
        )
    } else {
        null
    }
    val invalidLabel = stringResource(R.string.number_pattern_incorrect)

    ListItem(
        headlineContent = { Text(name ?: pattern) },
        supportingContent = {
            val lines = listOfNotNull(if (name != null) pattern else null, stats)
            if (lines.isNotEmpty()) Text(lines.joinToString("\n"))
        },
        leadingContent = when {
            selectionMode -> {
                { Checkbox(checked = selected, onCheckedChange = null) }
            }
            item.invalid -> {
                {
                    Icon(
                        painterResource(R.drawable.ic_error_24dp),
                        contentDescription = invalidLabel,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            else -> null
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        modifier = if (selectionMode) {
            Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
        } else {
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = stringResource(R.string.denylist_select),
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun DenylistScreenPreview() {
    val items = listOf(
        DenylistItem(1, "Telemarketing OZE", "+48000000101", Date(), false, 4, Date()),
        DenylistItem(2, null, "+999000%", Date(), false, 0, null),
        DenylistItem(3, "Bad pattern", "+48a", Date(), true, 0, null),
    )
    TranquilleTheme(dynamicColor = false) {
        DenylistScreen(
            state = DenylistUiState(loading = false, allItems = items),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateUp = {}, onQueryChange = {}, onBlockDenylistedChange = {}, onItemClick = {},
            onToggleSelection = {}, onSelectAll = {}, onClearSelection = {}, onDeleteSelected = {},
            onAdd = {}, onImport = {}, onExport = {},
        )
    }
}
