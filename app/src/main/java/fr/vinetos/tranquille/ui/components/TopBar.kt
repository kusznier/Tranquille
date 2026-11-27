package fr.vinetos.tranquille.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import fr.vinetos.tranquille.R

/** Top app bar with an optional "navigate up" button. */
@Composable
fun TranquilleTopBar(
    title: String,
    onNavigateUp: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back_24dp),
                        contentDescription = stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
                    )
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}
