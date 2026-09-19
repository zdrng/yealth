package dev.zdrng.yealth.ui.feature

import androidx.annotation.StringRes
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.HealthCategory

@StringRes
fun categoryLabel(category: HealthCategory): Int = when (category) {
    HealthCategory.ACTIVITY -> R.string.category_activity
    HealthCategory.BODY_MEASUREMENTS -> R.string.category_body_measurements
    HealthCategory.CYCLE_TRACKING -> R.string.category_cycle_tracking
    HealthCategory.NUTRITION -> R.string.category_nutrition
    HealthCategory.SLEEP -> R.string.category_sleep
    HealthCategory.VITALS -> R.string.category_vitals
    HealthCategory.WELLNESS -> R.string.category_wellness
}

val typeLabels: Map<String, Int> = mapOf(
    "active_calories_burned" to R.string.type_active_calories_burned,
    "cycling_pedaling_cadence" to R.string.type_cycling_pedaling_cadence,
    "distance" to R.string.type_distance,
    "elevation_gained" to R.string.type_elevation_gained,
    "exercise_session" to R.string.type_exercise_session,
    "floors_climbed" to R.string.type_floors_climbed,
    "planned_exercise_session" to R.string.type_planned_exercise_session,
    "power" to R.string.type_power,
    "speed" to R.string.type_speed,
    "steps" to R.string.type_steps,
    "steps_cadence" to R.string.type_steps_cadence,
    "total_calories_burned" to R.string.type_total_calories_burned,
    "vo2_max" to R.string.type_vo2_max,
    "wheelchair_pushes" to R.string.type_wheelchair_pushes,
    "basal_metabolic_rate" to R.string.type_basal_metabolic_rate,
    "body_fat" to R.string.type_body_fat,
    "body_water_mass" to R.string.type_body_water_mass,
    "bone_mass" to R.string.type_bone_mass,
    "height" to R.string.type_height,
    "lean_body_mass" to R.string.type_lean_body_mass,
    "weight" to R.string.type_weight,
    "cervical_mucus" to R.string.type_cervical_mucus,
    "intermenstrual_bleeding" to R.string.type_intermenstrual_bleeding,
    "menstruation_flow" to R.string.type_menstruation_flow,
    "menstruation_period" to R.string.type_menstruation_period,
    "ovulation_test" to R.string.type_ovulation_test,
    "sexual_activity" to R.string.type_sexual_activity,
    "hydration" to R.string.type_hydration,
    "nutrition" to R.string.type_nutrition,
    "sleep_session" to R.string.type_sleep_session,
    "basal_body_temperature" to R.string.type_basal_body_temperature,
    "blood_glucose" to R.string.type_blood_glucose,
    "blood_pressure" to R.string.type_blood_pressure,
    "body_temperature" to R.string.type_body_temperature,
    "heart_rate" to R.string.type_heart_rate,
    "heart_rate_variability_rmssd" to R.string.type_heart_rate_variability_rmssd,
    "oxygen_saturation" to R.string.type_oxygen_saturation,
    "respiratory_rate" to R.string.type_respiratory_rate,
    "resting_heart_rate" to R.string.type_resting_heart_rate,
    "skin_temperature" to R.string.type_skin_temperature,
    "mindfulness_session" to R.string.type_mindfulness_session,
)

@StringRes
fun periodLabel(period: Period): Int = when (period) {
    Period.DAY -> R.string.day
    Period.WEEK -> R.string.week
    Period.MONTH -> R.string.month
    Period.DAYS_7 -> R.string.days_7
    Period.DAYS_30 -> R.string.days_30
    Period.DAYS_90 -> R.string.days_90
    Period.YEAR -> R.string.year
}
