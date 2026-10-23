package nl.potentplum.mise.feature.season

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.domain.CalendarGrouping
import nl.potentplum.mise.domain.SeasonFilter
import nl.potentplum.mise.domain.SeasonStatus
import nl.potentplum.mise.testing.FakeContentRepository
import nl.potentplum.mise.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class SeasonViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        SeasonViewModel(FakeContentRepository(), MiseClock.fixed(LocalDate.of(2026, 9, 15)), saved)

    @Test
    fun `defaults to the verkrijgbaar list in ranked order`() =
        runTest {
            val state = viewModel().state.first { !it.loading }
            assertEquals(9, state.month)
            assertEquals(SeasonFilter.AVAILABLE, state.filter)
            assertEquals(49, state.rows.size)
            assertTrue(state.rows.all { it.status.isAvailable })
            assertEquals(
                "corn",
                state.rows
                    .first()
                    .vegetable.id,
            )
        }

    @Test
    fun `filters and search narrow the list and the counts`() =
        runTest {
            val vm = viewModel()
            vm.setFilter(SeasonFilter.SOON)
            assertTrue(
                vm.state
                    .first { !it.loading }
                    .rows
                    .all { it.status == SeasonStatus.SOON },
            )

            vm.setFilter(SeasonFilter.AVAILABLE)
            vm.setQuery("pompoen")
            val state = vm.state.first { !it.loading && it.query == "pompoen" }
            assertTrue(state.rows.isNotEmpty())
            assertTrue(state.counts.getValue(SeasonFilter.AVAILABLE) < 49)
        }

    @Test
    fun `mode, filter and grouping survive process death`() =
        runTest {
            val saved = SavedStateHandle()
            viewModel(saved).apply {
                setMode(SeasonMode.CALENDAR)
                setGrouping(CalendarGrouping.ALPHABETICAL)
            }
            val restored = viewModel(saved).state.first { !it.loading }
            assertEquals(SeasonMode.CALENDAR, restored.mode)
            assertEquals(1, restored.calendar.size)
            assertFalse(
                restored.calendar
                    .single()
                    .vegetables
                    .isEmpty(),
            )
        }
}
