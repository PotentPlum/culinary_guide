package nl.potentplum.mise.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.domain.ShoppingAggregator
import nl.potentplum.mise.domain.ShoppingList
import nl.potentplum.mise.domain.ShoppingListLine
import javax.inject.Inject

data class ShoppingUiState(
    val loading: Boolean = true,
    val list: ShoppingList = ShoppingList(emptyList(), emptyList()),
) {
    val hasChecked: Boolean get() = list.lines.any { it.checked }
}

sealed interface ShoppingEvent {
    /** Something was removed; the snackbar offers Ongedaan maken with [removed]. */
    data class Removed(
        val removed: List<ShoppingItemEntity>,
    ) : ShoppingEvent
}

/** Boodschappen (spec §6.14): the combined list, ticking, manual items, clearing with undo. */
@HiltViewModel
class ShoppingViewModel
    @Inject
    constructor(
        private val shopping: ShoppingRepository,
        content: ContentRepository,
    ) : ViewModel() {
        private val _events = Channel<ShoppingEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        val state: StateFlow<ShoppingUiState> =
            combine(flow { emit(ShoppingAggregator(content.content().units)) }, shopping.observeEntries()) { aggregator, entries ->
                ShoppingUiState(loading = false, list = aggregator.aggregate(entries))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

        fun toggle(line: ShoppingListLine) {
            viewModelScope.launch { shopping.setChecked(line.itemIds, !line.checked) }
        }

        fun addManual(text: String) {
            viewModelScope.launch { shopping.addManual(text) }
        }

        fun delete(line: ShoppingListLine) {
            viewModelScope.launch { removed(shopping.deleteForUndo(line.itemIds)) }
        }

        fun clearChecked() {
            viewModelScope.launch { removed(shopping.clearForUndo(onlyChecked = true)) }
        }

        fun clearAll() {
            viewModelScope.launch { removed(shopping.clearForUndo(onlyChecked = false)) }
        }

        /** Runs in the snackbar's app-wide scope, so it completes after this screen is gone. */
        suspend fun restore(items: List<ShoppingItemEntity>) = shopping.restore(items)

        private suspend fun removed(items: List<ShoppingItemEntity>) {
            if (items.isNotEmpty()) _events.send(ShoppingEvent.Removed(items))
        }

        companion object {
            /** "Delen" as plain text (spec §6.14): unticked lines, combined first, then Los. */
            fun shareText(
                title: String,
                list: ShoppingList,
            ): String =
                buildString {
                    append(title)
                    list.lines.filterNot { it.checked }.forEach { append("\n- ").append(it.text) }
                }
        }
    }
