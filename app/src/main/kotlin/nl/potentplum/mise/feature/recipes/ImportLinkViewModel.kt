package nl.potentplum.mise.feature.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.potentplum.mise.claude.enrich.EnrichmentQueue
import nl.potentplum.mise.core.navigation.ImportLinkRoute
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeDrafts
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.domain.IngredientScaler
import nl.potentplum.mise.ingest.ImageStore
import nl.potentplum.mise.ingest.JsonLdRecipeParser
import nl.potentplum.mise.ingest.PageFetcher
import nl.potentplum.mise.ingest.RecipeImportError
import nl.potentplum.mise.ingest.RecipeImportMapper
import nl.potentplum.mise.ingest.RecipeIngestionException
import nl.potentplum.mise.ingest.RecipeUrls
import javax.inject.Inject

/** Shared by link and photo import: when a draft can be previewed, and its ingredient lines for the preview. */
object ImportReview {
    fun isComplete(draft: Recipe): Boolean = draft.title.isNotBlank() && draft.ingredients.isNotEmpty() && draft.steps.isNotEmpty()

    fun lines(
        scaler: IngredientScaler,
        draft: Recipe,
    ): List<IngredientGroupLines> =
        draft.ingredientGroups.map { group ->
            IngredientGroupLines(
                group.name,
                group.items.map { IngredientLine(it.id, scaler.format(it.parsed), false, it.parsed.parsed) },
            )
        }
}

sealed interface ImportPhase {
    data object Idle : ImportPhase

    data object Loading : ImportPhase

    data class Failed(
        val error: RecipeImportError,
    ) : ImportPhase

    /** The same canonical URL is already in the library (spec §6.9). */
    data class Duplicate(
        val existingId: String,
        val draft: Recipe,
    ) : ImportPhase

    /** A clean import, shown in read view with Bewaren and Aanpassen. */
    data class Preview(
        val draft: Recipe,
        val ingredients: List<IngredientGroupLines>,
    ) : ImportPhase

    data object Saving : ImportPhase
}

sealed interface ImportEvent {
    data class Saved(
        val id: String,
    ) : ImportEvent

    /** Open edit mode for the draft; [highlightMissing] when the title, ingredients or steps are missing. */
    data class Edit(
        val draftId: String,
        val highlightMissing: Boolean,
    ) : ImportEvent

    data class OpenExisting(
        val id: String,
    ) : ImportEvent
}

data class ImportUiState(
    val url: String = "",
    val phase: ImportPhase = ImportPhase.Idle,
)

/** Import by pasted or shared link (spec §6.9): fetch → parse → preview (or edit when parts are missing) → Bewaren. */
@HiltViewModel
class ImportLinkViewModel
    @Inject
    constructor(
        private val fetcher: PageFetcher,
        private val recipes: RecipeRepository,
        private val drafts: RecipeDrafts,
        private val images: ImageStore,
        private val clock: MiseClock,
        private val enrichment: EnrichmentQueue,
        savedState: SavedStateHandle,
    ) : ViewModel() {
        private val prefilled = savedState.toRoute<ImportLinkRoute>().url
        private val _state = MutableStateFlow(ImportUiState(url = prefilled.orEmpty()))
        val state: StateFlow<ImportUiState> = _state.asStateFlow()
        private val _events = Channel<ImportEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()
        private var job: Job? = null

        init {
            // A shared link starts right away.
            if (!prefilled.isNullOrBlank()) load()
        }

        fun setUrl(value: String) = _state.update { it.copy(url = value, phase = ImportPhase.Idle) }

        fun load() {
            val input = _state.value.url
            job?.cancel()
            _state.update { it.copy(phase = ImportPhase.Loading) }
            job =
                viewModelScope.launch {
                    val phase =
                        try {
                            val page = fetcher.fetch(input)
                            val output =
                                try {
                                    JsonLdRecipeParser.ingestRecipeFromHtml(page.html)
                                } catch (_: RecipeIngestionException) {
                                    throw RecipeImportError.NoRecipe
                                }
                            val draft = RecipeImportMapper(recipes.parser()).map(output, page.url, clock.nowMillis())
                            val existing =
                                recipes.findInLibraryByUrl(page.url)
                                    ?: RecipeUrls.normalize(input)?.toString()?.let { recipes.findInLibraryByUrl(it) }
                            if (existing != null) ImportPhase.Duplicate(existing, draft) else review(draft, previewLines(draft))
                        } catch (e: RecipeImportError) {
                            ImportPhase.Failed(e)
                        }
                    _state.update { it.copy(phase = phase) }
                }
        }

        fun cancel() {
            job?.cancel()
            _state.update { it.copy(phase = ImportPhase.Idle) }
        }

        fun importCopy() {
            val duplicate = _state.value.phase as? ImportPhase.Duplicate ?: return
            viewModelScope.launch { _state.update { it.copy(phase = review(duplicate.draft, previewLines(duplicate.draft))) } }
        }

        fun openExisting() {
            val duplicate = _state.value.phase as? ImportPhase.Duplicate ?: return
            _events.trySend(ImportEvent.OpenExisting(duplicate.existingId))
        }

        /** Bewaren: one tap for a clean import; the image is downloaded so the library works offline. */
        fun save() {
            val draft = (_state.value.phase as? ImportPhase.Preview)?.draft ?: return
            _state.update { it.copy(phase = ImportPhase.Saving) }
            viewModelScope.launch {
                val imagePath = draft.imageUrl?.let { images.downloadRecipeImage(draft.id, it) }
                val saved = recipes.save(draft.copy(imagePath = imagePath))
                enrichment.request(saved.id)
                _events.send(ImportEvent.Saved(saved.id))
            }
        }

        fun edit() {
            val draft = (_state.value.phase as? ImportPhase.Preview)?.draft ?: return
            drafts.put(draft)
            _events.trySend(ImportEvent.Edit(draft.id, highlightMissing = false))
        }

        /** A clean import previews in read view; a missing title, ingredients or steps go straight to edit mode. */
        private fun review(
            draft: Recipe,
            lines: List<IngredientGroupLines>,
        ): ImportPhase {
            if (ImportReview.isComplete(draft)) return ImportPhase.Preview(draft, lines)
            drafts.put(draft)
            _events.trySend(ImportEvent.Edit(draft.id, highlightMissing = true))
            return ImportPhase.Idle
        }

        private suspend fun previewLines(draft: Recipe): List<IngredientGroupLines> = ImportReview.lines(recipes.scaler(), draft)
    }
