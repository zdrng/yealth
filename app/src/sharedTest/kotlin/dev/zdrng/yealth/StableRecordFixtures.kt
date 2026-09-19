package dev.zdrng.yealth

import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.*
import java.time.Instant
import java.time.ZoneOffset

/** Deterministic source fixtures, never part of the production app. */
internal object StableRecordFixtures {
    val start = Instant.parse("2026-09-18T08:00:00Z")
    val end = start.plusSeconds(60)
    val offset = ZoneOffset.ofHours(2)
    val metadata = Metadata.manualEntry("m5-fixture", 3)
    fun all(): Map<String, Record> = linkedMapOf(
        "active_calories_burned" to ActiveCaloriesBurnedRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, energy = Energy.kilocalories(123.456), metadata = metadata),
        "cycling_pedaling_cadence" to CyclingPedalingCadenceRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, samples = listOf(CyclingPedalingCadenceRecord.Sample(start, 80.25)), metadata = metadata),
        "distance" to DistanceRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, distance = Length.meters(12.3456), metadata = metadata),
        "elevation_gained" to ElevationGainedRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, elevation = Length.meters(12.3456), metadata = metadata),
        "exercise_session" to ExerciseSessionRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, metadata = metadata, exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, title = null, notes = null, segments = listOf(ExerciseSegment(start, end, ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, 0)), laps = listOf(ExerciseLap(start, end, Length.meters(300.0))), exerciseRoute = ExerciseRoute(listOf(ExerciseRoute.Location(start, 52.0, 13.0, Length.meters(2.0), null, Length.meters(30.0)))), plannedExerciseSessionId = null),
        "floors_climbed" to FloorsClimbedRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, floors = 42.25, metadata = metadata),
        "planned_exercise_session" to PlannedExerciseSessionRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, metadata = metadata, blocks = listOf(PlannedExerciseBlock(2, listOf(PlannedExerciseStep(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, PlannedExerciseStep.EXERCISE_PHASE_ACTIVE, ExerciseCompletionGoal.DistanceAndDurationGoal(Length.meters(100.0), java.time.Duration.ofSeconds(30)), listOf(ExercisePerformanceTarget.HeartRateTarget(100.0, 140.0)), "step")), "block")), exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, title = "plan", notes = "notes"),
        "power" to PowerRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, samples = listOf(PowerRecord.Sample(start, Power.watts(80.5))), metadata = metadata),
        "speed" to SpeedRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, samples = listOf(SpeedRecord.Sample(start, Velocity.metersPerSecond(3.5))), metadata = metadata),
        "steps" to StepsRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, count = 72L, metadata = metadata),
        "steps_cadence" to StepsCadenceRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, samples = listOf(StepsCadenceRecord.Sample(start, 100.25)), metadata = metadata),
        "total_calories_burned" to TotalCaloriesBurnedRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, energy = Energy.kilocalories(123.456), metadata = metadata),
        "vo2_max" to Vo2MaxRecord(time = start, zoneOffset = offset, metadata = metadata, vo2MillilitersPerMinuteKilogram = 42.25, measurementMethod = 0),
        "wheelchair_pushes" to WheelchairPushesRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, count = 72L, metadata = metadata),
        "basal_metabolic_rate" to BasalMetabolicRateRecord(time = start, zoneOffset = offset, basalMetabolicRate = Power.watts(30.5), metadata = metadata),
        "body_fat" to BodyFatRecord(time = start, zoneOffset = offset, percentage = Percentage(42.5), metadata = metadata),
        "body_water_mass" to BodyWaterMassRecord(time = start, zoneOffset = offset, mass = Mass.grams(12000.123), metadata = metadata),
        "bone_mass" to BoneMassRecord(time = start, zoneOffset = offset, mass = Mass.grams(12000.123), metadata = metadata),
        "height" to HeightRecord(time = start, zoneOffset = offset, height = Length.meters(1.73456), metadata = metadata),
        "lean_body_mass" to LeanBodyMassRecord(time = start, zoneOffset = offset, mass = Mass.grams(12000.123), metadata = metadata),
        "weight" to WeightRecord(time = start, zoneOffset = offset, weight = Mass.grams(12000.123), metadata = metadata),
        "cervical_mucus" to CervicalMucusRecord(time = start, zoneOffset = offset, metadata = metadata, appearance = 0, sensation = 0),
        "intermenstrual_bleeding" to IntermenstrualBleedingRecord(time = start, zoneOffset = offset, metadata = metadata),
        "menstruation_flow" to MenstruationFlowRecord(time = start, zoneOffset = offset, metadata = metadata, flow = 0),
        "menstruation_period" to MenstruationPeriodRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, metadata = metadata),
        "ovulation_test" to OvulationTestRecord(time = start, zoneOffset = offset, result = 0, metadata = metadata),
        "sexual_activity" to SexualActivityRecord(time = start, zoneOffset = offset, metadata = metadata, protectionUsed = 0),
        "hydration" to HydrationRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, volume = Volume.liters(0.75), metadata = metadata),
        "nutrition" to NutritionRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, metadata = metadata, biotin = null, caffeine = null, calcium = null, energy = null, energyFromFat = null, chloride = null, cholesterol = null, chromium = null, copper = null, dietaryFiber = null, folate = null, folicAcid = null, iodine = null, iron = null, magnesium = null, manganese = null, molybdenum = null, monounsaturatedFat = null, niacin = null, pantothenicAcid = null, phosphorus = null, polyunsaturatedFat = null, potassium = null, protein = null, riboflavin = null, saturatedFat = null, selenium = null, sodium = null, sugar = null, thiamin = null, totalCarbohydrate = null, totalFat = null, transFat = null, unsaturatedFat = null, vitaminA = null, vitaminB12 = null, vitaminB6 = null, vitaminC = null, vitaminD = null, vitaminE = null, vitaminK = null, zinc = null, name = null, mealType = 0),
        "sleep_session" to SleepSessionRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, metadata = metadata, title = null, notes = null, stages = listOf(SleepSessionRecord.Stage(start, end, SleepSessionRecord.STAGE_TYPE_LIGHT))),
        "basal_body_temperature" to BasalBodyTemperatureRecord(time = start, zoneOffset = offset, metadata = metadata, temperature = Temperature.celsius(36.7), measurementLocation = 0),
        "blood_glucose" to BloodGlucoseRecord(time = start, zoneOffset = offset, metadata = metadata, level = BloodGlucose.millimolesPerLiter(5.2), specimenSource = 0, mealType = 0, relationToMeal = 0),
        "blood_pressure" to BloodPressureRecord(time = start, zoneOffset = offset, metadata = metadata, systolic = Pressure.millimetersOfMercury(80.5), diastolic = Pressure.millimetersOfMercury(80.5), bodyPosition = 0, measurementLocation = 0),
        "body_temperature" to BodyTemperatureRecord(time = start, zoneOffset = offset, metadata = metadata, temperature = Temperature.celsius(36.7), measurementLocation = 0),
        "heart_rate" to HeartRateRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, samples = listOf(HeartRateRecord.Sample(start, 72)), metadata = metadata),
        "heart_rate_variability_rmssd" to HeartRateVariabilityRmssdRecord(time = start, zoneOffset = offset, heartRateVariabilityMillis = 42.25, metadata = metadata),
        "oxygen_saturation" to OxygenSaturationRecord(time = start, zoneOffset = offset, percentage = Percentage(42.5), metadata = metadata),
        "respiratory_rate" to RespiratoryRateRecord(time = start, zoneOffset = offset, rate = 42.25, metadata = metadata),
        "resting_heart_rate" to RestingHeartRateRecord(time = start, zoneOffset = offset, beatsPerMinute = 72L, metadata = metadata),
        "skin_temperature" to SkinTemperatureRecord(startTime = start, startZoneOffset = offset, endTime = end, endZoneOffset = null, metadata = metadata, deltas = listOf(SkinTemperatureRecord.Delta(start, TemperatureDelta.celsius(-0.25))), baseline = null, measurementLocation = 0),
    )
}
