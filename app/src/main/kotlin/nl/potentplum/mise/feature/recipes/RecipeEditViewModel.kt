package nl.potentplum.mise.feature.recipes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.FacetKey
import nl.potentplum.mise.content.model.FacetOption
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.navigation.RecipeEditRoute
import nl.potentplum.mise.core.util.IoDispatcher
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeDrafts
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.newId
import nl.potentplum.mise.ingest.ImageStore
import java.io.File
import javax.inject.Inject

data class EditOptions(
    val titles: FacetTitles = FacetTitles(),
    val dishTypeTitle: String = "",
    val cuisines: List<FacetOption> = emptyList(),
    val dishTypes: List<FacetOption> = emptyList(),
    val mainIngredients: List<FacetOption> = emptyList(),
    val techniques: List<Technique> = emptyList(),
)

data class RecipeEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = false,
    val editor: RecipeEditorState? = null,
    val dirty: Boolean = false,
    val showErrors: Boolean = false,
    val options: EditOptions = EditOptions(),
)

/**
 * Edit mode (spec §6.8). The editor state lives in the [SavedStateHandle], so an edit survives process death; a new
 * image goes to a temporary file and only replaces the recipe's image on save.
 */
@HiltViewModel
class RecipeEditViewModel
    @Inject
    constructor(
        private val recipes: RecipeRepository,
        private val drafts: RecipeDrafts,
        private val images: ImageStore,
        content: ContentRepository,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val io: CoroutineDispatcher,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedState.toRoute<RecipeEditRoute>()
        private val routeId = route.id
        private var original: Recipe? = null
        private val initial = MutableStateFlow<RecipeEditorState?>(null)
        private val options = MutableStateFlow(EditOptions())
        private val showErrors = MutableStateFlow(route.highlightMissing)
        private val editor = savedState.getStateFlow<String?>(KEY_EDITOR, null).map { json -> json?.let(::decode) }

        init {
            viewModelScope.launch {
                val loaded = routeId?.let { if (route.draft) drafts.get(it) else recipes.get(it) }
                original = loaded
                val start = loaded?.let(RecipeEditorMapper::fromRecipe) ?: RecipeEditorState(id = newId())
                initial.value = start
                if (savedState.get<String>(KEY_EDITOR) == null) savedState[KEY_EDITOR] = encode(start)
                val all = content.content()
                options.value =
                    EditOptions(
                        titles =
                            FacetTitles(
                                cuisine = all.facet(FacetKey.CUISINE).nl,
                                mainIngredient = all.facet(FacetKey.MAIN_INGREDIENT).nl,
                                prepTime = all.facet(FacetKey.PREP_TIME).nl,
                            ),
                        dishTypeTitle = all.facet(FacetKey.DISH_TYPE).nl,
                        cuisines = all.facet(FacetKey.CUISINE).options,
                        dishTypes = all.facet(FacetKey.DISH_TYPE).options,
                        mainIngredients = all.facet(FacetKey.MAIN_INGREDIENT).options,
                        techniques = all.skills.sortedBy { it.nl },
                    )
            }
        }

        val state: StateFlow<RecipeEditUiState> =
            combine(editor, initial, options, showErrors) { editor, initial, options, errors ->
                RecipeEditUiState(
                    loading = editor == null,
                    isNew = routeId == null || route.draft,
                    editor = editor,
                    dirty = editor != null && editor != initial,
                    showErrors = errors,
                    options = options,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, RecipeEditUiState())

        val current: RecipeEditorState? get() = savedState.get<String>(KEY_EDITOR)?.let(::decode)

        fun update(transform: (RecipeEditorState) -> RecipeEditorState) {
            val editor = current ?: return
            savedState[KEY_EDITOR] = encode(transform(editor))
        }

        /** Downscales a picked photo into a temporary file; it replaces the recipe image on save. */
        fun setImage(uri: Uri) {
            val editor = current ?: return
            viewModelScope.launch {
                val path =
                    withContext(io) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
                        images.store(File(File(context.cacheDir, "edit"), "${editor.id}-${System.nanoTime()}.jpg"), bytes)
                    }
                if (path != null) update { it.copy(imagePath = path) }
            }
        }

        fun removeImage() = update { it.copy(imagePath = null) }

        private val _saved = Channel<String>(Channel.BUFFERED)

        /** The id of the saved recipe, once per save. */
        val saved = _saved.receiveAsFlow()

        /** Saves; a missing title shows the errors instead. */
        fun save() {
            viewModelScope.launch { persist()?.let { _saved.send(it) } }
        }

        private suspend fun persist(): String? {
            val editor = current ?: return null
            if (!editor.hasTitle) {
                showErrors.value = true
                return null
            }
            val finalImage =
                withContext(io) {
                    val target = images.recipeImage(editor.id)
                    when {
                        editor.imagePath == null -> {
                            target.delete()
                            null
                        }

                        editor.imagePath != target.absolutePath -> {
                            target.parentFile?.mkdirs()
                            File(editor.imagePath).copyTo(target, overwrite = true)
                            File(editor.imagePath).delete()
                            target.absolutePath
                        }

                        else -> {
                            target.absolutePath
                        }
                    }
                }
            val recipe = RecipeEditorMapper.toRecipe(editor.copy(imagePath = finalImage), original, recipes.parser())
            val saved = recipes.save(recipe)
            if (route.draft) drafts.remove(saved.id)
            original = saved
            initial.value = RecipeEditorMapper.fromRecipe(saved)
            savedState[KEY_EDITOR] = encode(initial.value!!)
            return saved.id
        }

        private fun encode(state: RecipeEditorState) = Json.encodeToString(RecipeEditorState.serializer(), state)

        private fun decode(json: String) = Json.decodeFromString(RecipeEditorState.serializer(), json)

        private companion object {
            const val KEY_EDITOR = "editor"
        }
    }
