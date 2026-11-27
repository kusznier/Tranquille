package fr.vinetos.tranquille.presentation.dbmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.ui.components.TranquilleTopBar

@Composable
fun DbManagementScreen(
    state: DbManagementUiState,
    onNavigateUp: () -> Unit,
    onInfo: () -> Unit,
    onUpdate: () -> Unit,
    onAskReset: (PendingReset) -> Unit,
    onConfirmReset: () -> Unit,
    onCancelReset: () -> Unit,
) {
    Scaffold(
        topBar = { TranquilleTopBar(stringResource(R.string.db_management_activity_label), onNavigateUp = onNavigateUp) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onInfo) { Text(stringResource(R.string.db_management_db_info)) }
                Button(onClick = onUpdate) { Text(stringResource(R.string.db_management_update_db)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onAskReset(PendingReset.ALL) }) {
                    Text(stringResource(R.string.db_management_reset_base))
                }
                OutlinedButton(onClick = { onAskReset(PendingReset.UPDATES) }) {
                    Text(stringResource(R.string.db_management_reset_secondary))
                }
            }
            state.message?.let { message ->
                val text = when (message) {
                    is DbMessage.Info -> message.text
                    is DbMessage.Updated -> stringResource(R.string.db_management_update_result, message.version)
                    DbMessage.DatabaseRemoved -> stringResource(R.string.db_management_db_removed)
                    DbMessage.UpdatesRemoved -> stringResource(R.string.db_management_updates_removed)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    SelectionContainer {
                        Text(
                            text,
                            fontFamily = if (message is DbMessage.Info) FontFamily.Monospace else null,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    state.pendingReset?.let { reset ->
        AlertDialog(
            onDismissRequest = onCancelReset,
            title = { Text(stringResource(R.string.are_you_sure)) },
            text = {
                Text(
                    stringResource(
                        if (reset == PendingReset.ALL) R.string.db_management_reset_base_confirmation
                        else R.string.db_management_reset_secondary_confirmation
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmReset,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(
                        stringResource(
                            if (reset == PendingReset.ALL) R.string.db_management_reset_base
                            else R.string.db_management_reset_secondary
                        )
                    )
                }
            },
            dismissButton = { TextButton(onClick = onCancelReset) { Text(stringResource(R.string.no)) } },
        )
    }
}
