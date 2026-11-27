package fr.vinetos.tranquille.ui.components

import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import fr.vinetos.tranquille.ui.theme.TranquilleTheme

/**
 * Shows a Compose screen in this activity, edge to edge and in the app theme.
 * Activities stay AppCompatActivity so the app's own light/dark setting
 * (AppCompatDelegate) keeps applying to Compose screens.
 */
fun AppCompatActivity.setTranquilleContent(content: @Composable () -> Unit) {
    enableEdgeToEdge()
    setContent {
        TranquilleTheme(content = content)
    }
}
