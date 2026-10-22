package nl.potentplum.mise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import nl.potentplum.mise.data.db.dao.ShoppingDao
import javax.inject.Inject

@HiltViewModel
class ShellViewModel
    @Inject
    constructor(
        shoppingDao: ShoppingDao,
    ) : ViewModel() {
        /** The Boodschappen badge: unticked items on the list. */
        val uncheckedShoppingCount: StateFlow<Int> =
            shoppingDao.observeUncheckedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    }
