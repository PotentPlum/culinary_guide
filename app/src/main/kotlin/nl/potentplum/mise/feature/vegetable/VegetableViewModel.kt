package nl.potentplum.mise.feature.vegetable

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.navigation.VegetableRoute
import nl.potentplum.mise.core.util.MiseClock
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
    ) : VegetableUiState
}

@HiltViewModel
class VegetableViewModel
    @Inject
    constructor(
        content: ContentRepository,
        clock: MiseClock,
        savedState: SavedStateHandle,
    ) : ViewModel() {
        private val id = savedState.toRoute<VegetableRoute>().id
        private val _state = MutableStateFlow<VegetableUiState>(VegetableUiState.Loading)
        val state: StateFlow<VegetableUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch { _state.value = build(content.content(), clock.currentMonth()) }
        }

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
