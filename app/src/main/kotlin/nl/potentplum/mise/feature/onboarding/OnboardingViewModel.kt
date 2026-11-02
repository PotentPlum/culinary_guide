package nl.potentplum.mise.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.security.ClaudeKeyStatus
import nl.potentplum.mise.security.SecretStore
import javax.inject.Inject

/** The first-run steps of spec §6.16, in order. */
enum class OnboardingStep { WELCOME, KEY, RESTORE, FIRST_RECIPE }

/** First run (spec §6.16): welcome → optional key → optional restore → "Je eerste recept" → done. */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val recipes: RecipeRepository,
        secrets: SecretStore,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        val step: StateFlow<OnboardingStep> = savedState.getStateFlow(KEY_STEP, OnboardingStep.WELCOME)

        val hasKey: StateFlow<Boolean> =
            secrets.keyState.map { it.status == ClaudeKeyStatus.STORED }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                false,
            )

        fun next() {
            savedState[KEY_STEP] = OnboardingStep.entries.getOrElse(step.value.ordinal + 1) { OnboardingStep.FIRST_RECIPE }
        }

        /** After a restore: "Je eerste recept" is skipped when the backup brought recipes. */
        fun restored(onFinished: () -> Unit) {
            viewModelScope.launch {
                if (recipes.observeLibrary().first().isNotEmpty()) {
                    finish(onFinished)
                } else {
                    savedState[KEY_STEP] =
                        OnboardingStep.FIRST_RECIPE
                }
            }
        }

        /** Marks onboarding done, then runs [then] (open an import flow or Vandaag). */
        fun finish(then: () -> Unit) {
            viewModelScope.launch {
                settings.setOnboardingDone(true)
                then()
            }
        }

        private companion object {
            const val KEY_STEP = "step"
        }
    }
