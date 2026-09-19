# Mockup matching — 19 September 2026

The five `mockups/yealth-*.png` sheets are the visual source of truth for this change.
The style guidance in PLAN.md was not applied, per the user's instruction.

Sol implemented the screen layouts. Astra implemented and independently reviewed the
shared typography, palette, illustrations, cards and charts. The primary agent integrated
navigation and the live-data views, ran the emulator comparisons, and fed discrepancies
back through successive refinement passes.

## Visual changes

- Fixed light/dark palettes with lilac, mint, yellow, blue and electric-purple cards.
- Bundled licensed Nunito for the rounded headings and branding.
- Decorative heading artwork, Health Connect symbol and Nutrition empty-state illustration.
- Compact Today date pill, organic Health Connect panel, large numbers with smaller units.
- Filled Browse search, reference category order, two-column colorful tiles.
- Pill navigation with grid icon and separate compact back controls.
- Colored record-summary rows followed by the original detailed fields and metadata.
- Recorded-value charts and compact source tables shared by sample and live histories.
- Accessible full date labels and enlarged-text layouts.

The sample data remains explicitly labeled. Provider values, dates, source names and
permission states are not fabricated to duplicate the example numbers in the artwork.
The running app also retains categories and raw-detail actions beyond those pictured.

## Screenshot comparison

First-pass captures are in `scratchpad/mockup-match/round1/`; second-pass captures are
in `scratchpad/mockup-match/round2/`. These comparisons drove corrections to heading
line breaks, chart clipping, category order, icon scale, card colors, compact controls,
record-table density, and the empty-state layout.

## Final checks

- Debug and shrunk Release builds passed, including the final light-palette correction.
- 63 JVM unit tests passed; zero failures.
- All 5 BrowserNavigationTest cases passed, including all catalog routes, tab restoration,
  record detail, date selection and access actions.
- All 3 CategoryDeviceTest cases passed through the production service and mapper stack,
  including all 40 stable record types, live aggregate summaries, date-specific empty state,
  and route consent/cancellation.
- Final palette builds were recaptured with 2 light-theme and 3 dark-theme navigation cases,
  all passing. Captures include the default 30-day VO₂ screen as well as period restoration.
- The 200% font DesignDeviceTest passed. The final date pill expands to avoid clipping.
- Lint: zero errors, 20 warnings (12 dependency-update notices and 8 strings made unused
  by the layout changes).

Final screenshots: `scratchpad/mockup-match/final-dark/`, `final-light/`, and
`final-large-font/`. `final-home-browse.jpg` shows the two root screens in both themes;
`final-dark/overview.jpg` covers all reference screen types. Logs are alongside the captures.
The debug sample preview is left running on the project emulator.

The artwork is implemented as scalable Compose drawing and bundled typography, rather
than displaying the mockup image itself. Device system bars, real record contents, and
additional categories remain responsive to the device and available data.
