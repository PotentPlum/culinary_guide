package nl.potentplum.mise.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.ErrorState
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.ingest.RecipeImportError

/** "Link plakken" and shared links (spec §6.9). */
@Composable
fun ImportLinkScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onEdit: (draftId: String, highlightMissing: Boolean) -> Unit,
    onOpenExisting: (String) -> Unit,
    onManual: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: ImportLinkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ImportEvent.Saved -> onSaved(event.id)
                is ImportEvent.Edit -> onEdit(event.draftId, event.highlightMissing)
                is ImportEvent.OpenExisting -> onOpenExisting(event.id)
            }
        }
    }
    val preview = state.phase as? ImportPhase.Preview
    Scaffold(
        topBar = {
            MiseTopAppBar(
                title = preview?.draft?.title ?: stringResource(R.string.import_title),
                onOverflowItem = onOverflowItem,
                onBack = onBack,
            )
        },
        bottomBar = {
            if (preview != null || state.phase == ImportPhase.Saving) {
                ImportPreviewBar(enabled = preview != null, onSave = viewModel::save, onEdit = viewModel::edit)
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val phase = state.phase) {
                is ImportPhase.Preview -> {
                    ImportPreview(phase.draft, phase.ingredients)
                }

                ImportPhase.Saving -> {
                    Unit
                }

                else -> {
                    LinkForm(state, viewModel, onManual)
                }
            }
        }
    }
}

@Composable
private fun LinkForm(
    state: ImportUiState,
    viewModel: ImportLinkViewModel,
    onManual: () -> Unit,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.recipes_page_subtitle), style = MaterialTheme.typography.bodyMedium, color = MiseTheme.colors.inkSoft)
        OutlinedTextField(
            value = state.url,
            onValueChange = viewModel::setUrl,
            label = { Text(stringResource(R.string.recipes_input_label)) },
            placeholder = { Text(stringResource(R.string.recipes_input_placeholder)) },
            singleLine = true,
            enabled = state.phase != ImportPhase.Loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { viewModel.load() }),
            modifier = Modifier.fillMaxWidth(),
        )
        when (val phase = state.phase) {
            ImportPhase.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.recipes_loading_msg), modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::cancel) { Text(stringResource(R.string.common_cancel)) }
                }
            }

            is ImportPhase.Failed -> {
                ErrorState(message = importErrorMessage(phase.error), onRetry = viewModel::load)
                // No JSON-LD: enter it by hand (the Claude page fallback is P2, spec §8.5).
                if (phase.error == RecipeImportError.NoRecipe) {
                    OutlinedButton(
                        onClick = onManual,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.recipes_add_manual)) }
                }
            }

            is ImportPhase.Duplicate -> {
                EmptyState(title = stringResource(R.string.import_duplicate_title), body = phase.draft.title) {
                    Button(onClick = viewModel::openExisting) { Text(stringResource(R.string.import_duplicate_open)) }
                    TextButton(onClick = viewModel::importCopy) { Text(stringResource(R.string.import_duplicate_copy)) }
                }
            }

            else -> {
                Button(onClick = viewModel::load, enabled = state.url.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recipes_submit_btn))
                }
                Text(
                    stringResource(R.string.recipes_hint_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                )
            }
        }
    }
}

@Composable
fun importErrorMessage(error: RecipeImportError): String =
    when (error) {
        RecipeImportError.InvalidUrl -> stringResource(R.string.recipes_error_invalid_url)
        RecipeImportError.Unreachable -> stringResource(R.string.recipes_error_network)
        is RecipeImportError.Http -> stringResource(R.string.recipes_error_http, error.code)
        RecipeImportError.TooLarge -> stringResource(R.string.recipes_error_too_large)
        RecipeImportError.NoRecipe -> stringResource(R.string.recipes_error_no_recipe)
    }

/** Bewaren (primary) and Aanpassen under an import preview (spec §6.9). */
@Composable
fun ImportPreviewBar(
    enabled: Boolean,
    onSave: () -> Unit,
    onEdit: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onSave, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.recipes_save_btn))
                }
                OutlinedButton(onClick = onEdit, enabled = enabled) { Text(stringResource(R.string.import_preview_edit)) }
            }
        }
    }
}

/** The detail screen's read view for a draft; unparsed lines are marked. */
@Composable
fun ImportPreview(
    draft: Recipe,
    ingredients: List<IngredientGroupLines>,
) {
    RecipeBody(
        state = RecipeDetailUiState(loading = false, recipe = draft, servings = draft.servings, ingredients = ingredients),
        recipe = draft,
        onServings = null,
        onToggle = null,
        onAddToShopping = null,
        onTechnique = {},
        unparsedHint = stringResource(R.string.import_preview_unparsed),
    )
}
