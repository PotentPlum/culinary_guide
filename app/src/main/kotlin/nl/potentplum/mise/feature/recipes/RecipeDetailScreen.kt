package nl.potentplum.mise.feature.recipes

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.FacetChip
import nl.potentplum.mise.core.designsystem.component.FacetChips
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.MenuEntry
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.RecipeActionBar
import nl.potentplum.mise.core.designsystem.component.RecipeImage
import nl.potentplum.mise.core.util.DutchDates
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.domain.ScoreSteps
import nl.potentplum.mise.feature.cook.EvaluationSheet

/** Recept — detail (spec §6.8), in cook-first order, with the sticky Kookmodus / Gekookt bar. */
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCook: (recipeId: String, servings: Int?) -> Unit,
    onTechnique: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val recipe = state.recipe
    var evaluating by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RecipeDetailEvent.Deleted -> {
                    onBack()
                    snackbar.show(
                        message = resources.getString(R.string.recipe_deleted),
                        actionLabel = resources.getString(R.string.common_undo),
                        onAction = { viewModel.restore(event.recipe) },
                    )
                }

                is RecipeDetailEvent.AddedToShopping -> {
                    snackbar.show(resources.getQuantityString(R.plurals.shopping_added, event.count, event.count))
                }
            }
        }
    }

    val menu =
        if (recipe == null) {
            emptyList()
        } else {
            listOf(
                MenuEntry(stringResource(R.string.today_tonight_action)) {
                    viewModel.pinTonight()
                    snackbar.show(resources.getString(R.string.recipe_pinned))
                },
                MenuEntry(stringResource(R.string.recipes_edit_btn)) { onEdit(recipe.id) },
                MenuEntry(stringResource(R.string.recipe_menu_share)) {
                    val text =
                        viewModel.shareText(
                            resources.getString(R.string.recipes_section_ingredients),
                            resources.getString(R.string.recipes_section_instructions),
                        ) ?: return@MenuEntry
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, resources.getString(R.string.recipe_share_chooser)))
                },
                MenuEntry(stringResource(R.string.recipes_saved_remove_label), viewModel::delete),
            )
        }

    Scaffold(
        topBar = {
            MiseTopAppBar(
                title = recipe?.title.orEmpty(),
                onOverflowItem = onOverflowItem,
                onBack = onBack,
                menuEntries = menu,
                actions = {
                    if (recipe != null) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription =
                                    stringResource(
                                        if (recipe.isFavorite) R.string.recipe_favorite_remove else R.string.recipe_favorite_add,
                                    ),
                                tint = if (recipe.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (recipe != null) {
                RecipeActionBar(
                    resumeStep = state.resumeStep,
                    onCook = { onCook(recipe.id, state.servings) },
                    onCooked = { evaluating = true },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> {
                    Unit
                }

                recipe == null -> {
                    EmptyState(title = stringResource(R.string.error_not_found))
                }

                else -> {
                    RecipeBody(
                        state = state,
                        recipe = recipe,
                        onServings = viewModel::setServings,
                        onToggle = viewModel::toggleIngredient,
                        onAddToShopping = viewModel::addToShopping,
                        onTechnique = onTechnique,
                    )
                }
            }
        }
    }
    if (evaluating && recipe != null) {
        EvaluationSheet(recipeId = recipe.id, onDismiss = { evaluating = false }, onLogged = { evaluating = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeBody(
    state: RecipeDetailUiState,
    recipe: Recipe,
    onServings: ((Int) -> Unit)?,
    onToggle: ((String) -> Unit)?,
    onAddToShopping: (() -> Unit)?,
    onTechnique: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    unparsedHint: String? = null,
    /** Items before the recipe, such as a quest's text (spec §6.11). */
    leading: LazyListScope.() -> Unit = {},
    /** Items after the tags and notes, such as a quest's tips. */
    trailing: LazyListScope.() -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        leading()
        item(key = "header") {
            Column {
                if (recipe.imagePath != null) RecipeImage(recipe.imagePath, null, Modifier.fillMaxWidth().height(220.dp))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(recipe.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    if (recipe.sourceLabel.isNotEmpty() || recipe.sourceUrl != null) {
                        val url = recipe.sourceUrl
                        if (url != null) {
                            TextButton(onClick = { uriHandler.openUri(url) }, contentPadding = PaddingValues(0.dp)) {
                                Text(recipe.sourceLabel.ifEmpty { url }, maxLines = 1)
                            }
                        } else {
                            Text(recipe.sourceLabel, style = MaterialTheme.typography.bodyMedium, color = MiseTheme.colors.inkSoft)
                        }
                    }
                    recipe.author?.let {
                        Text(
                            stringResource(R.string.recipe_author, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiseTheme.colors.inkSoft,
                        )
                    }
                    if (recipe.description.isNotBlank()) Text(recipe.description, style = MaterialTheme.typography.bodyLarge)
                    Times(recipe)
                    if (state.servings != null) ServingsStepper(state.servings, onServings)
                }
            }
        }
        ingredients(state, onToggle, onAddToShopping, unparsedHint)
        steps(recipe)
        if (state.techniques.isNotEmpty()) {
            item(key = "techniques") {
                Section(stringResource(R.string.recipe_techniques_title)) {
                    FacetChips(state.techniques.map { FacetChip(it.nl) }, onClick = { chip ->
                        state.techniques.firstOrNull { it.nl == chip.label }?.let { onTechnique(it.id) }
                    })
                }
            }
        }
        if (recipe.extraFields.isNotEmpty()) {
            item(key = "extra") {
                Section(stringResource(R.string.recipe_extra_title)) {
                    recipe.extraFields.forEach { field ->
                        Text(
                            stringResource(R.string.recipe_extra_field, field.label, field.value),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (state.tags.isNotEmpty()) {
            item(key = "tags") { Section(stringResource(R.string.recipes_edit_label_tags)) { FacetChips(state.tags) } }
        }
        recipe.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            item(
                key = "notes",
            ) { Section(stringResource(R.string.recipe_notes_title)) { Text(notes, style = MaterialTheme.typography.bodyMedium) } }
        }
        trailing()
        item(key = "cooked") { Cooked(state.attempts) }
    }
}

@Composable
private fun Times(recipe: Recipe) {
    val parts =
        listOfNotNull(
            recipe.prepMinutes?.let { stringResource(R.string.recipes_label_prep_time) to it },
            recipe.cookMinutes?.let { stringResource(R.string.recipes_label_cook_time) to it },
            recipe.totalMinutes?.let { stringResource(R.string.recipes_label_total_time) to it },
        )
    if (parts.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        parts.forEach { (label, minutes) ->
            Column {
                Eyebrow(label)
                Text(stringResource(R.string.minutes_short, minutes), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ServingsStepper(
    servings: Int,
    onServings: ((Int) -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(stringResource(R.string.recipes_label_servings), Modifier.padding(end = 8.dp))
        if (onServings != null) {
            IconButton(onClick = { onServings(servings - 1) }, enabled = servings > 1) {
                Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.recipes_servings_decrease))
            }
        }
        Text(stringResource(R.string.recipe_servings_value, servings), style = MaterialTheme.typography.titleMedium)
        if (onServings != null) {
            IconButton(onClick = { onServings(servings + 1) }, enabled = servings < 99) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.recipes_servings_increase))
            }
        }
    }
}

private fun LazyListScope.ingredients(
    state: RecipeDetailUiState,
    onToggle: ((String) -> Unit)?,
    onAddToShopping: (() -> Unit)?,
    unparsedHint: String?,
) {
    if (state.ingredients.isEmpty()) return
    item(key = "ingredients-title") {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.recipes_section_ingredients),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (onAddToShopping != null) {
                TextButton(onClick = onAddToShopping) {
                    Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.recipe_add_to_shopping), modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
    if (state.recipe?.enrichState == EnrichState.PENDING) {
        item(key = "ingredients-enriching") {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(R.string.recipe_enriching),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                )
            }
        }
    }
    state.ingredients.forEachIndexed { index, group ->
        group.name?.let { name ->
            item(key = "ingredient-group-$index") { Eyebrow(name, Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)) }
        }
        group.lines.forEach { line ->
            item(key = "ingredient-${line.id}") {
                val rowModifier =
                    if (onToggle != null) {
                        Modifier.toggleable(value = line.checked, role = Role.Checkbox, onValueChange = { onToggle(line.id) })
                    } else {
                        Modifier
                    }
                Row(
                    rowModifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onToggle != null) Checkbox(checked = line.checked, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            line.display,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (line.checked) MiseTheme.colors.inkSoft else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (line.checked) TextDecoration.LineThrough else null,
                        )
                        if (!line.parsed && unparsedHint != null) {
                            Text(unparsedHint, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.steps(recipe: Recipe) {
    if (recipe.steps.isEmpty()) return
    item(key = "steps-title") {
        Text(
            stringResource(R.string.recipes_section_instructions),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp).semantics { heading() },
        )
    }
    recipe.stepGroups.forEachIndexed { index, group ->
        group.name?.let { name -> item(key = "step-group-$index") { Eyebrow(name, Modifier.padding(start = 16.dp, top = 12.dp)) } }
        group.steps.forEachIndexed { number, step ->
            item(key = "step-${step.id}") {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${number + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(step.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
internal fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}

/** The cooked count, last cooked date and the attempts with their scores (spec §6.8). */
@Composable
private fun Cooked(attempts: List<AttemptEntity>) {
    val steps = stringArrayResource(R.array.score_steps)
    Section(stringResource(R.string.recipe_attempts_title)) {
        if (attempts.isEmpty()) {
            Text(
                stringResource(R.string.recipe_never_cooked),
                style = MaterialTheme.typography.bodyMedium,
                color = MiseTheme.colors.inkSoft,
            )
            return@Section
        }
        Text(
            stringResource(
                R.string.recipe_cooked_summary,
                pluralStringResource(R.plurals.recipes_cooked_count, attempts.size, attempts.size),
                DutchDates.day(attempts.maxOf { it.cookedAt }),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        val execution = stringResource(R.string.evaluation_label_score)
        val taste = stringResource(R.string.evaluation_label_taste)
        attempts.forEach { attempt ->
            Column(Modifier.padding(top = 8.dp)) {
                Text(DutchDates.day(attempt.cookedAt), style = MaterialTheme.typography.titleSmall)
                val scores =
                    listOfNotNull(
                        attempt.score?.let { execution to steps[ScoreSteps.stepOf(it)] },
                        attempt.tasteScore?.let { taste to steps[ScoreSteps.stepOf(it)] },
                    )
                when (scores.size) {
                    2 -> {
                        Text(
                            stringResource(
                                R.string.recipe_attempt_scores,
                                scores[0].first,
                                scores[0].second,
                                scores[1].first,
                                scores[1].second,
                            ),
                        )
                    }

                    1 -> {
                        Text(stringResource(R.string.recipe_attempt_score, scores[0].first, scores[0].second))
                    }
                }
                attempt.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                attempt.photoPath?.let { RecipeImage(it, null, Modifier.padding(top = 4.dp).size(120.dp)) }
            }
        }
    }
}
