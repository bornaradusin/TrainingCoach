package io.github.bornaradusin.trainingcoach.anim

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.bornaradusin.trainingcoach.app
import io.github.bornaradusin.trainingcoach.data.Anim
import io.github.bornaradusin.trainingcoach.data.Exercise
import io.github.bornaradusin.trainingcoach.data.MachineView
import io.github.bornaradusin.trainingcoach.data.Pose
import io.github.bornaradusin.trainingcoach.data.Shape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min

/** The colours of the checked drawings (tools/animations/render.py). */
private object Col {
    val bg = Color(0xFFFAFAFA)
    val frame = Color(70, 72, 78)
    val pad = Color(196, 178, 150)
    val roller = Color(40, 40, 44)
    val pulley = Color(110, 110, 118)
    val chrome = Color(150, 150, 160)
    val stackbox = Color(55, 56, 60)
    val plate = Color(35, 35, 38)
    val plateUp = Color(230, 120, 40)
    val body = Color(52, 110, 190)
    val far = Color(140, 170, 215)
    val work = Color(235, 110, 40)
    val active = Color(20, 160, 90)
    val arrow = Color(220, 40, 40)
    fun of(name: String) = when (name) {
        "pad" -> pad; "roller" -> roller; "pulley" -> pulley; "chrome" -> chrome
        "stackbox" -> stackbox; "plate" -> plate; else -> frame
    }
}

/** The part of the world drawn, in cm: x from, x to, y from, y to. */
private class ViewBox(val x0: Float, val x1: Float, val y0: Float, val y1: Float) {
    val ratio get() = (x1 - x0) / (y1 - y0)
}
private val SIDE_BOX = ViewBox(-85f, 200f, 0f, 220f)
private val FRONT_BOX = ViewBox(-80f, 80f, 0f, 220f)

/**
 * An exercise on the IM-309, drawn to scale, as a loop: start position, the lift, a hold and the
 * slower way back, with the weight stack, the cable and the moving parts going along, an arrow
 * for the direction and a caption for each part of the movement.
 */
@Composable
fun ExerciseAnimation(exercise: Exercise, modifier: Modifier = Modifier, playing: Boolean = true) {
    val catalog = LocalContext.current.app.catalog
    val a = exercise.anim
    val front = a.view == "front"
    val machine = if (front) catalog.front else catalog.side
    val box = if (front) FRONT_BOX else SIDE_BOX
    // A still (thumbnails) shows the middle of the movement
    val time = if (playing) {
        val transition = rememberInfiniteTransition(label = exercise.id)
        val time by transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "time",
        )
        time
    } else 0.25f
    val t = progress(time)
    val travel = if (exercise.bodyweight) 0f else a.travel
    Box(modifier.aspectRatio(box.ratio).clip(RoundedCornerShape(8.dp)).background(Col.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            Pen(this, box).draw(a, machine, t, travel, front, lifting = time < 0.52f)
        }
        if (playing) {
            Text(
                caption(a, time),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF202020),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** Start position, lift (1.4 s), hold at the top, lower slowly (1.8 s). */
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

private fun caption(a: Anim, time: Float) = when {
    time < 0.08f || time >= 0.96f -> "Start position"
    time < 0.42f -> a.captions.lift
    time < 0.52f -> "Hold"
    else -> a.captions.lower
}

private fun List<Float>.pt() = Offset(this[0], this[1])

/** Draws in cm, y up, the way render.py does, in the same order. */
private class Pen(val d: DrawScope, val box: ViewBox) {
    val s = min(d.size.width / (box.x1 - box.x0), d.size.height / (box.y1 - box.y0))
    val ox = (d.size.width - (box.x1 - box.x0) * s) / 2f
    val oy = (d.size.height - (box.y1 - box.y0) * s) / 2f

    fun p(q: Offset) = Offset(ox + (q.x - box.x0) * s, oy + (box.y1 - q.y) * s)

    fun line(a: Offset, b: Offset, w: Float, c: Color) =
        d.drawLine(c, p(a), p(b), strokeWidth = w * s, cap = StrokeCap.Round)

    fun circle(c0: Offset, r: Float, c: Color) = d.drawCircle(c, r * s, p(c0))

    fun rect(x: Float, y: Float, w: Float, h: Float, c: Color, r: Float = 0f) =
        d.drawRoundRect(c, p(Offset(x, y + h)), Size(w * s, h * s), CornerRadius(r * s))

    fun poly(pts: List<Offset>, c: Color) {
        val path = Path()
        pts.forEachIndexed { i, q -> val pp = p(q); if (i == 0) path.moveTo(pp.x, pp.y) else path.lineTo(pp.x, pp.y) }
        path.close()
        d.drawPath(path, c)
    }

    fun shape(sh: Shape) {
        val c = Col.of(sh.c)
        when (sh.t) {
            "rect" -> rect(sh.x, sh.y, sh.w, sh.h, c)
            "rrect" -> rect(sh.x, sh.y, sh.w, sh.h, c, if (sh.r > 0f) sh.r else 2f)
            "line" -> if (sh.a != null && sh.b != null) line(sh.a.pt(), sh.b.pt(), sh.w, c)
            "circle" -> circle(Offset(sh.x, sh.y), sh.r, c)
        }
    }

    /** The weight stack; the 5 plates above the pin rise. Returns the top of the stack. */
    fun stack(m: MachineView, lift: Float, travel: Float): Offset? {
        val st = m.stack ?: return null
        var y = st.y
        val up = 5
        for (i in 0 until st.plates) {
            val rising = i >= st.plates - up
            val dy = if (rising) lift * travel else 0f
            rect(st.x, y + dy, st.w, st.h, if (rising && lift > 0f) Col.plateUp else Col.plate, 0.6f)
            y += st.h + st.gap
        }
        return Offset(st.x + st.w / 2f, y + lift * travel)
    }

    fun effector(j: Rig.Joints, a: Anim, front: Boolean): Offset = when (a.attach) {
        "neck" -> Offset(j.head.x + 6f * j.face, j.head.y - 10f)
        "ankle" -> if (!front) j.ankles[0] else Offset((j.ankles[0].x + j.ankles[1].x) / 2f, j.ankles[0].y)
        else -> if (!front) j.hands[0] else (j.hands[0] + j.hands[1]) / 2f
    }

    fun figureSide(j: Rig.Joints, work: Set<String>) {
        fun w(k: String) = if (k in work) Col.work else Col.body
        val face = j.face
        fun foot(an: Offset) = Offset(an.x + Rig.FOOT * face * 0.8f, an.y - 3f)
        // The far arm and leg, lighter and behind
        line(j.hip, j.knees[1], Rig.Side.TH, Col.far); line(j.knees[1], j.ankles[1], Rig.Side.SH, Col.far)
        line(j.ankles[1], foot(j.ankles[1]), Rig.Side.FOOT, Col.far)
        line(j.shoulder, j.elbows[1], Rig.Side.UA, Col.far); line(j.elbows[1], j.hands[1], Rig.Side.FA, Col.far)
        circle(j.hands[1], Rig.HAND, Col.far)
        // Body
        line(j.hip, j.shoulder, Rig.Side.TORSO, w("torso"))
        line(j.shoulder, j.head, Rig.Side.NECK, Col.body)
        circle(j.head, Rig.HEAD, Col.body)
        // The near arm and leg
        line(j.hip, j.knees[0], Rig.Side.TH, w("th")); line(j.knees[0], j.ankles[0], Rig.Side.SH, w("sh"))
        line(j.ankles[0], foot(j.ankles[0]), Rig.Side.FOOT, Col.body)
        line(j.shoulder, j.elbows[0], Rig.Side.UA, w("ua")); line(j.elbows[0], j.hands[0], Rig.Side.FA, w("fa"))
        circle(j.hands[0], Rig.HAND, Col.body)
    }

    fun figureFront(j: Rig.Joints, work: Set<String>) {
        fun w(k: String) = if (k in work) Col.work else Col.body
        for (i in 0..1) {
            line(j.hips[i], j.knees[i], Rig.Front.TH, w("th")); line(j.knees[i], j.ankles[i], Rig.Front.SH, w("sh"))
        }
        poly(listOf(j.shoulders[1], j.shoulders[0], j.hips[0], j.hips[1]), w("torso"))
        line(j.shoulders[1], j.shoulders[0], 10f, w("torso")); line(j.hips[1], j.hips[0], 12f, Col.body)
        line(j.shoulder, j.head, Rig.Front.NECK, Col.body); circle(j.head, Rig.HEAD, Col.body)
        for (i in 0..1) {
            line(j.shoulders[i], j.elbows[i], Rig.Front.UA, w("ua")); line(j.elbows[i], j.hands[i], Rig.Front.FA, w("fa"))
            circle(j.hands[i], Rig.HAND, Col.body)
        }
    }

    fun arrow(a: Offset, b: Offset) {
        val dx = b.x - a.x; val dy = b.y - a.y; val n = hypot(dx, dy)
        if (n < 1f) return
        val ux = dx / n; val uy = dy / n
        val tip = Offset(a.x + ux * 18f, a.y + uy * 18f)
        line(a, tip, 2.2f, Col.arrow)
        val left = Offset(tip.x - ux * 6f - uy * 4f, tip.y - uy * 6f + ux * 4f)
        val right = Offset(tip.x - ux * 6f + uy * 4f, tip.y - uy * 6f - ux * 4f)
        poly(listOf(tip, left, right), Col.arrow)
    }

    fun draw(a: Anim, m: MachineView, t: Float, travel: Float, front: Boolean, lifting: Boolean) {
        val pose: Pose = Rig.between(a.start, a.end, t)
        val j = Rig.joints(pose, front)
        val parts = a.parts.toSet()
        val behind = a.infront
        for (sh in m.shapes) if (sh.l == "back" || behind) shape(sh)
        if (!front) {
            // The leg lever and the press arms at rest, when they aren't the moving part
            val legPivot = m.pivots["LEG"]?.pt(); val legRest = m.rest["LEG"]?.pt()
            if ("lever" !in parts && "heel" !in parts && legPivot != null && legRest != null) {
                line(legPivot, legRest, 3f, Col.frame); circle(Offset(legRest.x - 6f, legRest.y), 5f, Col.roller)
            }
            val press = m.pivots["PRESS"]?.pt()
            if ("press" !in parts && press != null) {
                line(press, Offset(88f, 100f), 4f, Col.frame); rect(84f, 106f, 7f, 30f, Col.pad, 2f)
            }
        }
        val top = stack(m, t, travel)
        // Cable: the beam pulley to the stack (side view), and its pulley to what it's clipped to
        a.cable?.let { name ->
            val pulley = m.pulleys[name]?.pt() ?: return@let
            if (!front && top != null) m.pulleys["BEAM"]?.pt()?.let { line(it, top, 0.8f, Col.chrome) }
            line(pulley, effector(j, a, front), 1f, Col.active)
        }
        val pressPivot = m.pivots["PRESS"]?.pt()
        if ("press" in parts && !front && pressPivot != null) line(pressPivot, j.hands[1], 4f, Col.frame)
        if (front) figureFront(j, a.work.toSet()) else figureSide(j, a.work.toSet())
        if ("press" in parts && !front && pressPivot != null) {
            line(pressPivot, j.hands[0], 4.5f, Col.active); circle(j.hands[0], 3f, Col.active)
        }
        if ("pads" in parts && front) {
            listOf("R", "L").forEachIndexed { i, side ->
                val pivot = m.pivots["PRESS_$side"]?.pt() ?: return@forEachIndexed
                val el = j.elbows[i]; val ha = j.hands[i]
                line(pivot, Offset(ha.x, ha.y + 8f), 4f, Col.active)
                line(el, ha, 9f, Col.pad)
            }
        }
        if ("bar" in parts) {
            val e = effector(j, a, front); val half = a.barw / 2f
            if (front) line(Offset(e.x - half, e.y), Offset(e.x + half, e.y), 3f, Col.active)
            else circle(e, 3.2f, Col.active)
        }
        if ("rope" in parts) circle(effector(j, a, front), 3f, Col.active)
        val legPivot = m.pivots["LEG"]?.pt()
        if ("lever" in parts && legPivot != null) {
            val an = j.ankles[0]
            line(legPivot, an, 3f, Col.active); circle(Offset(an.x - 6f, an.y + 1f), 5f, Col.roller)
        }
        if ("heel" in parts) {
            val an = j.ankles[0]; circle(Offset(an.x - 6f * j.face, an.y + 4f), 5f, Col.roller)
        }
        for (sh in m.shapes) if (sh.l == "front" && !behind) shape(sh)
        // Arrow: which way it's moving right now
        if (t > 0f && t < 1f) {
            val e0 = effector(Rig.joints(Rig.between(a.start, a.end, t - 0.05f), front), a, front)
            val e1 = effector(Rig.joints(Rig.between(a.start, a.end, t + 0.05f), front), a, front)
            val off = Offset(a.arrowOff.getOrElse(0) { 0f }, a.arrowOff.getOrElse(1) { 0f })
            val e = effector(j, a, front) + off
            arrow(e, if (lifting) e + (e1 - e0) else e + (e0 - e1))
        }
    }
}
