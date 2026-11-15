package io.github.bornaradusin.trainingcoach.anim

import androidx.compose.ui.geometry.Offset
import io.github.bornaradusin.trainingcoach.data.Pose
import kotlin.math.cos
import kotlin.math.sin

/**
 * The person, to scale (about 1.75 m), in centimetres: x towards the back of the machine, y up
 * from the floor. The same as tools/animations/rig.py, so the app draws what was checked there.
 */
object Rig {
    const val TORSO = 52f
    const val NECK = 8f
    const val HEAD = 11f
    const val UPPER_ARM = 30f
    const val FOREARM = 26f
    const val THIGH = 43f
    const val SHIN = 42f
    const val FOOT = 22f
    const val HAND = 4.5f
    /** Half widths, front view. */
    const val SHOULDERS = 19f
    const val HIPS = 10f

    /** Thickness from the side. */
    object Side { const val TORSO = 24f; const val UA = 10f; const val FA = 8f; const val TH = 15f; const val SH = 11f; const val FOOT = 7f; const val NECK = 9f }
    /** Thickness from the front. */
    object Front { const val UA = 10f; const val FA = 8f; const val TH = 15f; const val SH = 11f; const val NECK = 11f }

    fun dir(a: Float): Offset {
        val r = Math.toRadians(a.toDouble())
        return Offset(cos(r).toFloat(), sin(r).toFloat())
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        val diff = ((b - a + 540f) % 360f) - 180f
        return a + diff * t
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun angle(a: Float?, b: Float?, t: Float): Float? =
        if (a == null && b == null) null else lerpAngle(a ?: b!!, b ?: a!!, t)

    fun between(s: Pose, e: Pose, t: Float) = Pose(
        hx = lerp(s.hx, e.hx, t), hy = lerp(s.hy, e.hy, t), torso = lerpAngle(s.torso, e.torso, t),
        ua = lerpAngle(s.ua, e.ua, t), fa = lerpAngle(s.fa, e.fa, t),
        th = lerpAngle(s.th, e.th, t), sh = lerpAngle(s.sh, e.sh, t),
        ua2 = angle(s.ua2, e.ua2, t), fa2 = angle(s.fa2, e.fa2, t),
        th2 = angle(s.th2, e.th2, t), sh2 = angle(s.sh2, e.sh2, t),
        face = s.face, nk = lerp(s.nk, e.nk, t),
        uas = lerp(s.uas, e.uas, t), ths = lerp(s.ths, e.ths, t), shs = lerp(s.shs, e.shs, t),
        kx = lerp(s.kx, e.kx, t), sy = lerp(s.sy, e.sy, t),
    )

    /**
     * The joints. Side view: index 0 is the near arm or leg, 1 the far one.
     * Front view: index 0 is the person's right (+x), 1 the left.
     */
    class Joints(
        val hip: Offset, val shoulder: Offset, val head: Offset, val face: Float,
        val shoulders: List<Offset>, val elbows: List<Offset>, val hands: List<Offset>,
        val hips: List<Offset>, val knees: List<Offset>, val ankles: List<Offset>,
    )

    private fun limb(from: Offset, a1: Float, l1: Float, a2: Float, l2: Float, s1: Float = 1f, mirror: Boolean = false): Pair<Offset, Offset> {
        var u = dir(a1); var f = dir(a2)
        if (mirror) { u = Offset(-u.x, u.y); f = Offset(-f.x, f.y) }
        val mid = Offset(from.x + u.x * l1 * s1, from.y + u.y * l1)
        return mid to (mid + f * l2)
    }

    fun joints(p: Pose, front: Boolean): Joints {
        val hip = Offset(p.hx, p.hy)
        val s0 = hip + dir(p.torso) * TORSO
        val sho = Offset(s0.x, s0.y + p.sy)
        val head = sho + dir(p.torso + p.nk) * (NECK + HEAD)
        return if (!front) {
            val a1 = limb(sho, p.ua, UPPER_ARM, p.fa, FOREARM)
            val a2 = limb(sho, p.ua2 ?: p.ua, UPPER_ARM, p.fa2 ?: p.fa, FOREARM)
            val l1 = limb(hip, p.th, THIGH, p.sh, SHIN)
            val l2 = limb(hip, p.th2 ?: p.th, THIGH, p.sh2 ?: p.sh, SHIN)
            Joints(hip, sho, head, p.face, listOf(sho, sho),
                listOf(a1.first, a2.first), listOf(a1.second, a2.second),
                listOf(hip, hip), listOf(l1.first, l2.first), listOf(l1.second, l2.second))
        } else {
            val sides = listOf(1f, -1f)
            val shs = sides.map { Offset(sho.x + it * SHOULDERS, sho.y) }
            val hps = sides.map { Offset(hip.x + it * HIPS, hip.y) }
            val arms = sides.indices.map { limb(shs[it], p.ua, UPPER_ARM, p.fa, FOREARM, p.uas, mirror = it == 1) }
            val knees = sides.indices.map { Offset(hps[it].x + sides[it] * p.kx, hps[it].y - THIGH * p.ths) }
            val ankles = knees.map { Offset(it.x, it.y - SHIN * p.shs) }
            Joints(hip, sho, head, p.face, shs, arms.map { it.first }, arms.map { it.second }, hps, knees, ankles)
        }
    }
}
