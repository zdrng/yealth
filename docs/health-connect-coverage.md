# Health Connect coverage — client 1.1.0

Current scope: **40 stable ordinary record readers are implemented** against Health Connect
client 1.1.0. The 41st catalog type, Mindfulness, is explicitly reviewed and deferred as an
experimental API. The manifest contains only these stable readers’ read permissions plus
separate history access; runtime requests are progressive by category and feature-gated.
The table distinguishes raw-record coverage from future specialized history charts.

## Record coverage

| SDK record | Category | ID | Read permission | Time | Feature | Status | Visible route |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ActiveCaloriesBurnedRecord | ACTIVITY | active_calories_burned | READ_ACTIVE_CALORIES_BURNED | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| CyclingPedalingCadenceRecord | ACTIVITY | cycling_pedaling_cadence | READ_EXERCISE | SERIES | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| DistanceRecord | ACTIVITY | distance | READ_DISTANCE | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| ElevationGainedRecord | ACTIVITY | elevation_gained | READ_ELEVATION_GAINED | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| ExerciseSessionRecord | ACTIVITY | exercise_session | READ_EXERCISE | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| FloorsClimbedRecord | ACTIVITY | floors_climbed | READ_FLOORS_CLIMBED | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| PlannedExerciseSessionRecord | ACTIVITY | planned_exercise_session | READ_PLANNED_EXERCISE | INTERVAL | PLANNED_EXERCISE | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| PowerRecord | ACTIVITY | power | READ_POWER | SERIES | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| SpeedRecord | ACTIVITY | speed | READ_SPEED | SERIES | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| StepsRecord | ACTIVITY | steps | READ_STEPS | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail; Today summary |
| StepsCadenceRecord | ACTIVITY | steps_cadence | READ_STEPS | SERIES | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| TotalCaloriesBurnedRecord | ACTIVITY | total_calories_burned | READ_TOTAL_CALORIES_BURNED | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| Vo2MaxRecord | ACTIVITY | vo2_max | READ_VO2_MAX | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail; specialized chart M6 |
| WheelchairPushesRecord | ACTIVITY | wheelchair_pushes | READ_WHEELCHAIR_PUSHES | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Activity → records/detail |
| BasalMetabolicRateRecord | BODY_MEASUREMENTS | basal_metabolic_rate | READ_BASAL_METABOLIC_RATE | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Body Measurements → records/detail |
| BodyFatRecord | BODY_MEASUREMENTS | body_fat | READ_BODY_FAT | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Body Measurements → records/detail |
| BodyWaterMassRecord | BODY_MEASUREMENTS | body_water_mass | READ_BODY_WATER_MASS | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Body Measurements → records/detail |
| BoneMassRecord | BODY_MEASUREMENTS | bone_mass | READ_BONE_MASS | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Body Measurements → records/detail |
| HeightRecord | BODY_MEASUREMENTS | height | READ_HEIGHT | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Body Measurements → records/detail |
| LeanBodyMassRecord | BODY_MEASUREMENTS | lean_body_mass | READ_LEAN_BODY_MASS | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Body Measurements → records/detail |
| WeightRecord | BODY_MEASUREMENTS | weight | READ_WEIGHT | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Body Measurements → records/detail |
| CervicalMucusRecord | CYCLE_TRACKING | cervical_mucus | READ_CERVICAL_MUCUS | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Cycle Tracking → records/detail |
| IntermenstrualBleedingRecord | CYCLE_TRACKING | intermenstrual_bleeding | READ_INTERMENSTRUAL_BLEEDING | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Cycle Tracking → records/detail |
| MenstruationFlowRecord | CYCLE_TRACKING | menstruation_flow | READ_MENSTRUATION | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Cycle Tracking → records/detail |
| MenstruationPeriodRecord | CYCLE_TRACKING | menstruation_period | READ_MENSTRUATION | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Cycle Tracking → records/detail |
| OvulationTestRecord | CYCLE_TRACKING | ovulation_test | READ_OVULATION_TEST | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Cycle Tracking → records/detail |
| SexualActivityRecord | CYCLE_TRACKING | sexual_activity | READ_SEXUAL_ACTIVITY | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Cycle Tracking → records/detail |
| HydrationRecord | NUTRITION | hydration | READ_HYDRATION | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Nutrition → records/detail |
| NutritionRecord | NUTRITION | nutrition | READ_NUTRITION | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Nutrition → records/detail |
| SleepSessionRecord | SLEEP | sleep_session | READ_SLEEP | INTERVAL | — | Implemented M5; typed mapper + shared paging | Browse → Sleep → records/detail; Today summary |
| BasalBodyTemperatureRecord | VITALS | basal_body_temperature | READ_BASAL_BODY_TEMPERATURE | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail |
| BloodGlucoseRecord | VITALS | blood_glucose | READ_BLOOD_GLUCOSE | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail |
| BloodPressureRecord | VITALS | blood_pressure | READ_BLOOD_PRESSURE | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail |
| BodyTemperatureRecord | VITALS | body_temperature | READ_BODY_TEMPERATURE | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail |
| HeartRateRecord | VITALS | heart_rate | READ_HEART_RATE | SERIES | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail; Today summary |
| HeartRateVariabilityRmssdRecord | VITALS | heart_rate_variability_rmssd | READ_HEART_RATE_VARIABILITY | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail |
| OxygenSaturationRecord | VITALS | oxygen_saturation | READ_OXYGEN_SATURATION | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail |
| RespiratoryRateRecord | VITALS | respiratory_rate | READ_RESPIRATORY_RATE | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail |
| RestingHeartRateRecord | VITALS | resting_heart_rate | READ_RESTING_HEART_RATE | INSTANT | — | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail; specialized chart M6 |
| SkinTemperatureRecord | VITALS | skin_temperature | READ_SKIN_TEMPERATURE | SERIES | SKIN_TEMPERATURE | Implemented M5; typed mapper + shared paging | Browse → Vitals → records/detail |
| MindfulnessSessionRecord | WELLNESS | mindfulness_session | READ_MINDFULNESS | INTERVAL | MINDFULNESS | Reviewed M5: deferred to M8 (experimental API) | Browse → Wellness → records/detail |

## Explicit deferrals

- Mindfulness is experimental in 1.1.0: catalogued and feature-gated, with no read adapter or
  permission. M5 review retains deferral to M8 because the pinned API requires experimental opt-in; Wellness exposes unavailable/experimental status rather than an empty result.
- Activity intensity is not in 1.1.0; review a newer SDK before adding it.
- Personal Health Records/FHIR use a separate experimental resource API, not `Record`. They
  remain a tracked post-v1 milestone rather than an unmentioned gap in full-data coverage.
- Exercise route coordinates require their own consent flow when supplied by another app. M5
  exposes consent-required separately from absent data, using the SDK per-session consent contract. No device location permission is requested.

## Source of truth

Inventory reviewed against the published 1.1.0 source archive and the official
[SDK release notes](https://developer.android.com/jetpack/androidx/releases/health-connect).
[Data-type documentation](https://developer.android.com/health-and-fitness/health-connect/data-types)
can describe newer APIs; the pinned artifact determines what this build can support.

A JVM inventory test compares this catalog with concrete `Record` classes in the SDK JAR.
An SDK update that adds or removes a type requires an explicit inventory and coverage review.

## M4 Steps acceptance

Steps now has provider aggregate summaries on Today/Activity/metric screens, plus verified
raw records, incremental pages and individual metadata details. The source total is the
Health Connect aggregate, never a sum of raw rows. Two Toolbox fixtures and a 360-step total
were compared on the API 36 emulator; see [M4 verification](m4-verification.md).
Other catalog entries retain the reader/permission status above; M4 adds no new record type.

## M5 complete stable coverage

All **40 stable ordinary record types** in client 1.1.0 now have explicit compile-time readers.
Mindfulness is the sole experimental catalog entry, deliberately deferred to M8. The manifest
matches the implemented permission set; category requests remain progressive and feature-gated.
Each stable adapter has source fixtures exercising field/time/metadata mapping, access denial,
empty results and cursor paging. All optional nutrients retain null versus zero. Nested stages,
segments, laps, planned blocks/steps/goals/targets and route positions retain their fields;
series retain sample timestamps and units. Detail rows flatten nested structure for lazy display.

Sleep duration, heart-rate average, distance and exercise duration use provider aggregates via
the domain service. Category overview record counts are explicitly partial when a next page
exists; Browse tiles count catalog types, not unqueried health records. Steps Week has exact
local-day provider bins, with no client-side source summing. See M5 verification for acceptance.
