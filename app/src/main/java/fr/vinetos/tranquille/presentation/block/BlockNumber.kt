package fr.vinetos.tranquille.presentation.block

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dummydomain.yetanothercallblocker.sia.model.NumberCategory
import fr.vinetos.tranquille.NumberInfoUtils
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.data.DenylistItem
import fr.vinetos.tranquille.data.DenylistUtils
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.data.SiaNumberCategoryUtils
import fr.vinetos.tranquille.presentation.denylist.DenylistRepository
import java.util.Date

/** What the block confirmation shows about a number. */
data class BlockCandidate(
    val number: String,
    /** Contact or database name, if different from the number. */
    val label: String?,
    /** Category and ratings, e.g. "Telemarketer · 👎 5 · 👍 1". */
    val summary: String?,
    /** Pre-filled description: the database name or the category. */
    val suggestedDescription: String,
) {
    companion object {
        fun from(context: Context, info: NumberInfo): BlockCandidate {
            val category = info.communityDatabaseItem
                ?.let { NumberCategory.getById(it.category) }
                ?.takeIf { it != NumberCategory.NONE }
                ?.let { SiaNumberCategoryUtils.getName(context, it) }
            val summary = listOfNotNull(
                NumberInfoUtils.getShortDescription(context, info),
                NumberInfoUtils.getShortRatings(context, info),
            ).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { null }
            return BlockCandidate(
                number = info.number,
                label = info.name?.takeIf { it != info.number },
                summary = summary,
                suggestedDescription = info.featuredDatabaseItem?.name?.takeIf { it.isNotBlank() }
                    ?: category.orEmpty(),
            )
        }
    }
}

enum class BlockResult { ADDED, ALREADY_ON_DENYLIST, INVALID_NUMBER }

/** Adds one exact number to the denylist; blocks, call it off the main thread. */
class NumberBlocker(private val repository: DenylistRepository) {

    fun block(number: String, description: String): BlockResult {
        val pattern = DenylistUtils.cleanNumber(number)
        if (pattern.isEmpty() || !DenylistUtils.isValidPattern(pattern)) return BlockResult.INVALID_NUMBER
        if (repository.getAll().any { it.pattern == pattern }) return BlockResult.ALREADY_ON_DENYLIST
        repository.insert(
            DenylistItem(
                id = -1L,
                name = description.trim(),
                pattern = pattern,
                creationDate = Date(),
                invalid = false,
                numberOfCalls = 0,
                lastCallDate = null,
            )
        )
        return BlockResult.ADDED
    }
}

/** Confirmation before blocking a number, with an optional, pre-filled description. */
@Composable
fun BlockNumberDialog(
    candidate: BlockCandidate,
    onConfirm: (description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var description by rememberSaveable(candidate.number) { mutableStateOf(candidate.suggestedDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.block_number_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(candidate.label ?: candidate.number, style = MaterialTheme.typography.titleMedium)
                    if (candidate.label != null) {
                        Text(candidate.number, style = MaterialTheme.typography.bodyMedium)
                    }
                    candidate.summary?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.block_number_description)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(description) }) {
                Text(stringResource(R.string.block_number_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
