package nl.potentplum.mise.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.HeroHeader
import nl.potentplum.mise.core.designsystem.styledStringResource
import nl.potentplum.mise.feature.settings.BackupSection

/** How first run ends (spec §6.16 step 4): in an import flow, or on Vandaag. */
enum class FirstRecipeChoice { LINK, PHOTOS, LATER }

@Composable
fun OnboardingScreen(
    onAddKey: () -> Unit,
    onDone: (FirstRecipeChoice) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val hasKey by viewModel.hasKey.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LinearProgressIndicator(
                progress = { (step.ordinal + 1f) / OnboardingStep.entries.size },
                modifier = Modifier.fillMaxWidth(),
                drawStopIndicator = {},
            )
            when (step) {
                OnboardingStep.WELCOME -> {
                    HeroHeader(
                        title = styledStringResource(R.string.onboarding_welcome_title),
                        eyebrow = stringResource(R.string.brand_sub),
                    )
                    Text(stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
                    Button(
                        onClick = viewModel::next,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.onboarding_start)) }
                }

                OnboardingStep.KEY -> {
                    Title(stringResource(R.string.onboarding_key_title))
                    Text(stringResource(R.string.key_intro), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.key_storage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiseTheme.colors.inkSoft,
                    )
                    if (hasKey) {
                        Text(
                            stringResource(R.string.key_saved),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Button(
                            onClick = viewModel::next,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.onboarding_next)) }
                    } else {
                        Button(onClick = onAddKey, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_key_add)) }
                        TextButton(
                            onClick = viewModel::next,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.onboarding_later)) }
                    }
                }

                OnboardingStep.RESTORE -> {
                    Title(stringResource(R.string.onboarding_restore_title))
                    Text(stringResource(R.string.onboarding_restore_body), style = MaterialTheme.typography.bodyLarge)
                    BackupSection(
                        onRestored = { viewModel.restored { onDone(FirstRecipeChoice.LATER) } },
                        onEnrich = null,
                        showExport = false,
                    )
                    TextButton(
                        onClick = viewModel::next,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.onboarding_skip)) }
                }

                OnboardingStep.FIRST_RECIPE -> {
                    Title(stringResource(R.string.onboarding_first_recipe_title))
                    Text(stringResource(R.string.onboarding_first_recipe_sub), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { viewModel.finish { onDone(FirstRecipeChoice.LINK) } }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.recipes_add_link))
                    }
                    OutlinedButton(
                        onClick = { viewModel.finish { onDone(FirstRecipeChoice.PHOTOS) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.recipes_add_camera))
                    }
                    TextButton(onClick = { viewModel.finish { onDone(FirstRecipeChoice.LATER) } }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_later))
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
}
