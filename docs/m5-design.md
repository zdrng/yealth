# M5 expressive UI

The design keeps the mockups' large headings, date selection, compact metric summaries and
category grid, using public Material 3 components from the pinned `1.5.0-alpha14` artifact.
The supplied Material reference images informed generous rounded shapes, prominent numerals,
connected controls, meaningful iconography and the rounded weekly bars.

## Reusable presentation

- `HealthIcon`: an official Material icon within a themed Material `Surface`. Its visible
  category label supplies meaning; the icon is decorative to screen readers.
- `MetricSummaryCard`: provider-independent value, loading, supporting and error presentation
  on a clickable Material `Card`. Source/aggregate labels are supplied by the caller.
- `DataCard` and `CategoryTile`: Material cards with category icons, emphasized type and
  consistent primary/secondary/tertiary container roles. Category counts count catalog types.
- `PeriodPicker`: public expressive `ToggleButton` and
  `ButtonGroupDefaults.connected*ButtonShapes` in a wrapping `FlowRow`. Selection has radio
  semantics and the container is a selectable group. This replaces the disconnected tab strip.
- `DateControls`: Material filled tonal icon buttons and date button, retaining date-picker
  behavior and 48 dp interaction targets.
- `LiveAccessScreen`: compact Material list cards show category, access status and available
  type count. The rationale dialog retains the complete data-type list.
- `StepsSummaryCard`: compact provider-total card, emphasized value and native expressive
  `LoadingIndicator`; detailed Week selections include the provider's seven daily aggregates.
- `RoundedBarChart`: the sole custom data drawing, because Material supplies no chart control.
  Bars start at a true zero baseline. Their height maps directly to values; nulls remain gaps
  and display a dash. All bars share one color; none imply a target, score or health judgment. Each column exposes its date and exact value for accessibility. At large font scales, exact values move to full-width dated rows below the bars; numeric strings
  remain intact. Original record lists remain available below the chart.

Dynamic wallpaper colors remain enabled. API 28–30 use coordinated lilac, sage and rose
fallback schemes with all surface-container roles specified for light and dark appearances.
Typography uses Material's emphasized styles; interaction motion remains the expressive scheme.

The debug-only Week chart shows seven explicitly synthetic individual Steps records and says
so directly. It never calls them daily totals. Production daily bars are provided by the
Health Connect aggregation service; the UI never adds raw step records together.

Record display chooses meaningful available fields, rather than the first nullable schema
field. Sessions prefer titles and otherwise show their interval length labeled “session”;
this is distinct from the Health Connect sleep total. Series values use the latest sample
by timestamp. All original fields and nested values remain reachable in record detail.

## API and asset provenance

The connected shapes, toggle buttons and loading indicator APIs were verified against the
actual pinned AAR public bytecode and compiled with the pinned Nix toolchain. Current API
reference: [ButtonGroupDefaults](https://developer.android.com/reference/kotlin/androidx/compose/material3/ButtonGroupDefaults).
The current web reference may describe a newer alpha; the local artifact governs this code.

The six drawable assets are official [Google Material Icons](https://github.com/google/material-design-icons),
[Apache License 2.0](https://github.com/google/material-design-icons/blob/master/LICENSE):
`maps/directions_walk`, `image/bedtime`, `maps/restaurant`, `action/accessibility_new`,
`places/spa`, and `maps/local_florist`, all from `materialicons/24px.svg`.
Only the original filled path data was converted to Android vector XML; no dependency or
network permission was added. Core Material icons continue to come from the existing library.

## Verification

`ANDROID_USER_HOME="$HOME/.local/share/yealth/android" nix develop -c ./gradlew :app:compileDebugKotlin --offline`
passed for the component integration. Device layout, scrolling, dark mode and enlarged text
are reviewed separately in the milestone verification report. Existing navigation and test
tags are preserved.

Screenshot review refinement: overview cards now share compact 14 dp vertical padding,
smaller supporting type, stable category colors and a filled Sleep role. Steps refresh is
an icon action alongside the compact summary instead of its own footer. Category headings
use 16 sp Material title type, and enlarged text switches to one column before long words
can orphan a final letter. Large screen headings use the 36 sp emphasized display style.
Record overview times are compact; full dates and offsets remain on raw records and details.

## Opened metric and record refinement — 19 September

The additional Google reference puts the measurement ahead of screen chrome. Metric and
record pages now use the metric name in the compact app bar instead of repeating a 36 sp
heading. `MetricValue` uses Material's 57 sp emphasized display type for the number and
16 sp emphasized title type for its unit, retaining a single accessible text value.

The Steps total leads directly into a 208 dp plot (previously 148 dp); precise query bounds
and source notes follow the chart. Sleep, heart rate, distance and exercise metric pages
reuse their existing provider aggregates and identify total versus average. Other metrics
lead with a labeled individual record. `RecordMetricHero` selects typed primary fields,
keeps a blood-pressure pair together with one unit, identifies series sample timestamps,
and distinguishes session interval length from the provider's sleep/exercise total.
All raw fields, sources, times and metadata remain available below.

Screenshot evidence is in `scratchpad/data-prominence/`: `light` and `dark` contain labeled
synthetic previews, `large-dark` uses 200% Android font settings, and `live` exercises the
production UI with test-only SDK fixtures. These are layout evidence, not personal health data.
Astra implemented the record component; Terra accepted the numeric hierarchy and chart
readability. Large-font layouts remain scrollable with full dated chart values below.
