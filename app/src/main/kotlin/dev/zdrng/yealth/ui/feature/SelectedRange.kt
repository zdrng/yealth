package dev.zdrng.yealth.ui.feature

import dev.zdrng.yealth.domain.model.*
import java.time.*

data class SelectedRange(val query: RecordQuery, val recentWindow: HealthTimeRange?)

/** Local calendar boundaries, not multiples of 24 hours. Re-resolve zone on foreground return. */
fun selectedRange(selection: LiveSelection, historyGranted: Boolean, clock: Clock, zone: ZoneId): SelectedRange {
    val requested = HealthTimeRange(selection.period.start(selection.date).atStartOfDay(zone).toInstant(),
        selection.date.plusDays(1).atStartOfDay(zone).toInstant())
    val recentStart = clock.instant().minus(Duration.ofDays(30))
    val limited = !historyGranted && requested.start < recentStart
    val olderOnly = limited && requested.end <= recentStart
    val range = if (limited && !olderOnly) HealthTimeRange(recentStart, requested.end) else requested
    return SelectedRange(RecordQuery(selection.type, range, requireFullHistory = olderOnly || historyGranted),
        range.takeIf { limited && !olderOnly })
}
