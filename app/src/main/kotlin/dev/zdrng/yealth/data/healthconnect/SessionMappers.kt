package dev.zdrng.yealth.data.healthconnect

import androidx.health.connect.client.records.*
import dev.zdrng.yealth.domain.model.*

private fun fields(vararg values: Pair<String, HealthValue>) = HealthValue.Fields(values.map { RecordField(it.first, it.second) })
private fun text(value: String?) = value?.let { HealthValue.Text(it) } ?: HealthValue.Missing
private fun meters(value: androidx.health.connect.client.units.Length?) = value?.let { HealthValue.Decimal(it.inMeters, "m") } ?: HealthValue.Missing
private fun count(value: Int) = HealthValue.Integer(value.toLong())
private fun duration(value: java.time.Duration) = HealthValue.Text(value.toString()) // ISO-8601 preserves nanoseconds.

internal fun mapSegment(value: ExerciseSegment) = fields(
    "startTime" to HealthValue.Timestamp(value.startTime), "endTime" to HealthValue.Timestamp(value.endTime),
    "segmentType" to HealthValue.Code(value.segmentType), "repetitions" to count(value.repetitions),
)
internal fun mapLap(value: ExerciseLap) = fields(
    "startTime" to HealthValue.Timestamp(value.startTime), "endTime" to HealthValue.Timestamp(value.endTime), "length" to meters(value.length),
)
internal fun mapStage(value: SleepSessionRecord.Stage) = fields(
    "startTime" to HealthValue.Timestamp(value.startTime), "endTime" to HealthValue.Timestamp(value.endTime), "stage" to HealthValue.Code(value.stage),
)
internal fun mapBlock(value: PlannedExerciseBlock) = fields(
    "repetitions" to count(value.repetitions), "description" to text(value.description),
    "steps" to HealthValue.Items(value.steps.map { step -> fields(
        "exerciseType" to HealthValue.Code(step.exerciseType), "exercisePhase" to HealthValue.Code(step.exercisePhase),
        "description" to text(step.description), "completionGoal" to mapGoal(step.completionGoal),
        "performanceTargets" to HealthValue.Items(step.performanceTargets.map(::mapTarget)),
    ) }),
)

internal fun mapRouteResult(value: ExerciseRouteResult): HealthValue = when (value) {
    is ExerciseRouteResult.Data -> fields("status" to HealthValue.Text("available"), "locations" to mapRoute(value.exerciseRoute))
    is ExerciseRouteResult.ConsentRequired -> fields("status" to HealthValue.Text("consent_required"))
    is ExerciseRouteResult.NoData -> fields("status" to HealthValue.Text("no_data"))
    else -> fields("status" to HealthValue.Text("unsupported"))
}
internal fun mapRoute(value: ExerciseRoute) = HealthValue.Items(value.route.map { location -> fields(
    "time" to HealthValue.Timestamp(location.time),
    "latitude" to HealthValue.Decimal(location.latitude, "°"), "longitude" to HealthValue.Decimal(location.longitude, "°"),
    "horizontalAccuracy" to meters(location.horizontalAccuracy), "verticalAccuracy" to meters(location.verticalAccuracy),
    "altitude" to meters(location.altitude),
) })

internal fun mapGoal(value: ExerciseCompletionGoal): HealthValue = when (value) {
    is ExerciseCompletionGoal.ActiveCaloriesBurnedGoal -> fields("type" to HealthValue.Text("active_calories"), "activeCalories" to HealthValue.Decimal(value.activeCalories.inKilocalories, "kcal"))
    is ExerciseCompletionGoal.TotalCaloriesBurnedGoal -> fields("type" to HealthValue.Text("total_calories"), "totalCalories" to HealthValue.Decimal(value.totalCalories.inKilocalories, "kcal"))
    is ExerciseCompletionGoal.DistanceAndDurationGoal -> fields("type" to HealthValue.Text("distance_and_duration"), "distance" to meters(value.distance), "duration" to duration(value.duration))
    is ExerciseCompletionGoal.DistanceGoal -> fields("type" to HealthValue.Text("distance"), "distance" to meters(value.distance))
    is ExerciseCompletionGoal.DurationGoal -> fields("type" to HealthValue.Text("duration"), "duration" to duration(value.duration))
    is ExerciseCompletionGoal.RepetitionsGoal -> fields("type" to HealthValue.Text("repetitions"), "repetitions" to count(value.repetitions))
    is ExerciseCompletionGoal.StepsGoal -> fields("type" to HealthValue.Text("steps"), "steps" to count(value.steps))
    is ExerciseCompletionGoal.ManualCompletion -> fields("type" to HealthValue.Text("manual_completion"))
    else -> fields("type" to HealthValue.Text("unknown"))
}
internal fun mapTarget(value: ExercisePerformanceTarget): HealthValue = when (value) {
    is ExercisePerformanceTarget.CadenceTarget -> fields("type" to HealthValue.Text("cadence"), "minCadence" to HealthValue.Decimal(value.minCadence, "rpm"), "maxCadence" to HealthValue.Decimal(value.maxCadence, "rpm"))
    is ExercisePerformanceTarget.HeartRateTarget -> fields("type" to HealthValue.Text("heart_rate"), "minHeartRate" to HealthValue.Decimal(value.minHeartRate, "bpm"), "maxHeartRate" to HealthValue.Decimal(value.maxHeartRate, "bpm"))
    is ExercisePerformanceTarget.PowerTarget -> fields("type" to HealthValue.Text("power"), "minPower" to HealthValue.Decimal(value.minPower.inWatts, "W"), "maxPower" to HealthValue.Decimal(value.maxPower.inWatts, "W"))
    is ExercisePerformanceTarget.SpeedTarget -> fields("type" to HealthValue.Text("speed"), "minSpeed" to HealthValue.Decimal(value.minSpeed.inMetersPerSecond, "m/s"), "maxSpeed" to HealthValue.Decimal(value.maxSpeed.inMetersPerSecond, "m/s"))
    is ExercisePerformanceTarget.WeightTarget -> fields("type" to HealthValue.Text("weight"), "mass" to HealthValue.Decimal(value.mass.inGrams, "g"))
    is ExercisePerformanceTarget.RateOfPerceivedExertionTarget -> fields("type" to HealthValue.Text("recorded_rpe"), "rpe" to count(value.rpe))
    is ExercisePerformanceTarget.AmrapTarget -> fields("type" to HealthValue.Text("amrap"))
    else -> fields("type" to HealthValue.Text("unknown"))
}
