package nl.potentplum.mise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import nl.potentplum.mise.data.db.dao.ShoppingDao
import nl.potentplum.mise.data.settings.Settings
import nl.potentplum.mise.data.settings.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class ShellViewModel
    @Inject
    constructor(
        shoppingDao: ShoppingDao,
        settings: SettingsRepository,
    ) : ViewModel() {
        /** Null until the settings are read, so first run doesn't flash for returning users. */
        val onboardingDone: StateFlow<Boolean?> =
            settings.settings.map<Settings, Boolean?> { it.onboardingDone }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

        /** The Boodschappen badge: unticked items on the list. */
        val uncheckedShoppingCount: StateFlow<Int> =
            shoppingDao.observeUncheckedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    }
