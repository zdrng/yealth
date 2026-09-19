package dev.zdrng.yealth.data.healthconnect

import androidx.health.connect.client.records.*
import dev.zdrng.yealth.domain.model.*

// Explicit, compile-time mappings for the pinned stable SDK. No reflective production reads.

internal fun mapActiveCaloriesBurned(record: ActiveCaloriesBurnedRecord) = HealthRecord(
    typeId = "active_calories_burned",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("energy", HealthValue.Decimal(record.energy.inKilocalories, "kcal")),
    ),
)

internal fun mapCyclingPedalingCadence(record: CyclingPedalingCadenceRecord) = HealthRecord(
    typeId = "cycling_pedaling_cadence",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(

    ),
    samples = record.samples.map { RecordSample(it.time, listOf(RecordField("revolutionsPerMinute", HealthValue.Decimal(it.revolutionsPerMinute, "rpm")))) },
)

internal fun mapDistance(record: DistanceRecord) = HealthRecord(
    typeId = "distance",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("distance", HealthValue.Decimal(record.distance.inMeters, "m")),
    ),
)

internal fun mapElevationGained(record: ElevationGainedRecord) = HealthRecord(
    typeId = "elevation_gained",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("elevation", HealthValue.Decimal(record.elevation.inMeters, "m")),
    ),
)

internal fun mapExerciseSession(record: ExerciseSessionRecord) = HealthRecord(
    typeId = "exercise_session",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("exerciseType", HealthValue.Code(record.exerciseType)),
        RecordField("title", record.title?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
        RecordField("notes", record.notes?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
        RecordField("segments", HealthValue.Items(record.segments.map { mapSegment(it) })),
        RecordField("laps", HealthValue.Items(record.laps.map { mapLap(it) })),
        RecordField("exerciseRouteResult", mapRouteResult(record.exerciseRouteResult)),
        RecordField("plannedExerciseSessionId", record.plannedExerciseSessionId?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
    ),
)

internal fun mapFloorsClimbed(record: FloorsClimbedRecord) = HealthRecord(
    typeId = "floors_climbed",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("floors", HealthValue.Decimal(record.floors, "floors")),
    ),
)

internal fun mapPlannedExerciseSession(record: PlannedExerciseSessionRecord) = HealthRecord(
    typeId = "planned_exercise_session",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("hasExplicitTime", HealthValue.Flag(record.hasExplicitTime)),
        RecordField("exerciseType", HealthValue.Code(record.exerciseType)),
        RecordField("completedExerciseSessionId", record.completedExerciseSessionId?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
        RecordField("blocks", HealthValue.Items(record.blocks.map { mapBlock(it) })),
        RecordField("title", record.title?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
        RecordField("notes", record.notes?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
    ),
)

internal fun mapPower(record: PowerRecord) = HealthRecord(
    typeId = "power",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(

    ),
    samples = record.samples.map { RecordSample(it.time, listOf(RecordField("power", HealthValue.Decimal(it.power.inWatts, "W")))) },
)

internal fun mapSpeed(record: SpeedRecord) = HealthRecord(
    typeId = "speed",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(

    ),
    samples = record.samples.map { RecordSample(it.time, listOf(RecordField("speed", HealthValue.Decimal(it.speed.inMetersPerSecond, "m/s")))) },
)

internal fun mapStepsCadence(record: StepsCadenceRecord) = HealthRecord(
    typeId = "steps_cadence",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(

    ),
    samples = record.samples.map { RecordSample(it.time, listOf(RecordField("rate", HealthValue.Decimal(it.rate, "steps/min")))) },
)

internal fun mapTotalCaloriesBurned(record: TotalCaloriesBurnedRecord) = HealthRecord(
    typeId = "total_calories_burned",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("energy", HealthValue.Decimal(record.energy.inKilocalories, "kcal")),
    ),
)

internal fun mapWheelchairPushes(record: WheelchairPushesRecord) = HealthRecord(
    typeId = "wheelchair_pushes",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("count", HealthValue.Integer(record.count, "pushes")),
    ),
)

internal fun mapBasalMetabolicRate(record: BasalMetabolicRateRecord) = HealthRecord(
    typeId = "basal_metabolic_rate",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("basalMetabolicRate", HealthValue.Decimal(record.basalMetabolicRate.inWatts, "W")),
    ),
)

internal fun mapBodyFat(record: BodyFatRecord) = HealthRecord(
    typeId = "body_fat",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("percentage", HealthValue.Decimal(record.percentage.value, "%")),
    ),
)

internal fun mapBodyWaterMass(record: BodyWaterMassRecord) = HealthRecord(
    typeId = "body_water_mass",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("mass", HealthValue.Decimal(record.mass.inGrams, "g")),
    ),
)

internal fun mapBoneMass(record: BoneMassRecord) = HealthRecord(
    typeId = "bone_mass",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("mass", HealthValue.Decimal(record.mass.inGrams, "g")),
    ),
)

internal fun mapHeight(record: HeightRecord) = HealthRecord(
    typeId = "height",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("height", HealthValue.Decimal(record.height.inMeters, "m")),
    ),
)

internal fun mapLeanBodyMass(record: LeanBodyMassRecord) = HealthRecord(
    typeId = "lean_body_mass",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("mass", HealthValue.Decimal(record.mass.inGrams, "g")),
    ),
)

internal fun mapWeight(record: WeightRecord) = HealthRecord(
    typeId = "weight",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("weight", HealthValue.Decimal(record.weight.inGrams, "g")),
    ),
)

internal fun mapCervicalMucus(record: CervicalMucusRecord) = HealthRecord(
    typeId = "cervical_mucus",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("appearance", HealthValue.Code(record.appearance)),
        RecordField("sensation", HealthValue.Code(record.sensation)),
    ),
)

internal fun mapIntermenstrualBleeding(record: IntermenstrualBleedingRecord) = HealthRecord(
    typeId = "intermenstrual_bleeding",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(

    ),
)

internal fun mapMenstruationFlow(record: MenstruationFlowRecord) = HealthRecord(
    typeId = "menstruation_flow",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("flow", HealthValue.Code(record.flow)),
    ),
)

internal fun mapMenstruationPeriod(record: MenstruationPeriodRecord) = HealthRecord(
    typeId = "menstruation_period",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(

    ),
)

internal fun mapOvulationTest(record: OvulationTestRecord) = HealthRecord(
    typeId = "ovulation_test",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("result", HealthValue.Code(record.result)),
    ),
)

internal fun mapSexualActivity(record: SexualActivityRecord) = HealthRecord(
    typeId = "sexual_activity",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("protectionUsed", HealthValue.Code(record.protectionUsed)),
    ),
)

internal fun mapHydration(record: HydrationRecord) = HealthRecord(
    typeId = "hydration",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("volume", HealthValue.Decimal(record.volume.inLiters, "L")),
    ),
)

internal fun mapNutrition(record: NutritionRecord) = HealthRecord(
    typeId = "nutrition",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("biotin", record.biotin?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("caffeine", record.caffeine?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("calcium", record.calcium?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("energy", record.energy?.let { HealthValue.Decimal(it.inKilocalories, "kcal") } ?: HealthValue.Missing),
        RecordField("energyFromFat", record.energyFromFat?.let { HealthValue.Decimal(it.inKilocalories, "kcal") } ?: HealthValue.Missing),
        RecordField("chloride", record.chloride?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("cholesterol", record.cholesterol?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("chromium", record.chromium?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("copper", record.copper?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("dietaryFiber", record.dietaryFiber?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("folate", record.folate?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("folicAcid", record.folicAcid?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("iodine", record.iodine?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("iron", record.iron?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("magnesium", record.magnesium?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("manganese", record.manganese?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("molybdenum", record.molybdenum?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("monounsaturatedFat", record.monounsaturatedFat?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("niacin", record.niacin?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("pantothenicAcid", record.pantothenicAcid?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("phosphorus", record.phosphorus?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("polyunsaturatedFat", record.polyunsaturatedFat?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("potassium", record.potassium?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("protein", record.protein?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("riboflavin", record.riboflavin?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("saturatedFat", record.saturatedFat?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("selenium", record.selenium?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("sodium", record.sodium?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("sugar", record.sugar?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("thiamin", record.thiamin?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("totalCarbohydrate", record.totalCarbohydrate?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("totalFat", record.totalFat?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("transFat", record.transFat?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("unsaturatedFat", record.unsaturatedFat?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("vitaminA", record.vitaminA?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("vitaminB12", record.vitaminB12?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("vitaminB6", record.vitaminB6?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("vitaminC", record.vitaminC?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("vitaminD", record.vitaminD?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("vitaminE", record.vitaminE?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("vitaminK", record.vitaminK?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("zinc", record.zinc?.let { HealthValue.Decimal(it.inGrams, "g") } ?: HealthValue.Missing),
        RecordField("name", record.name?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
        RecordField("mealType", HealthValue.Code(record.mealType)),
    ),
)

internal fun mapSleepSession(record: SleepSessionRecord) = HealthRecord(
    typeId = "sleep_session",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("title", record.title?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
        RecordField("notes", record.notes?.let { HealthValue.Text(it) } ?: HealthValue.Missing),
        RecordField("stages", HealthValue.Items(record.stages.map { mapStage(it) })),
    ),
)

internal fun mapBasalBodyTemperature(record: BasalBodyTemperatureRecord) = HealthRecord(
    typeId = "basal_body_temperature",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("temperature", HealthValue.Decimal(record.temperature.inCelsius, "°C")),
        RecordField("measurementLocation", HealthValue.Code(record.measurementLocation)),
    ),
)

internal fun mapBloodGlucose(record: BloodGlucoseRecord) = HealthRecord(
    typeId = "blood_glucose",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("level", HealthValue.Decimal(record.level.inMillimolesPerLiter, "mmol/L")),
        RecordField("specimenSource", HealthValue.Code(record.specimenSource)),
        RecordField("mealType", HealthValue.Code(record.mealType)),
        RecordField("relationToMeal", HealthValue.Code(record.relationToMeal)),
    ),
)

internal fun mapBloodPressure(record: BloodPressureRecord) = HealthRecord(
    typeId = "blood_pressure",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("systolic", HealthValue.Decimal(record.systolic.inMillimetersOfMercury, "mmHg")),
        RecordField("diastolic", HealthValue.Decimal(record.diastolic.inMillimetersOfMercury, "mmHg")),
        RecordField("bodyPosition", HealthValue.Code(record.bodyPosition)),
        RecordField("measurementLocation", HealthValue.Code(record.measurementLocation)),
    ),
)

internal fun mapBodyTemperature(record: BodyTemperatureRecord) = HealthRecord(
    typeId = "body_temperature",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("temperature", HealthValue.Decimal(record.temperature.inCelsius, "°C")),
        RecordField("measurementLocation", HealthValue.Code(record.measurementLocation)),
    ),
)

internal fun mapHeartRateVariabilityRmssd(record: HeartRateVariabilityRmssdRecord) = HealthRecord(
    typeId = "heart_rate_variability_rmssd",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("heartRateVariabilityMillis", HealthValue.Decimal(record.heartRateVariabilityMillis, "ms")),
    ),
)

internal fun mapOxygenSaturation(record: OxygenSaturationRecord) = HealthRecord(
    typeId = "oxygen_saturation",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("percentage", HealthValue.Decimal(record.percentage.value, "%")),
    ),
)

internal fun mapRespiratoryRate(record: RespiratoryRateRecord) = HealthRecord(
    typeId = "respiratory_rate",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("rate", HealthValue.Decimal(record.rate, "breaths/min")),
    ),
)

internal fun mapRestingHeartRate(record: RestingHeartRateRecord) = HealthRecord(
    typeId = "resting_heart_rate",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("beatsPerMinute", HealthValue.Integer(record.beatsPerMinute, "bpm")),
    ),
)

internal fun mapSkinTemperature(record: SkinTemperatureRecord) = HealthRecord(
    typeId = "skin_temperature",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("baseline", record.baseline?.let { HealthValue.Decimal(it.inCelsius, "°C") } ?: HealthValue.Missing),
        RecordField("measurementLocation", HealthValue.Code(record.measurementLocation)),
    ),
    samples = record.deltas.map { RecordSample(it.time, listOf(RecordField("delta", HealthValue.Decimal(it.delta.inCelsius, "°C")))) },
)
