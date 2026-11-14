package nl.potentplum.mise.feature.curriculum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import nl.potentplum.mise.R
import nl.potentplum.mise.content.CurriculumRepository
import nl.potentplum.mise.data.repo.CurriculumProgressRepository
import nl.potentplum.mise.domain.CurriculumRules
import nl.potentplum.mise.domain.NextStep
import nl.potentplum.mise.domain.StepCount
import javax.inject.Inject

/** The next step of Opleiding and how far along it is; null until the first step is done, or once every step is. */
data class ContinueLearning(
    val next: NextStep,
    val overall: StepCount,
)

@HiltViewModel
class ContinueLearningViewModel
    @Inject
    constructor(
        curriculum: CurriculumRepository,
        progress: CurriculumProgressRepository,
    ) : ViewModel() {
        val state: StateFlow<ContinueLearning?> =
            combine(flow { emit(curriculum.index()) }, progress.observeProgress()) { index, done ->
                val overall = index.courses.fold(StepCount(0, 0)) { sum, course -> sum + CurriculumRules.courseSteps(course, done) }
                val next = CurriculumRules.nextStep(index, done)
                if (overall.started && next != null) ContinueLearning(next, overall) else null
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

/**
 * "Verder leren" on Vandaag (spec §6.2, §6.17): once Opleiding has started, its next step sits where cooking is planned, so
 * the course is not forgotten behind the ⋮ menu. Shows nothing before the first step.
 */
@Composable
fun ContinueLearningCard(
    onLesson: (String) -> Unit,
    onExercise: (String) -> Unit,
    onCheck: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContinueLearningViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    val next = current.next
    NextStepCard(
        next = next,
        eyebrow = stringResource(R.string.today_learning_eyebrow),
        progress = stringResource(R.string.curriculum_overall, current.overall.done, current.overall.total),
        modifier = modifier,
    ) {
        when (next) {
            is NextStep.Lesson -> onLesson(next.id)
            is NextStep.Exercise -> onExercise(next.id)
            is NextStep.Check -> onCheck(next.module.id)
        }
    }
}
