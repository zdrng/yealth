package dev.zdrng.yealth

import androidx.lifecycle.SavedStateHandle
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.ui.feature.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class BrowserViewModelTest {
    private val date = LocalDate.of(2026, 9, 18)
    private val type = RecordType("steps", HealthCategory.ACTIVITY, TimeShape.INTERVAL, "read", emptyList())
    private fun record(id: String, time: RecordTime) = HealthRecord(type.id, time,
        RecordMetadata(id, "test.source", Instant.EPOCH, null, 0, 0, null), emptyList())

    @Test fun `selection restores without storing health values`() {
        val handle = SavedStateHandle()
        val content = BrowserContent(listOf(type), initialDate = date)
        val vm = BrowserViewModel(content, handle, ZoneOffset.UTC)
        vm.search("Steps")
        vm.selectPeriod(Period.WEEK)
        vm.moveDate(-1)
        val restored = BrowserViewModel(content, handle, ZoneOffset.UTC)
        assertEquals("Steps", restored.uiState.value.query)
        assertEquals(date.minusDays(7), restored.uiState.value.date)
        assertEquals(Period.WEEK, restored.uiState.value.period)
        assertEquals(setOf("query", "period", "date"), handle.keys())
    }

    @Test fun `instant boundaries and overlapping intervals are preserved`() {
        val start = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = start.plusSeconds(86400)
        val records = listOf(
            record("at-start", RecordTime.Point(start, null)),
            record("at-end", RecordTime.Point(end, null)),
            record("overlap", RecordTime.Interval(start.minusSeconds(3600), start.plusSeconds(1), null, null)),
            record("ends-at-start", RecordTime.Interval(start.minusSeconds(3600), start, null, null)),
        )
        val vm = BrowserViewModel(BrowserContent(listOf(type), records, initialDate = date), SavedStateHandle(), ZoneOffset.UTC)
        assertEquals(setOf("at-start", "overlap"), vm.uiState.value.records.map { it.metadata.id }.toSet())
        vm.selectDate(date.plusDays(1))
        assertEquals(listOf("at-end"), vm.uiState.value.records.map { it.metadata.id })
    }

    @Test fun `calendar selection handles DST and leap years`() {
        val day = LocalDate.of(2026, 3, 29)
        val zone = ZoneId.of("Europe/Berlin")
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        val content = BrowserContent(listOf(type), listOf(record("late", RecordTime.Point(end.minusSeconds(1), null)), record("next", RecordTime.Point(end, null))), initialDate = day)
        assertEquals(listOf("late"), BrowserViewModel(content, SavedStateHandle(), zone).uiState.value.records.map { it.metadata.id })
        assertEquals(LocalDate.of(2024, 2, 29), Period.YEAR.start(LocalDate.of(2025, 2, 28)))
        assertEquals(LocalDate.of(2024, 2, 29), Period.MONTH.shift(LocalDate.of(2024, 3, 31), -1))
    }

    @Test fun `search uses localized type and category labels and does not read records`() {
        val other = type.copy(id = "sleep_session", category = HealthCategory.SLEEP)
        val labels = mapOf(type.id to "Schritte", other.id to "Schlaf")
        val categories = mapOf(HealthCategory.ACTIVITY to "Aktivität", HealthCategory.SLEEP to "Schlaf")
        assertEquals(listOf(type), filterCatalog(listOf(type, other), " AKTIVITÄT ", labels, categories))
        assertEquals(listOf(other), filterCatalog(listOf(type, other), "schlaf", labels, categories))
        assertTrue(filterCatalog(listOf(type, other), "unknown", labels, categories).isEmpty())
    }

    @Test fun `screen selections are independent and unknown record is explicit`() {
        val content = BrowserContent(listOf(type), initialDate = date)
        val first = BrowserViewModel(content, SavedStateHandle(), ZoneOffset.UTC)
        val second = BrowserViewModel(content, SavedStateHandle(), ZoneOffset.UTC)
        first.selectDate(date.minusDays(1))
        first.search("steps")
        assertEquals(date, second.uiState.value.date)
        assertEquals("", second.uiState.value.query)
        assertNull(second.record("missing"))
        assertFalse(content.isPreview)
        assertTrue(content.records.isEmpty())
    }
}
