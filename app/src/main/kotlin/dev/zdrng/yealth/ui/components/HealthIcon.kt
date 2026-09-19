package dev.zdrng.yealth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import dev.zdrng.yealth.ui.theme.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.HealthCategory

/** A Material surface and official Material icon; decorative alongside the visible label. */
@Composable
fun HealthIcon(category: HealthCategory, modifier: Modifier = Modifier, prominent: Boolean = false, typeId: String? = null, backgroundColor: Color? = null) {
    val tint = LocalContentColor.current
    val palette = LocalMockupColors.current
    Surface(modifier = modifier, shape = CircleShape,
        color = backgroundColor ?: when (category) {
            HealthCategory.SLEEP -> Color(0xFF322AA8).copy(alpha = .55f)
            HealthCategory.VITALS -> Color(0xFFEBD14C).copy(alpha = .4f)
            HealthCategory.NUTRITION -> Color(0xFF6CCF9C).copy(alpha = .25f)
            HealthCategory.WELLNESS -> Color(0xFF739FEB).copy(alpha = .3f)
            else -> Color(0xFF8A58DC).copy(alpha = .18f)
        }, contentColor = tint) {
        Box(Modifier.padding(if (prominent) 16.dp else 12.dp), contentAlignment = Alignment.Center) {
            val iconModifier = Modifier.size(if (prominent) 40.dp else 32.dp)
            if (typeId == "distance") Canvas(iconModifier) {
                val w=size.width; val h=size.height
                drawPath(Path().apply {
                    moveTo(.5f*w,.98f*h); cubicTo(.25f*w,.66f*h,.12f*w,.48f*h,.13f*w,.32f*h)
                    cubicTo(.14f*w,-.08f*h,.86f*w,-.08f*h,.87f*w,.32f*h)
                    cubicTo(.89f*w,.49f*h,.73f*w,.71f*h,.5f*w,.98f*h); close()
                }, tint)
                drawCircle(palette.mint, .12f*w, androidx.compose.ui.geometry.Offset(.5f*w,.34f*h))
            }
            else if (category == HealthCategory.BODY_MEASUREMENTS) Canvas(iconModifier) {
                val w=size.width; val h=size.height
                drawCircle(tint,.17f*w,androidx.compose.ui.geometry.Offset(.5f*w,.18f*h))
                drawRoundRect(tint,androidx.compose.ui.geometry.Offset(.31f*w,.41f*h),androidx.compose.ui.geometry.Size(.38f*w,.57f*h),androidx.compose.ui.geometry.CornerRadius(.07f*w))
                drawLine(tint,androidx.compose.ui.geometry.Offset(.2f*w,.5f*h),androidx.compose.ui.geometry.Offset(.2f*w,.86f*h),.13f*w,androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(tint,androidx.compose.ui.geometry.Offset(.8f*w,.5f*h),androidx.compose.ui.geometry.Offset(.8f*w,.86f*h),.13f*w,androidx.compose.ui.graphics.StrokeCap.Round)
            }
            else if (category == HealthCategory.NUTRITION) Canvas(iconModifier) {
                scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    drawPath(Path().apply {
                        moveTo(12f, 8f); cubicTo(4f, 2f, 1f, 10f, 4f, 18f)
                        cubicTo(6f, 24f, 10f, 22f, 12f, 21f)
                        cubicTo(16f, 24f, 20f, 21f, 21f, 14f)
                        cubicTo(23f, 5f, 17f, 4f, 12f, 8f); close()
                        moveTo(12f, 6f); cubicTo(11f, 2f, 16f, 0f, 18f, 1f)
                        cubicTo(18f, 4f, 15f, 7f, 12f, 6f); close()
                    }, tint)
                }
            }
            else if (category == HealthCategory.VITALS) Icon(Icons.Default.Favorite, null, iconModifier)
            else Icon(painterResource(when (category) {
                HealthCategory.ACTIVITY -> R.drawable.ic_walk
                HealthCategory.BODY_MEASUREMENTS -> R.drawable.ic_accessibility_new
                HealthCategory.CYCLE_TRACKING -> R.drawable.ic_local_florist
                HealthCategory.NUTRITION -> R.drawable.ic_restaurant
                HealthCategory.SLEEP -> R.drawable.ic_bedtime
                else -> R.drawable.ic_spa
            }), null, iconModifier)
        }
    }
}

/** Stable category roles, shared by overview cards and the category grid. */
@Composable
fun categoryCardColors(category: HealthCategory): CardColors {
    val palette = LocalMockupColors.current
    return when (category) {
        HealthCategory.ACTIVITY -> CardDefaults.cardColors(palette.lilac, Ink)
        HealthCategory.SLEEP -> CardDefaults.cardColors(MaterialTheme.colorScheme.tertiary, Color.White)
        HealthCategory.VITALS -> CardDefaults.cardColors(palette.lemon, Ink)
        HealthCategory.CYCLE_TRACKING, HealthCategory.BODY_MEASUREMENTS -> CardDefaults.cardColors(palette.lilac, Ink)
        HealthCategory.NUTRITION -> CardDefaults.cardColors(palette.mint, Ink)
        HealthCategory.WELLNESS -> CardDefaults.cardColors(palette.blue, Ink)
    }
}

fun metricCategory(typeId: String): HealthCategory = when (typeId) {
    "sleep_session" -> HealthCategory.SLEEP
    "heart_rate", "resting_heart_rate", "basal_body_temperature", "blood_glucose", "blood_pressure", "body_temperature",
    "heart_rate_variability_rmssd", "oxygen_saturation", "respiratory_rate", "skin_temperature" -> HealthCategory.VITALS
    "basal_metabolic_rate", "body_fat", "body_water_mass", "bone_mass", "height", "lean_body_mass", "weight" -> HealthCategory.BODY_MEASUREMENTS
    "cervical_mucus", "intermenstrual_bleeding", "menstruation_flow", "menstruation_period", "ovulation_test", "sexual_activity" -> HealthCategory.CYCLE_TRACKING
    "nutrition", "hydration" -> HealthCategory.NUTRITION
    "mindfulness_session" -> HealthCategory.WELLNESS
    else -> HealthCategory.ACTIVITY
}
