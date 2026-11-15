package io.github.bornaradusin.trainingcoach.anim

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.bornaradusin.trainingcoach.data.Anim
import io.github.bornaradusin.trainingcoach.data.Exercise
import kotlin.math.PI
import kotlin.math.cos

/** Pulleys and pivots on the drawing of the IM-309 (the same numbers as tools/animations/render.py). */
private object Machine {
    val side = mapOf(
        "HP" to Offset(54f, 12f), "LP" to Offset(34f, 95f), "MP" to Offset(71f, 34f), "PRESS" to Offset(72f, 24f),
    )
    val front = mapOf("HP" to Offset(50f, 4f), "LP" to Offset(50f, 95f), "MP" to Offset(50f, 30f))
}

/**
 * An exercise done on the machine, as a looping animation: a figure lifting, a pause, and the
 * slower way back, with the cable and the weight stack moving along.
 */
@Composable
fun ExerciseAnimation(exercise: Exercise, modifier: Modifier = Modifier, playing: Boolean = true) {
    // A still (thumbnails) shows the middle of the movement
    val t = if (playing) {
        val transition = rememberInfiniteTransition(label = exercise.id)
        val time by transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
            label = "time",
        )
        progress(time)
    } else 0.5f
    val machine = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val frame = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val figure = MaterialTheme.colorScheme.primary
    val far = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val cable = MaterialTheme.colorScheme.tertiary
    val plates = MaterialTheme.colorScheme.secondary
    Canvas(modifier.aspectRatio(1f)) {
        val u = size.minDimension / 100f
        val a = exercise.anim
        val front = a.view == "front"
        drawMachine(front, u, machine, frame, plates, if (exercise.bodyweight) 0f else t)
        drawFigure(a, Rig.between(a.start, a.end, t), front, u, figure, far, cable, machine)
    }
}

/** Hold, lift (1.2 s), hold at the top, lower slowly (1.6 s). */
private fun progress(time: Float): Float {
    fun ease(x: Float) = (0.5f - 0.5f * cos(PI * x)).toFloat()
    return when {
        time < 0.08f -> 0f
        time < 0.42f -> ease((time - 0.08f) / 0.34f)
        time < 0.52f -> 1f
        time < 0.96f -> 1f - ease((time - 0.52f) / 0.44f)
        else -> 0f
    }
}

private fun DrawScope.drawMachine(front: Boolean, u: Float, line: Color, frame: Color, plates: Color, lift: Float) {
    fun ln(a: Offset, b: Offset, w: Float = 1.6f, c: Color = line) =
        drawLine(c, a * u, b * u, strokeWidth = w * u, cap = StrokeCap.Round)
    fun p(x: Float, y: Float) = Offset(x, y)
    if (!front) {
        // Weight stack: 12 plates; the top ones rise as the weight is lifted
        drawRoundRect(frame, Offset(86f * u, 14f * u), Size(10f * u, 82f * u), CornerRadius(u), style = Stroke(0.8f * u))
        val rise = lift * 10f
        for (i in 0 until 12) {
            val y = 70f + i * 2.1f - if (i < 4) rise else 0f
            drawRect(plates.copy(alpha = if (i < 4) 0.8f else 0.4f), Offset(87.5f * u, y * u), Size(7f * u, 1.6f * u))
        }
        ln(p(30f, 96f), p(98f, 96f), 2.4f)
        ln(p(72f, 96f), p(72f, 10f))
        ln(p(52f, 10f), p(96f, 12f))
        ln(p(52f, 63f), p(67f, 63f), 2.6f)   // seat
        ln(p(60f, 63f), p(60f, 96f))
        ln(p(68f, 63f), p(72f, 33f), 2.6f)   // backrest
        ln(p(47f, 56f), p(55f, 47f), 2.6f)   // preacher pad
        ln(p(49f, 58f), p(49f, 96f))         // leg station post
        for (c in listOf(p(49f, 66f), p(48f, 82f))) drawCircle(line, 1.8f * u, c * u, style = Stroke(1.2f * u))
        for (pulley in Machine.side.values) drawCircle(line, 1.3f * u, pulley * u, style = Stroke(0.9f * u))
    } else {
        drawRoundRect(frame, Offset(42f * u, 8f * u), Size(16f * u, 88f * u), CornerRadius(u), style = Stroke(0.8f * u))
        ln(p(20f, 96f), p(80f, 96f), 2.4f)
        ln(p(40f, 63f), p(60f, 63f), 2.6f)   // seat
        ln(p(46f, 63f), p(46f, 96f)); ln(p(54f, 63f), p(54f, 96f))
        ln(p(30f, 6f), p(70f, 6f))           // top beam
        drawCircle(line, 1.3f * u, Machine.front.getValue("HP") * u, style = Stroke(0.9f * u))
    }
}

private fun DrawScope.drawFigure(
    a: Anim, pose: io.github.bornaradusin.trainingcoach.data.Pose, front: Boolean, u: Float,
    body: Color, far: Color, cable: Color, machine: Color,
) {
    val j = Rig.joints(pose, front)
    fun ln(x: Offset, y: Offset, w: Float, c: Color) = drawLine(c, x * u, y * u, strokeWidth = w * u, cap = StrokeCap.Round)

    // The cable, from its pulley to what it's clipped to
    a.cable?.let { name ->
        val pulley = (if (front) Machine.front else Machine.side)[name]
        if (pulley != null) {
            val end = when (a.attach) {
                "neck" -> j.neck
                "ankle" -> j.ankles[0]
                else -> if (front) (j.hands[0] + j.hands[1]) / 2f else j.hands[0]
            }
            ln(pulley, end, 0.6f, cable)
        }
    }
    // Far limbs first (side view), lighter
    if (!front) {
        ln(j.hips[1], j.knees[1], 2.4f, far); ln(j.knees[1], j.ankles[1], 2.2f, far)
        ln(j.shoulder, j.elbows[1], 2.2f, far); ln(j.elbows[1], j.hands[1], 2f, far)
    }
    // Torso and head
    ln(j.hip, j.shoulder, 3.2f, body)
    ln(j.shoulder, j.neck, 2.2f, body)
    drawCircle(body, Rig.HEAD * u, j.head * u)
    if (front) {
        ln(j.arms[0].first, j.arms[1].first, 2.6f, body)
        ln(j.hips[0], j.hips[1], 2.6f, body)
    }
    val limbs = if (front) listOf(0, 1) else listOf(0)
    for (i in limbs) {
        val shoulder = j.arms[i].first
        ln(j.hips[i], j.knees[i], 2.8f, body); ln(j.knees[i], j.ankles[i], 2.5f, body)
        ln(shoulder, j.elbows[i], 2.6f, body); ln(j.elbows[i], j.hands[i], 2.3f, body)
        drawCircle(body, 1.2f * u, j.hands[i] * u)
    }
    // Attachments
    if (a.lever == "PRESS" && !front) ln(Machine.side.getValue("PRESS"), j.hands[0], 1.4f, machine)
    if (a.pads) for (i in 0..1) {
        // Butterfly pads along the forearms
        val mid = (j.elbows[i] + j.hands[i]) / 2f
        drawRoundRect(machine, Offset((mid.x - 1.6f) * u, (mid.y - 4f) * u), Size(3.2f * u, 8f * u), CornerRadius(u))
    }
    if (a.bar) {
        val c = if (front) (j.hands[0] + j.hands[1]) / 2f else j.hands[0]
        val half = maxOf(a.barw / 2f, kotlin.math.abs(j.hands[0].x - j.hands[1].x) / 2f + 2f)
        ln(Offset(c.x - half, c.y), Offset(c.x + half, c.y), 1.3f, machine)
    }
    if (a.roller == "ankle") {
        val ankle = j.ankles[0]
        drawCircle(machine, 2f * u, ankle * u)
        if (a.pivot == "knee") ln(j.knees[0], ankle, 0.8f, machine)
    }
}
