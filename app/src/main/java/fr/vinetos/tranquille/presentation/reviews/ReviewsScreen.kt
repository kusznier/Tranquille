package fr.vinetos.tranquille.presentation.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dummydomain.yetanothercallblocker.sia.model.CommunityReview
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.data.SiaNumberCategoryUtils
import fr.vinetos.tranquille.presentation.info.RatingsSummary
import fr.vinetos.tranquille.ui.components.TranquilleTopBar

@Composable
fun ReviewsScreen(number: String, state: ReviewsUiState, onNavigateUp: () -> Unit) {
    Scaffold(topBar = { TranquilleTopBar(title = number, onNavigateUp = onNavigateUp) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            when (state) {
                ReviewsUiState.Loading -> item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.reviews_loading))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                ReviewsUiState.Error -> item {
                    Text(
                        stringResource(R.string.reviews_loading_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                is ReviewsUiState.Loaded -> {
                    item {
                        RatingsSummary(state.negative, state.neutral, state.positive, Modifier.padding(16.dp))
                    }
                    items(state.reviews, key = { it.id }) { review ->
                        ReviewRow(review)
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(review: CommunityReview) {
    val context = LocalContext.current
    val (icon, color) = when (review.rating) {
        CommunityReview.Rating.POSITIVE -> R.drawable.ic_thumb_up_24dp to R.color.ratePositive
        CommunityReview.Rating.NEUTRAL -> R.drawable.ic_thumbs_up_down_24dp to R.color.rateNeutral
        CommunityReview.Rating.NEGATIVE -> R.drawable.ic_thumb_down_24dp to R.color.rateNegative
        else -> R.drawable.ic_question_mark_24dp to R.color.notFound
    }
    val category = review.category?.let { SiaNumberCategoryUtils.getName(context, it) }
    ListItem(
        headlineContent = { Text(category ?: review.title.orEmpty()) },
        supportingContent = {
            val lines = listOfNotNull(
                review.title?.takeIf { it.isNotEmpty() && category != null },
                review.comment?.takeIf { it.isNotEmpty() },
            )
            if (lines.isNotEmpty()) Text(lines.joinToString("\n"))
        },
        leadingContent = {
            Icon(painterResource(icon), contentDescription = null, tint = colorResource(color), modifier = Modifier.size(28.dp))
        },
    )
}
