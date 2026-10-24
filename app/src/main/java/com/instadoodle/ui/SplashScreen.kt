package com.instadoodle.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Launch animation: a pen draws a star and a squiggle, then the app name pops in. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val draw = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { draw.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
        delay(900)
        title.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        delay(600)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(Brand.splashGradient), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Canvas(Modifier.size(180.dp)) {
                val path = doodlePath(size.minDimension)
                val measure = PathMeasure().apply { setPath(path, false) }
                val end = measure.length * draw.value
                val visible = Path()
                measure.getSegment(0f, end, visible, true)
                drawPath(visible, Color.White, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (draw.value in 0.01f..0.99f) {
                    val tip = measure.getPosition(end)
                    drawCircle(Color.White, 7.dp.toPx(), tip)
                    drawCircle(Brand.Yellow, 3.5.dp.toPx(), tip)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "InstaDoodle",
                Modifier.alpha(title.value).scale(0.8f + 0.2f * title.value),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Turn any picture into a doodle",
                Modifier.alpha(title.value),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** One continuous path: a five-point star followed by a looping underline. */
private fun doodlePath(side: Float): Path {
    val cx = side / 2f
    val cy = side * 0.42f
    val outer = side * 0.36f
    val inner = outer * 0.45f
    val path = Path()
    // Start and end at the lower-left tip so the pen flows straight into the underline.
    for (i in 6..16) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val p = Offset(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat())
        if (i == 6) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    val baseY = side * 0.92f
    path.quadraticBezierTo(side * 0.18f, side * 0.80f, side * 0.12f, baseY)
    var x = side * 0.12f
    val step = side * 0.19f
    repeat(4) {
        path.cubicTo(x + step * 0.9f, baseY - side * 0.10f, x + step * 0.1f, baseY - side * 0.12f, x + step * 0.55f, baseY)
        x += step * 0.55f
        path.quadraticBezierTo(x + step * 0.25f, baseY + side * 0.03f, x + step * 0.45f, baseY)
        x += step * 0.45f
    }
    return path
}
