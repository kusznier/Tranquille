package fr.vinetos.tranquille.presentation.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dummydomain.yetanothercallblocker.sia.model.NumberCategory
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.data.NumberInfo
import fr.vinetos.tranquille.data.SiaNumberCategoryUtils

/** Ratings counts with the same icons and colors as the old summary view. */
@Composable
fun RatingsSummary(negative: Int, neutral: Int, positive: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.notification_incoming_call_text_description, negative, positive, neutral)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingCount(R.drawable.ic_thumb_down_24dp, R.color.rateNegative, negative)
        RatingCount(R.drawable.ic_thumbs_up_down_24dp, R.color.rateNeutral, neutral)
        RatingCount(R.drawable.ic_thumb_up_24dp, R.color.ratePositive, positive)
    }
}

@Composable
private fun RatingCount(icon: Int, color: Int, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = colorResource(color), modifier = Modifier.size(24.dp))
        Text(count.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

/** Actions available from the number info dialog. */
interface NumberInfoActions {
    fun addToDenylist(info: NumberInfo)
    fun openOnlineReviews(info: NumberInfo)
    fun openWebReview(info: NumberInfo)
}

/**
 * Number details and actions, replacing InfoDialogHelper's AlertDialog. Online actions send
 * the number to the reviews service, so for a contact they ask for confirmation first, as before.
 */
@Composable
fun NumberInfoDialog(info: NumberInfo, actions: NumberInfoActions, onDismiss: () -> Unit) {
    var pendingOnlineAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current

    val online: (() -> Unit) -> Unit = { action ->
        if (info.contactItem != null) {
            pendingOnlineAction = action
        } else {
            action()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (info.noNumber) stringResource(R.string.no_number) else info.number) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val category = info.communityDatabaseItem?.let { NumberCategory.getById(it.category) }
                info.contactItem?.displayName?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium)
                }
                info.featuredDatabaseItem?.name?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
                if (category != null && category != NumberCategory.NONE) {
                    Text(SiaNumberCategoryUtils.getName(context, category), style = MaterialTheme.typography.titleMedium)
                }
                info.denylistItem?.let { denylisted ->
                    Text(
                        stringResource(
                            if (info.contactItem != null) R.string.info_in_denylist_contact
                            else R.string.info_in_denylist
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    denylisted.name?.takeIf { it.isNotEmpty() }?.let { Text(it) }
                }
                info.communityDatabaseItem?.let { item ->
                    if (item.positiveRatingsCount + item.negativeRatingsCount + item.neutralRatingsCount > 0) {
                        RatingsSummary(item.negativeRatingsCount, item.neutralRatingsCount, item.positiveRatingsCount)
                    }
                }
            }
        },
        confirmButton = {
            if (!info.noNumber) {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { online { actions.openWebReview(info) } }) {
                        Text(stringResource(R.string.add_web_review))
                    }
                    TextButton(onClick = { actions.addToDenylist(info); onDismiss() }) {
                        Text(stringResource(R.string.add_to_denylist))
                    }
                    TextButton(onClick = { online { actions.openOnlineReviews(info) } }) {
                        Text(stringResource(R.string.online_reviews))
                    }
                }
            }
        },
    )

    pendingOnlineAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingOnlineAction = null },
            title = { Text(stringResource(R.string.are_you_sure)) },
            text = { Text(stringResource(R.string.load_reviews_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = { pendingOnlineAction = null; action(); onDismiss() }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOnlineAction = null }) { Text(stringResource(R.string.no)) }
            },
        )
    }
}
