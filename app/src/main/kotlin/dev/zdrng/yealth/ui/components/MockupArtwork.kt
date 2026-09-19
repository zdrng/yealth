package dev.zdrng.yealth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.*
import dev.zdrng.yealth.ui.theme.*

/** The soft asymmetric silhouette used for feature panels in the reference artwork. */
val OrganicShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width; val h = size.height
        return Outline.Generic(Path().apply {
            moveTo(.14f*w, .015f*h)
            cubicTo(.30f*w, -.03f*h, .39f*w, .075f*h, .55f*w, .045f*h)
            cubicTo(.73f*w, -.01f*h, .89f*w, -.025f*h, .96f*w, .12f*h)
            cubicTo(1.015f*w, .23f*h, 1.01f*w, .79f*h, .95f*w, .91f*h)
            cubicTo(.87f*w, 1.07f*h, .7f*w, .955f*h, .53f*w, .97f*h)
            cubicTo(.32f*w, .99f*h, .15f*w, 1.04f*h, .06f*w, .91f*h)
            cubicTo(-.015f*w, .8f*h, -.015f*w, .25f*h, .04f*w, .12f*h)
            cubicTo(.06f*w, .06f*h, .10f*w, .025f*h, .14f*w, .015f*h)
            close()
        })
    }
}

@Composable
fun DecorativeBloom(modifier: Modifier = Modifier, content: (@Composable () -> Unit)? = null) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width; val h=size.height
            rotate(-25f) { drawOval(Color(0xFF6857ED), topLeft=androidx.compose.ui.geometry.Offset(.13f*w,0f), size=Size(.6f*w,.91f*h)) }
            rotate(35f) { drawOval(Lilac, topLeft=androidx.compose.ui.geometry.Offset(.33f*w,.06f*h), size=Size(.64f*w,.85f*h)) }
            rotate(-14f) { drawOval(Color(0xFF8A75FF).copy(alpha=.85f), topLeft=androidx.compose.ui.geometry.Offset(.02f*w,.5f*h), size=Size(.92f*w,.42f*h)) }
        }
        content?.invoke()
    }
}

enum class HeadingArtwork { Abstract, Search, Access }

@Composable
fun HeadingBloom(artwork: HeadingArtwork, modifier: Modifier = Modifier, content: (@Composable () -> Unit)? = null) {
    DecorativeBloom(modifier) {
        when (artwork) {
            HeadingArtwork.Abstract -> content?.invoke()
            HeadingArtwork.Search -> Canvas(Modifier.size(72.dp)) {
                drawCircle(Color(0xFF7060FC).copy(alpha = .9f))
                val center = androidx.compose.ui.geometry.Offset(size.width * .43f, size.height * .43f)
                drawCircle(Color.White, size.width * .19f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                drawLine(Color.White, center + androidx.compose.ui.geometry.Offset(size.width*.14f,size.height*.14f),
                    androidx.compose.ui.geometry.Offset(size.width*.73f,size.height*.73f), 3.dp.toPx(), StrokeCap.Round)
            }
            HeadingArtwork.Access -> Canvas(Modifier.size(86.dp)) {
                drawOval(Mint)
                val w = size.width; val h = size.height
                drawPath(Path().apply {
                    moveTo(.5f*w,.2f*h); lineTo(.77f*w,.3f*h); lineTo(.75f*w,.57f*h)
                    cubicTo(.72f*w,.73f*h,.6f*w,.84f*h,.5f*w,.88f*h)
                    cubicTo(.38f*w,.83f*h,.25f*w,.72f*h,.23f*w,.57f*h)
                    lineTo(.23f*w,.3f*h); close()
                }, Ink)
                drawPath(Path().apply { moveTo(.37f*w,.52f*h); lineTo(.47f*w,.62f*h); lineTo(.64f*w,.43f*h) },
                    Lilac, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(), cap=StrokeCap.Round, join=StrokeJoin.Round))
            }
        }
    }
}

/** Decorative empty-state illustration: a bowl of vegetables on layered yellow shapes. */
@Composable
fun EmptyNutritionArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        fun point(x:Float,y:Float) = androidx.compose.ui.geometry.Offset(x*w,y*h)
        drawPath(Path().apply {
            moveTo(.14f*w,.31f*h)
            cubicTo(.04f*w,.02f*h,.31f*w,-.02f*h,.43f*w,.12f*h)
            cubicTo(.65f*w,-.15f*h,.81f*w,.02f*h,.83f*w,.29f*h)
            cubicTo(.87f*w,.45f*h,1.02f*w,.56f*h,.89f*w,.83f*h)
            cubicTo(.8f*w,1.01f*h,.44f*w,1.02f*h,.3f*w,.91f*h)
            cubicTo(.16f*w,.83f*h,.25f*w,.56f*h,.14f*w,.31f*h); close()
        }, Color(0xFFFFF9BD))
        drawOval(Lemon, point(.24f,.2f), Size(.55f*w,.7f*h))
        drawOval(Lemon, point(.04f,.57f), Size(.08f*w,.2f*h))
        drawOval(Lemon, point(.85f,.1f), Size(.065f*w,.16f*h))
        drawPath(Path().apply {
            moveTo(.32f*w,.61f*h); lineTo(.71f*w,.61f*h)
            cubicTo(.68f*w,.99f*h,.36f*w,.99f*h,.32f*w,.61f*h); close()
        }, Ink)
        drawOval(Ink, point(.36f,.51f), Size(.1f*w,.1f*h))
        drawOval(Ink, point(.55f,.43f), Size(.12f*w,.18f*h))
        drawOval(Ink, point(.64f,.52f), Size(.06f*w,.08f*h))
        rotate(-28f, point(.47f,.48f)) { drawOval(Ink, point(.445f,.35f), Size(.07f*w,.26f*h)) }
        rotate(28f, point(.52f,.48f)) { drawOval(Ink, point(.49f,.34f), Size(.07f*w,.25f*h)) }
        drawLine(Mint,point(.48f,.58f),point(.54f,.4f),2.dp.toPx(),StrokeCap.Round)
        drawLine(Ink,point(.52f,.27f),point(.52f,.2f),5.dp.toPx(),StrokeCap.Round)
        drawLine(Ink,point(.42f,.3f),point(.405f,.25f),5.dp.toPx(),StrokeCap.Round)
        drawLine(Ink,point(.61f,.3f),point(.63f,.25f),5.dp.toPx(),StrokeCap.Round)
    }
}

/** Linked heart mark on the pale mint medallion used by the Health Connect introduction. */
@Composable
fun HealthConnectArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(Color(0xFFE2FFF2))
        drawPath(Path().apply {
            moveTo(.5f*w,.67f*h)
            cubicTo(.44f*w,.63f*h,.27f*w,.52f*h,.27f*w,.41f*h)
            cubicTo(.27f*w,.28f*h,.4f*w,.25f*h,.49f*w,.35f*h)
            cubicTo(.59f*w,.25f*h,.71f*w,.3f*h,.72f*w,.42f*h)
            cubicTo(.73f*w,.51f*h,.6f*w,.62f*h,.5f*w,.67f*h)
        }, Ink, style = androidx.compose.ui.graphics.drawscope.Stroke(w*.09f, cap=StrokeCap.Round, join=StrokeJoin.Round))
        drawPath(Path().apply {
            moveTo(.46f*w,.64f*h)
            cubicTo(.38f*w,.56f*h,.45f*w,.48f*h,.54f*w,.39f*h)
            cubicTo(.64f*w,.28f*h,.77f*w,.34f*h,.75f*w,.44f*h)
            cubicTo(.74f*w,.49f*h,.65f*w,.59f*h,.59f*w,.66f*h)
            cubicTo(.54f*w,.72f*h,.48f*w,.7f*h,.46f*w,.64f*h)
        }, Color(0xFF6453FF), style = androidx.compose.ui.graphics.drawscope.Stroke(w*.095f, cap=StrokeCap.Round, join=StrokeJoin.Round))
    }
}
