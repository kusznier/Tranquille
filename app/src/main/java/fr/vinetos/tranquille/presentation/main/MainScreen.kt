package fr.vinetos.tranquille.presentation.main

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.vinetos.tranquille.CallLogRow
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.data.CallLogItem
import fr.vinetos.tranquille.ui.components.TranquilleTopBar

/** Actions the main screen can ask the activity to perform. */
interface MainActions {
    fun lookupNumber()
    /** Opens the settings tab (from the "blocking is off" status card). */
    fun openSettings()
    fun openAbout()
    fun blockNumber()
    fun openCall(row: CallLogRow)
    fun downloadDatabase()
    fun requestCallScreening()
    fun openNotificationSettings()
    fun requestCallLogPermission()
    fun setBlockByRating(block: Boolean)
}

@Composable
fun MainScreen(state: MainUiState, actions: MainActions, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TranquilleTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = actions::lookupNumber) {
                        Icon(painterResource(R.drawable.ic_search_24dp), stringResource(R.string.title_lookup_number))
                    }
                    MainMenu(actions)
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = actions::blockNumber,
                icon = { Icon(painterResource(R.drawable.ic_plus_24dp), contentDescription = null) },
                text = { Text(stringResource(R.string.block_a_number)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "status") {
                StatusCard(state.status, actions, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            item(key = "header") {
                Text(
                    text = stringResource(R.string.recent_calls),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)
                        .semantics { heading() },
                )
            }
            when {
                !state.status.hasCallLogPermission -> item(key = "permission") {
                    CallLogPermission(actions::requestCallLogPermission)
                }
                !state.loading && state.callLog.isEmpty() -> item(key = "empty") {
                    Text(
                        text = stringResource(R.string.no_recent_calls),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
                else -> items(state.callLog, key = { it.key }) { row ->
                    CallLogRowItem(row, onClick = { actions.openCall(row) })
                }
            }
        }
    }
}

@Composable
private fun MainMenu(actions: MainActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more_vert_24dp), stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.open_about_activity)) },
                onClick = { expanded = false; actions.openAbout() },
            )
        }
    }
}

@Composable
fun StatusCard(status: ProtectionStatus, actions: MainActions, modifier: Modifier = Modifier) {
    val problem = status.problem
    val colors = if (problem == null) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = colors) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (problem == null) ProtectionOn(status, actions) else ProblemContent(problem, status, actions)
        }
    }
}

@Composable
private fun ProtectionOn(status: ProtectionStatus, actions: MainActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusIcon(R.drawable.ic_security_24dp, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                stringResource(R.string.status_protection_on),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            val updated = if (status.lastUpdateTime > 0) {
                stringResource(
                    R.string.status_db_updated,
                    DateUtils.getRelativeTimeSpanString(status.lastUpdateTime).toString(),
                )
            } else {
                stringResource(R.string.status_db_never_updated)
            }
            Text(
                stringResource(R.string.status_db_version, status.dbVersion ?: 0) + " · " + updated,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                pluralStringResource(R.plurals.status_denylist_count, status.denylistCount, status.denylistCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().toggleable(
            value = status.blockByRating,
            role = Role.Switch,
            onValueChange = actions::setBlockByRating,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.block_negative_sia),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = status.blockByRating, onCheckedChange = null)
        }
    }
}

@Composable
private fun ProblemContent(problem: StatusProblem, status: ProtectionStatus, actions: MainActions) {
    val (title, text) = when (problem) {
        StatusProblem.NO_DATABASE -> R.string.no_main_db_title to R.string.no_main_db_text
        StatusProblem.SCREENING_OFF -> R.string.status_screening_off_title to R.string.status_screening_off_text
        StatusProblem.BLOCKING_OFF -> R.string.status_blocking_off_title to R.string.status_blocking_off_text
        StatusProblem.NOTIFICATIONS_OFF -> R.string.status_notifications_off_title to R.string.status_notifications_off_text
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusIcon(R.drawable.ic_error_24dp, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
        Spacer(Modifier.width(16.dp))
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
    Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    )
    when (problem) {
        StatusProblem.NO_DATABASE -> Button(onClick = actions::downloadDatabase, enabled = !status.dbDownloading, colors = buttonColors) {
            Text(stringResource(if (status.dbDownloading) R.string.main_db_downloading else R.string.download_main_db))
        }
        StatusProblem.SCREENING_OFF -> Button(onClick = actions::requestCallScreening, colors = buttonColors) {
            Text(stringResource(R.string.status_turn_on))
        }
        StatusProblem.BLOCKING_OFF -> Button(onClick = actions::openSettings, colors = buttonColors) {
            Text(stringResource(R.string.open_settings_activity))
        }
        StatusProblem.NOTIFICATIONS_OFF -> Button(onClick = actions::openNotificationSettings, colors = buttonColors) {
            Text(stringResource(R.string.status_turn_on))
        }
    }
}

@Composable
private fun StatusIcon(icon: Int, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = container, contentColor = content) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.padding(12.dp).size(24.dp))
    }
}

@Composable
private fun CallLogPermission(onAllow: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.call_log_permission_message), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAllow) { Text(stringResource(R.string.status_grant)) }
    }
}

@Composable
private fun CallLogRowItem(row: CallLogRow, onClick: () -> Unit) {
    val time = DateUtils.getRelativeTimeSpanString(
        row.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL,
    ).toString()
    ListItem(
        headlineContent = { Text(row.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.types.forEach { type -> CallTypeIcon(type) }
                val parts = listOfNotNull(
                    if (row.count > 3) "(${row.count})" else null,
                    row.description,
                    row.duration,
                )
                if (parts.isNotEmpty()) {
                    Text(parts.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        leadingContent = {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (row.ratingIcon != null) {
                    Icon(
                        painterResource(row.ratingIcon),
                        contentDescription = row.description,
                        tint = row.ratingColor?.let { colorResource(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = { Text(time, style = MaterialTheme.typography.labelMedium) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun CallTypeIcon(type: CallLogItem.Type) {
    val (icon, label) = when (type) {
        CallLogItem.Type.INCOMING -> R.drawable.ic_call_received_24dp to R.string.call_type_incoming
        CallLogItem.Type.OUTGOING -> R.drawable.ic_call_made_24dp to R.string.call_type_outgoing
        CallLogItem.Type.MISSED -> R.drawable.ic_call_missed_24dp to R.string.call_type_missed
        CallLogItem.Type.REJECTED -> R.drawable.ic_call_rejected_24dp to R.string.call_type_rejected
        CallLogItem.Type.OTHER -> return
    }
    // The drawables carry their own colors (as in the old list), so keep them untinted.
    Icon(
        painterResource(icon),
        contentDescription = stringResource(label),
        tint = androidx.compose.ui.graphics.Color.Unspecified,
        modifier = Modifier.size(16.dp),
    )
}
