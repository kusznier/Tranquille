package fr.vinetos.tranquille.presentation.denylist

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.ui.components.TranquilleTopBar
import fr.vinetos.tranquille.ui.theme.TranquilleTheme

/** Which notice to show about contacts, which the denylist never blocks when enabled. */
enum class ContactsNotice { NEVER_BLOCKED, NOT_ENABLED, NO_PERMISSION }

@Composable
fun EditDenylistItemScreen(
    state: EditDenylistItemUiState,
    contactsNotice: ContactsNotice,
    onNavigateUp: () -> Unit,
    onNameChange: (String) -> Unit,
    onPatternChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Scaffold(
        topBar = {
            TranquilleTopBar(
                title = stringResource(
                    if (state.isNew) R.string.title_add_denylist_item_activity
                    else R.string.title_edit_denylist_item_activity
                ),
                onNavigateUp = onNavigateUp,
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = onDelete) {
                            Icon(painterResource(R.drawable.ic_delete_24dp), stringResource(R.string.denylist_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val error = state.patternError
            OutlinedTextField(
                value = state.pattern,
                onValueChange = onPatternChange,
                label = { Text(stringResource(R.string.denylist_pattern_label)) },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    Text(
                        when (error) {
                            PatternError.EMPTY -> stringResource(R.string.number_pattern_empty)
                            PatternError.INCORRECT -> stringResource(R.string.number_pattern_incorrect)
                            null -> stringResource(R.string.number_pattern_hint)
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.denylist_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier.fillMaxWidth(),
            )

            state.item?.let { item -> Stats(item.numberOfCalls, item.lastCallDate) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (contactsNotice == ContactsNotice.NEVER_BLOCKED)
                        MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = stringResource(
                        when (contactsNotice) {
                            ContactsNotice.NEVER_BLOCKED -> R.string.contacts_are_not_blocked_denylist_notice
                            ContactsNotice.NOT_ENABLED -> R.string.contacts_are_not_blocked_not_enabled
                            ContactsNotice.NO_PERMISSION -> R.string.contacts_are_not_blocked_no_permission
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Button(onClick = onSave, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun Stats(numberOfCalls: Long, lastCallDate: java.util.Date?) {
    val context = LocalContext.current
    val text = if (numberOfCalls > 0) {
        val date = lastCallDate?.let {
            DateFormat.getMediumDateFormat(context).format(it) + ' ' + DateFormat.getTimeFormat(context).format(it)
        } ?: stringResource(R.string.denylist_item_date_no_info)
        pluralStringResource(R.plurals.denylist_item_stats, numberOfCalls.toInt(), numberOfCalls, date)
    } else {
        stringResource(R.string.denylist_item_no_calls)
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview(showBackground = true)
@Composable
private fun EditDenylistItemScreenPreview() {
    TranquilleTheme(dynamicColor = false) {
        EditDenylistItemScreen(
            state = EditDenylistItemUiState(name = "Telemarketing", pattern = "+48000000*"),
            contactsNotice = ContactsNotice.NEVER_BLOCKED,
            onNavigateUp = {}, onNameChange = {}, onPatternChange = {}, onSave = {}, onDelete = {},
        )
    }
}
