package nl.potentplum.mise.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.potentplum.mise.testing.TestContent
import nl.potentplum.mise.testing.TestResources
import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonalityTest {
    @Test
    fun `all 792 cells of the seasonality fixture match`() {
        val statuses =
            Json
                .parseToJsonElement(TestResources.read("seasonality/status.json"))
                .jsonObject
                .getValue("statuses")
                .jsonObject
        val vegetables = TestContent.content.vegetablesById
        assertEquals(vegetables.keys, statuses.keys)

        var cells = 0
        for ((id, months) in statuses) {
            months.jsonArray.forEachIndexed { index, expected ->
                val actual = Seasonality.statusFor(vegetables.getValue(id), index + 1)
                assertEquals("$id month ${index + 1}", expected.jsonPrimitive.content.uppercase(), actual.name)
                cells++
            }
        }
        assertEquals(792, cells)
    }

    @Test
    fun `soon looks ahead across the year boundary`() {
        assertEquals(SeasonStatus.SOON, Seasonality.statusFor(peak = listOf(1), shoulder = emptyList(), month = 11))
        assertEquals(SeasonStatus.SOON, Seasonality.statusFor(peak = listOf(2), shoulder = emptyList(), month = 12))
        assertEquals(SeasonStatus.OFF, Seasonality.statusFor(peak = listOf(3), shoulder = emptyList(), month = 12))
    }

    @Test
    fun `ending looks back across the year boundary`() {
        assertEquals(SeasonStatus.ENDING, Seasonality.statusFor(peak = listOf(12), shoulder = emptyList(), month = 1))
    }

    @Test
    fun `shoulder wins over soon and ending`() {
        assertEquals(SeasonStatus.SHOULDER, Seasonality.statusFor(peak = listOf(5), shoulder = listOf(4, 6), month = 4))
        assertEquals(SeasonStatus.SHOULDER, Seasonality.statusFor(peak = listOf(5), shoulder = listOf(4, 6), month = 6))
    }

    @Test
    fun `next and previous month wrap`() {
        assertEquals(1, Seasonality.nextMonth(12))
        assertEquals(12, Seasonality.previousMonth(1))
        assertEquals(7, Seasonality.nextMonth(6))
        assertEquals(5, Seasonality.previousMonth(6))
    }
}
