package fr.vinetos.tranquille.presentation.about

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.ui.components.TranquilleTopBar
import fr.vinetos.tranquille.ui.theme.TranquilleTheme

@Composable
fun AboutScreen(
    state: AboutUiState,
    onNavigateUp: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenDbManagement: () -> Unit,
) {
    Scaffold(
        topBar = {
            TranquilleTopBar(
                title = stringResource(R.string.about_activity),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()
            Links()
            DbCard(state, onCheckForUpdates, onOpenDbManagement)
            Credits()
            Text(
                text = stringResource(R.string.license),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.version_string, state.versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(132.dp),
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.motto),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Links() {
    LinkCard(
        listOf(
            R.string.homepage to R.string.url_repo,
            R.string.faq to R.string.url_faq,
            R.string.issues to R.string.url_issues,
        )
    )
}

/** Credits for the projects this fork is based on (AGPL-3.0). */
@Composable
private fun Credits() {
    Text(
        text = stringResource(R.string.about_based_on),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
    LinkCard(
        listOf(
            R.string.about_upstream_tranquille to R.string.url_upstream_tranquille,
            R.string.about_upstream_yacb to R.string.url_upstream_yacb,
        )
    )
}

@Composable
private fun LinkCard(links: List<Pair<Int, Int>>) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        links.forEach { (label, url) ->
            val urlString = stringResource(url)
            ListItem(
                headlineContent = { Text(stringResource(label)) },
                supportingContent = { Text(urlString, maxLines = 1) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.clickable(role = Role.Button) { uriHandler.openUri(urlString) },
            )
        }
    }
}

@Composable
private fun DbCard(
    state: AboutUiState,
    onCheckForUpdates: () -> Unit,
    onOpenDbManagement: () -> Unit,
) {
    val version = state.dbVersion?.toString() ?: stringResource(R.string.db_version_not_available)
    val lastCheck = when {
        state.checkingForUpdates -> stringResource(R.string.db_last_update_check_checking)
        state.lastUpdateCheckTime == 0L -> stringResource(R.string.db_last_update_check_never)
        else -> DateUtils.getRelativeTimeSpanString(state.lastUpdateCheckTime).toString()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.db_version, version), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.db_last_update_check, lastCheck), style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onCheckForUpdates, enabled = state.canCheckForUpdates) {
                    Text(stringResource(R.string.check_for_db_updates))
                }
                OutlinedButton(onClick = onOpenDbManagement) {
                    Text(stringResource(R.string.db_management_activity_label))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutScreenPreview() {
    TranquilleTheme(dynamicColor = false) {
        AboutScreen(
            state = AboutUiState(versionName = "0.1.0", dbVersion = 4213, lastUpdateCheckTime = 0),
            onNavigateUp = {},
            onCheckForUpdates = {},
            onOpenDbManagement = {},
        )
    }
}

