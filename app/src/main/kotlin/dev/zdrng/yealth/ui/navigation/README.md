# Navigation

`YealthApp` owns the Today/Browse/Access graphs. Navigation Compose saves and restores each
root's stack; children retain the originating tab. Record details are focused destinations
without bottom/rail navigation. Route arguments contain stable IDs and date/period selections,
never record payloads. ViewModels are scoped to back-stack entries.

M3 keeps the originating route while a rationale or platform permission screen is open.
The metric entry owns its live record ViewModel, shared by its focused detail. The Activity
owns permission-result registration and visibility notifications, with SDK contracts/intents
provided through AppContainer. Returning from system settings triggers a fresh access check.
