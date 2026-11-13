package nl.potentplum.mise.feature.quests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import nl.potentplum.mise.claude.ClaudeService
import nl.potentplum.mise.claude.quest.QuestGenerator
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.FacetKey
import nl.potentplum.mise.content.model.MeasureUnit
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.content.model.Vegetable
import nl.potentplum.mise.core.navigation.QuestRefineRoute
import nl.potentplum.mise.core.network.NetworkStatus
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestIngredient
import nl.potentplum.mise.domain.QuestParams
import nl.potentplum.mise.domain.SeasonPresentation
import javax.inject.Inject

data class RefineOptions(
    /** In-season vegetables in the §11.1 order. */
    val vegetables: List<Vegetable> = emptyList(),
    /** Skills only (spec §6.11). */
    val techniques: List<Technique> = emptyList(),
    val cuisines: List<String> = emptyList(),
    val units: List<MeasureUnit> = emptyList(),
)

data class QuestRefineUiState(
    val params: QuestParams = QuestParams(),
    /** What is typed in each row's Hoeveelheid field ("0,5"), aligned with `params.ingredients`. */
    val quantityTexts: List<String> = emptyList(),
    val options: RefineOptions = RefineOptions(),
    val hasKey: Boolean = true,
    val online: Boolean = true,
    val generation: QuestGenState = QuestGenState.Idle,
) {
    val ingredientReady: Boolean
        get() =
            when (params.ingredientMode) {
                IngredientMode.CUSTOM -> params.ingredients.any { it.name.isNotBlank() }
                IngredientMode.SEASONAL -> params.vegIds.isNotEmpty()
                IngredientMode.SURPRISE -> true
            }

    val canGenerate: Boolean get() = hasKey && online && ingredientReady && generation !is QuestGenState.Generating
}

/** Verfijn (spec §6.11): every quest parameter, pre-filled from a vegetable or a technique. Survives process death. */
@HiltViewModel
class QuestRefineViewModel
    @Inject
    constructor(
        generator: QuestGenerator,
        content: ContentRepository,
        claude: ClaudeService,
        network: NetworkStatus,
        clock: MiseClock,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedState.toRoute<QuestRefineRoute>()
        private val generation = QuestGeneration(viewModelScope, generator, content)
        private val options = MutableStateFlow(RefineOptions())
        private val _opened = Channel<String>(Channel.BUFFERED)
        val opened = _opened.receiveAsFlow()

        private val form =
            savedState
                .getStateFlow(KEY_PARAMS, encode(initialParams()))
                .map { Json.decodeFromString(QuestParams.serializer(), it) }
        private val quantities =
            savedState
                .getStateFlow(KEY_QUANTITIES, "[]")
                .map { Json.decodeFromString(ListSerializer(String.serializer()), it) }

        init {
            viewModelScope.launch {
                val all = content.content()
                options.value =
                    RefineOptions(
                        vegetables =
                            SeasonPresentation
                                .ranked(
                                    all.vegetables,
                                    clock.currentMonth(),
                                ).filter { it.status.isAvailable }
                                .map { it.vegetable },
                        techniques = all.skills.sortedWith(compareBy(SeasonPresentation.dutchCollator()) { it.nl }),
                        cuisines = all.facet(FacetKey.CUISINE).options.map { it.value },
                        units = all.units,
                    )
            }
        }

        val state: StateFlow<QuestRefineUiState> =
            combine(form, quantities, options, combine(claude.hasKey, network.online, ::Pair), generation.state) {
                params,
                quantities,
                options,
                (hasKey, online),
                gen,
                ->
                QuestRefineUiState(params, quantities, options, hasKey, online, gen)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestRefineUiState())

        private fun initialParams(): QuestParams =
            when {
                route.vegId != null -> {
                    QuestParams(
                        ingredientMode = IngredientMode.SEASONAL,
                        vegIds = listOf(route.vegId),
                        techniqueId = route.techniqueId,
                    )
                }

                else -> {
                    QuestParams(techniqueId = route.techniqueId)
                }
            }

        fun update(transform: (QuestParams) -> QuestParams) {
            val current = Json.decodeFromString(QuestParams.serializer(), savedState[KEY_PARAMS] ?: encode(initialParams()))
            savedState[KEY_PARAMS] = encode(transform(current))
        }

        fun setIngredient(
            index: Int,
            text: String,
        ) = updateRow(index) { it.copy(name = text.take(QuestParams.MAX_INGREDIENT_LENGTH)) }

        /** Dutch decimals ("0,5"); anything unparseable or ≤ 0 means no quantity. */
        fun setQuantity(
            index: Int,
            text: String,
        ) {
            setQuantityTexts(quantityTexts().padded(index + 1).toMutableList().also { it[index] = text })
            updateRow(index) {
                it.copy(
                    quantity =
                        text
                            .trim()
                            .replace(',', '.')
                            .toDoubleOrNull()
                            ?.takeIf { q -> q > 0 },
                )
            }
        }

        fun setUnit(
            index: Int,
            unit: String?,
        ) = updateRow(index) { it.copy(unit = unit) }

        /** Another Zelf kiezen row, up to [QuestParams.MAX_INGREDIENTS]. */
        fun addIngredient() =
            update {
                if (it.ingredients.size >= QuestParams.MAX_INGREDIENTS) it else it.copy(ingredients = it.ingredients + QuestIngredient(""))
            }

        fun removeIngredient(index: Int) {
            setQuantityTexts(quantityTexts().filterIndexed { i, _ -> i != index })
            update { params ->
                val left = params.ingredients.filterIndexed { i, _ -> i != index }
                params.copy(ingredients = left.ifEmpty { listOf(QuestIngredient("")) })
            }
        }

        /** Seizoensgroente: in or out, up to [QuestParams.MAX_INGREDIENTS] (the picker disables the rest). */
        fun toggleVegetable(id: String) =
            update {
                when {
                    id in it.vegIds -> it.copy(vegIds = it.vegIds - id)
                    it.vegIds.size >= QuestParams.MAX_INGREDIENTS -> it
                    else -> it.copy(vegIds = it.vegIds + id)
                }
            }

        private fun updateRow(
            index: Int,
            transform: (QuestIngredient) -> QuestIngredient,
        ) = update { params ->
            params.copy(ingredients = params.ingredients.mapIndexed { i, item -> if (i == index) transform(item) else item })
        }

        private fun quantityTexts(): List<String> =
            Json.decodeFromString(ListSerializer(String.serializer()), savedState[KEY_QUANTITIES] ?: "[]")

        private fun setQuantityTexts(texts: List<String>) {
            savedState[KEY_QUANTITIES] = Json.encodeToString(ListSerializer(String.serializer()), texts)
        }

        private fun List<String>.padded(size: Int) = this + List((size - this.size).coerceAtLeast(0)) { "" }

        fun generate() {
            if (!state.value.canGenerate) return
            val params = state.value.params
            generation.start(params = { params }, onDone = { _opened.send(it.id) })
        }

        fun cancel() = generation.cancel()

        fun dismissError() = generation.dismissError()

        private fun encode(params: QuestParams) = Json.encodeToString(QuestParams.serializer(), params)

        private companion object {
            const val KEY_PARAMS = "params"
            const val KEY_QUANTITIES = "quantities"
        }
    }
