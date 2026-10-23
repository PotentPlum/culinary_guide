package nl.potentplum.mise.feature.techniques

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.content.model.TechniqueLevel
import nl.potentplum.mise.content.model.Vegetable
import nl.potentplum.mise.core.navigation.TechniqueRoute
import nl.potentplum.mise.domain.TechniqueCatalog
import nl.potentplum.mise.domain.TechniqueCategoryGroup
import javax.inject.Inject

data class TechniquesUiState(
    val loading: Boolean = true,
    val query: String = "",
    val level: TechniqueLevel? = null,
    val groups: List<TechniqueCategoryGroup> = emptyList(),
)

@HiltViewModel
class TechniquesViewModel
    @Inject
    constructor(
        content: ContentRepository,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val techniques = MutableStateFlow<List<Technique>?>(null)
        private val query = savedState.getStateFlow(KEY_QUERY, "")
        private val level = savedState.getStateFlow<String?>(KEY_LEVEL, null)

        init {
            viewModelScope.launch { techniques.value = content.content().techniques }
        }

        val state: StateFlow<TechniquesUiState> =
            combine(techniques, query, level) { all, query, level ->
                if (all == null) return@combine TechniquesUiState()
                val selected = level?.let(TechniqueLevel::valueOf)
                TechniquesUiState(false, query, selected, TechniqueCatalog.grouped(all, selected, query))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TechniquesUiState())

        fun setQuery(value: String) {
            savedState[KEY_QUERY] = value
        }

        fun setLevel(value: TechniqueLevel?) {
            savedState[KEY_LEVEL] = value?.name
        }

        private companion object {
            const val KEY_QUERY = "query"
            const val KEY_LEVEL = "level"
        }
    }

sealed interface TechniqueUiState {
    data object Loading : TechniqueUiState

    data object NotFound : TechniqueUiState

    data class Loaded(
        val technique: Technique,
        val vegetables: List<Vegetable>,
        val related: List<Technique>,
        val parent: Technique?,
        val children: List<Technique>,
    ) : TechniqueUiState
}

@HiltViewModel
class TechniqueViewModel
    @Inject
    constructor(
        content: ContentRepository,
        savedState: SavedStateHandle,
    ) : ViewModel() {
        private val id = savedState.toRoute<TechniqueRoute>().id
        private val _state = MutableStateFlow<TechniqueUiState>(TechniqueUiState.Loading)
        val state: StateFlow<TechniqueUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val all = content.content()
                val technique = all.technique(id)
                _state.value =
                    if (technique == null) {
                        TechniqueUiState.NotFound
                    } else {
                        TechniqueUiState.Loaded(
                            technique = technique,
                            vegetables = technique.relatedVegIds.mapNotNull(all::vegetable),
                            related = technique.relatedTechniqueIds.mapNotNull(all::technique),
                            parent = technique.parentId?.let(all::technique),
                            children = TechniqueCatalog.children(all.techniques, id),
                        )
                    }
            }
        }
    }
