package fr.vinetos.tranquille.presentation.lookup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dummydomain.yetanothercallblocker.sia.model.NumberCategory
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.data.SiaNumberCategoryUtils
import fr.vinetos.tranquille.presentation.info.RatingsSummary
import fr.vinetos.tranquille.ui.components.TranquilleTopBar

@Composable
fun LookupScreen(
    state: LookupUiState,
    onNavigateUp: () -> Unit,
    onInputChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onLoadReviews: () -> Unit,
    onAddWebReview: () -> Unit,
) {
    Scaffold(
        topBar = { TranquilleTopBar(stringResource(R.string.title_lookup_number), onNavigateUp = onNavigateUp) },
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
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                label = { Text(stringResource(R.string.lookup_hint_input_number)) },
                singleLine = true,
                isError = state.showNotANumber,
                supportingText = if (state.showNotANumber) {
                    { Text(stringResource(R.string.lookup_error_not_a_number)) }
                } else null,
                trailingIcon = {
                    if (state.input.isEmpty()) {
                        IconButton(onClick = onPaste) {
                            Icon(painterResource(R.drawable.ic_menu_paste_material), stringResource(R.string.lookup_paste_number))
                        }
                    } else {
                        IconButton(onClick = onClear) {
                            Icon(painterResource(R.drawable.ic_clear_24dp), stringResource(R.string.lookup_clear_number))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.lookup_query_db))
            }
            if (state.searching) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.result?.let { ResultCard(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLoadReviews) { Text(stringResource(R.string.lookup_load_reviews)) }
                OutlinedButton(onClick = onAddWebReview) { Text(stringResource(R.string.add_web_review)) }
            }
        }
    }
}

@Composable
private fun ResultCard(result: LookupResult) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val item = result.community
            if (item == null) {
                Text(stringResource(R.string.lookup_number_not_found), style = MaterialTheme.typography.titleMedium)
            } else {
                Text("+" + item.number, style = MaterialTheme.typography.titleLarge)
                val category = NumberCategory.getById(item.category)
                Text(
                    if (category != null) SiaNumberCategoryUtils.getName(context, category)
                    else stringResource(R.string.lookup_res_category, item.category),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (item.positiveRatingsCount + item.negativeRatingsCount + item.neutralRatingsCount > 0) {
                    RatingsSummary(item.negativeRatingsCount, item.neutralRatingsCount, item.positiveRatingsCount)
                }
            }
            result.featured?.let {
                Text(stringResource(R.string.lookup_res_featured_name, it.name), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
