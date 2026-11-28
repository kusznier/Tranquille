package fr.vinetos.tranquille.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.vinetos.tranquille.R
import fr.vinetos.tranquille.presentation.main.OnboardingState
import fr.vinetos.tranquille.presentation.main.OnboardingStep
import fr.vinetos.tranquille.presentation.main.ProtectionStatus
import fr.vinetos.tranquille.presentation.main.isDone
import fr.vinetos.tranquille.ui.theme.TranquilleTheme

/** What the first-run setup can ask the activity to do. */
interface OnboardingActions {
    fun requestCallScreening()
    fun requestPermissions()
    fun downloadDatabase()
    fun setAutoUpdates(enabled: Boolean)
    /** Next step, or close the setup after the last one; also used to skip a step. */
    fun next()
}

/**
 * First-run setup (U7): one step per screen, each saying why it is needed.
 * Every step can be skipped; the status card on the main screen still shows
 * whatever is missing afterwards.
 */
@Composable
fun OnboardingScreen(state: OnboardingState, status: ProtectionStatus, actions: OnboardingActions) {
    val step = state.step
    val done = status.isDone(step)
    val counter = stringResource(R.string.onboarding_step_counter, state.index + 1, state.steps.size)
    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (done) {
                    Button(onClick = actions::next) {
                        Text(stringResource(if (state.isLast) R.string.onboarding_finish else R.string.onboarding_next))
                    }
                } else {
                    TextButton(onClick = actions::next) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LinearProgressIndicator(
                progress = { (state.index + 1f) / state.steps.size },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = counter },
            )
            Text(
                counter,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    painterResource(step.icon),
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp).size(40.dp),
                )
            }
            Text(
                stringResource(step.title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(step.text), style = MaterialTheme.typography.bodyLarge)
            if (done) {
                Done()
            } else {
                StepAction(step, status, actions)
            }
            if (step == OnboardingStep.DATABASE) {
                AutoUpdatesSwitch(status.autoUpdates, actions::setAutoUpdates)
            }
        }
    }
}

private val OnboardingStep.icon: Int
    get() = when (this) {
        OnboardingStep.CALL_SCREENING -> R.drawable.ic_security_24dp
        OnboardingStep.PERMISSIONS -> R.drawable.ic_info_24dp
        OnboardingStep.DATABASE -> R.drawable.ic_file_download_24dp
    }

private val OnboardingStep.title: Int
    get() = when (this) {
        OnboardingStep.CALL_SCREENING -> R.string.onboarding_screening_title
        OnboardingStep.PERMISSIONS -> R.string.onboarding_permissions_title
        OnboardingStep.DATABASE -> R.string.onboarding_database_title
    }

private val OnboardingStep.text: Int
    get() = when (this) {
        OnboardingStep.CALL_SCREENING -> R.string.onboarding_screening_text
        OnboardingStep.PERMISSIONS -> R.string.onboarding_permissions_text
        OnboardingStep.DATABASE -> R.string.onboarding_database_text
    }

@Composable
private fun StepAction(step: OnboardingStep, status: ProtectionStatus, actions: OnboardingActions) {
    when (step) {
        OnboardingStep.CALL_SCREENING -> FilledTonalButton(onClick = actions::requestCallScreening) {
            Text(stringResource(R.string.onboarding_screening_action))
        }
        OnboardingStep.PERMISSIONS -> FilledTonalButton(onClick = actions::requestPermissions) {
            Text(stringResource(R.string.status_grant))
        }
        OnboardingStep.DATABASE -> FilledTonalButton(
            onClick = actions::downloadDatabase,
            enabled = !status.dbDownloading,
        ) {
            Text(stringResource(if (status.dbDownloading) R.string.main_db_downloading else R.string.download_main_db))
        }
    }
}

@Composable
private fun Done() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_check_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.onboarding_step_done),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AutoUpdatesSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onChange,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.auto_updates), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.auto_updates_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingScreenPreview() {
    TranquilleTheme(dynamicColor = false) {
        OnboardingScreen(
            state = OnboardingState(OnboardingStep.entries, index = 2),
            status = ProtectionStatus(dbVersion = null),
            actions = object : OnboardingActions {
                override fun requestCallScreening() {}
                override fun requestPermissions() {}
                override fun downloadDatabase() {}
                override fun setAutoUpdates(enabled: Boolean) {}
                override fun next() {}
            },
        )
    }
}
