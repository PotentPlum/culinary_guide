package nl.potentplum.mise.feature.quests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.claude.quest.QuestGenerator
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.navigation.QuestRoute
import nl.potentplum.mise.core.network.NetworkStatus
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.domain.QuestParams
import nl.potentplum.mise.feature.recipes.ImportReview
import nl.potentplum.mise.feature.recipes.IngredientGroupLines
import javax.inject.Inject

data class QuestUiState(
    val loading: Boolean = true,
    val quest: QuestEntity? = null,
    val recipe: Recipe? = null,
    val ingredients: List<IngredientGroupLines> = emptyList(),
    val techniques: List<Technique> = emptyList(),
    val vegetableName: String? = null,
    val generation: QuestGenState = QuestGenState.Idle,
    val online: Boolean = true,
) {
    val notFound: Boolean get() = !loading && (quest == null || recipe == null)
}

sealed interface QuestEvent {
    /** "Andere quest" made a new one: it replaces this screen. */
    data class Replaced(
        val id: String,
    ) : QuestEvent

    data object SavedToLibrary : QuestEvent
}

/** The quest result (spec §6.11): text, why, badges, technique chips and the full recipe. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuestViewModel
    @Inject
    constructor(
        private val quests: QuestRepository,
        private val recipes: RecipeRepository,
        private val content: ContentRepository,
        generator: QuestGenerator,
        network: NetworkStatus,
        savedState: SavedStateHandle,
    ) : ViewModel() {
        private val id = savedState.toRoute<QuestRoute>().id
        private val generation = QuestGeneration(viewModelScope, generator, content)
        private val _events = Channel<QuestEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private val quest = quests.observe(id)
        private val recipe = quest.flatMapLatest { it?.let { q -> recipes.observe(q.recipeId) } ?: flowOf(null) }

        val state: StateFlow<QuestUiState> =
            combine(quest, recipe, generation.state, network.online) { quest, recipe, gen, online ->
                val all = content.content()
                QuestUiState(
                    loading = false,
                    quest = quest,
                    recipe = recipe,
                    ingredients = recipe?.let { ImportReview.lines(recipes.scaler(), it) }.orEmpty(),
                    techniques = recipe?.tagValues(TagKey.TECHNIQUE)?.mapNotNull(all::technique).orEmpty(),
                    vegetableName = quest?.vegId?.let(all::vegetable)?.nl,
                    generation = gen,
                    online = online,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestUiState())

        /** Andere quest: the same parameters (spec §6.11). */
        fun another() {
            val current = state.value.quest ?: return
            generation.start(params = { QuestParams.of(current) }, onDone = { _events.send(QuestEvent.Replaced(it.id)) })
        }

        fun cancel() = generation.cancel()

        fun dismissError() = generation.dismissError()

        /** ⋮ Bewaar recept in bibliotheek: the hidden recipe becomes a normal library recipe. */
        fun saveToLibrary() {
            val recipe = state.value.recipe ?: return
            viewModelScope.launch {
                recipes.save(recipe.copy(inLibrary = true))
                _events.send(QuestEvent.SavedToLibrary)
            }
        }
    }
