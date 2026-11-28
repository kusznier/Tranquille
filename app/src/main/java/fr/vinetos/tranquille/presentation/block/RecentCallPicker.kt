package fr.vinetos.tranquille.presentation.block

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.vinetos.tranquille.CallLogRow
import fr.vinetos.tranquille.R

/** Recent calls that can be blocked: one row per number, newest first, no hidden numbers. */
fun blockableCalls(rows: List<CallLogRow>, limit: Int = 50): List<CallLogRow> =
    rows.filter { !it.numberInfo.noNumber && !it.numberInfo.number.isNullOrEmpty() }
        .distinctBy { it.numberInfo.number }
        .take(limit)

/**
 * First step of "Block a number": pick one of the recent calls, or enter a number
 * by hand. Picking a call leads to [BlockNumberDialog].
 */
@Composable
fun RecentCallPicker(
    rows: List<CallLogRow>,
    onPick: (CallLogRow) -> Unit,
    onEnterManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    val calls = blockableCalls(rows)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.block_a_number)) },
        text = {
            if (calls.isEmpty()) {
                Text(stringResource(R.string.no_recent_calls))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(calls, key = { it.key }) { row -> PickerRow(row, onClick = { onPick(row) }) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEnterManually) { Text(stringResource(R.string.block_number_enter_manually)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun PickerRow(row: CallLogRow, onClick: () -> Unit) {
    val number = row.numberInfo.number
    val time = DateUtils.getRelativeTimeSpanString(
        row.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL,
    ).toString()
    ListItem(
        headlineContent = { Text(row.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val details = listOfNotNull(number.takeIf { it != row.label }, row.description, time)
            Text(details.joinToString(" · "), maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (row.ratingIcon != null) {
                    Icon(
                        painterResource(row.ratingIcon),
                        contentDescription = row.description,
                        tint = row.ratingColor?.let { colorResource(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
