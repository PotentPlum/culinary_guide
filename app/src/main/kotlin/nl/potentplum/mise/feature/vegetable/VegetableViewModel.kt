package nl.potentplum.mise.feature.vegetable

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.navigation.VegetableRoute
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.domain.LibraryItem
import nl.potentplum.mise.domain.SeasonPresentation
import nl.potentplum.mise.domain.TextMatch
import nl.potentplum.mise.domain.VegetableInSeason
import javax.inject.Inject

/** A substitution chip; [vegetableId] is set when its text names another vegetable (spec §6.5). */
data class Substitution(
    val text: String,
    val vegetableId: String?,
)

sealed interface VegetableUiState {
    data object Loading : VegetableUiState

    data object NotFound : VegetableUiState

    data class Loaded(
        val presentation: VegetableInSeason,
        val month: Int,
        val substitutions: List<Substitution>,
        val techniques: List<Technique>,
        /** "In jouw recepten" (spec §6.5): library recipes that use this vegetable, most cooked first. */
        val recipes: List<LibraryItem> = emptyList(),
    ) : VegetableUiState
}

@HiltViewModel
class VegetableViewModel
    @Inject
    constructor(
        content: ContentRepository,
        recipes: RecipeRepository,
        clock: MiseClock,
        savedState: SavedStateHandle,
    ) : ViewModel() {
        private val id = savedState.toRoute<VegetableRoute>().id

        val state: StateFlow<VegetableUiState> =
            combine(flow { emit(content.content()) }, recipes.observeLibrary()) { content, library ->
                when (val built = build(content, clock.currentMonth())) {
                    is VegetableUiState.Loaded -> {
                        built.copy(
                            recipes =
                                library
                                    .filter { id in it.vegIds }
                                    .sortedWith(compareByDescending<LibraryItem> { it.cookedCount }.thenBy { it.title }),
                        )
                    }

                    else -> {
                        built
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VegetableUiState.Loading)

        private fun build(
            content: Content,
            month: Int,
        ): VegetableUiState {
            val vegetable = content.vegetable(id) ?: return VegetableUiState.NotFound
            return VegetableUiState.Loaded(
                presentation = SeasonPresentation.present(vegetable, month),
                month = month,
                substitutions = vegetable.substitutions.map { Substitution(it, VegetableLinks.find(content, it, exclude = id)) },
                techniques = content.techniques.filter { id in it.relatedVegIds },
            )
        }
    }

/** Resolves free text such as "Groene asperges" or "peen" to a vegetable by any of its " / " names. */
object VegetableLinks {
    fun find(
        content: Content,
        text: String,
        exclude: String? = null,
    ): String? {
        val wanted = TextMatch.fold(text)
        return content.vegetables
            .firstOrNull { veg -> veg.id != exclude && veg.nl.split(" / ").any { TextMatch.fold(it) == wanted } }
            ?.id
    }
}
