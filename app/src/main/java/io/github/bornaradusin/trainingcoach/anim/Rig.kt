package io.github.bornaradusin.trainingcoach.anim

import androidx.compose.ui.geometry.Offset
import io.github.bornaradusin.trainingcoach.data.Pose
import kotlin.math.cos
import kotlin.math.sin

/**
 * The stick figure: limb lengths and forward kinematics, the same as tools/animations/rig.py,
 * so what the app draws is what was checked there. Units are 0–100 across the picture.
 */
object Rig {
    const val NECK = 2.5f
    const val HEAD = 3.6f
    const val TORSO = 18f
    const val UPPER_ARM = 11f
    const val FOREARM = 10f
    const val THIGH = 15f
    const val SHIN = 15f
    const val SHOULDERS = 7f
    const val HIPS = 4.5f

    private fun dir(a: Float): Offset {
        val r = Math.toRadians(a.toDouble())
        return Offset(cos(r).toFloat(), sin(r).toFloat())
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        val diff = ((b - a + 540f) % 360f) - 180f
        return a + diff * t
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    fun between(s: Pose, e: Pose, t: Float) = Pose(
        hx = lerp(s.hx, e.hx, t), hy = lerp(s.hy, e.hy, t), torso = lerpAngle(s.torso, e.torso, t),
        ua = lerpAngle(s.ua, e.ua, t), fa = lerpAngle(s.fa, e.fa, t),
        th = lerpAngle(s.th, e.th, t), sh = lerpAngle(s.sh, e.sh, t),
        ua2 = lerpAngle(s.ua2 ?: s.ua, e.ua2 ?: e.ua, t), fa2 = lerpAngle(s.fa2 ?: s.fa, e.fa2 ?: e.fa, t),
        th2 = lerpAngle(s.th2 ?: s.th, e.th2 ?: e.th, t), sh2 = lerpAngle(s.sh2 ?: s.sh, e.sh2 ?: e.sh, t),
        uas = lerp(s.uas, e.uas, t), ths = lerp(s.ths, e.ths, t), sy = lerp(s.sy, e.sy, t),
    )

    /** The figure's joints. Side view: near and far limbs. Front view: right and left (mirrored). */
    class Joints(
        val hip: Offset, val shoulder: Offset, val neck: Offset, val head: Offset,
        val arms: List<Pair<Offset, Offset>>,      // (shoulder, elbow) … per arm, see [elbows]/[hands]
        val elbows: List<Offset>, val hands: List<Offset>,
        val hips: List<Offset>, val knees: List<Offset>, val ankles: List<Offset>,
    )

    fun joints(p: Pose, front: Boolean): Joints {
        val hip = Offset(p.hx, p.hy)
        val t = dir(p.torso)
        val sho = Offset(hip.x + t.x * TORSO, hip.y + t.y * TORSO + p.sy)
        val neck = sho + t * NECK
        val head = neck + t * HEAD

        fun arm(from: Offset, ua: Float, fa: Float, scale: Float, mirror: Boolean): Pair<Offset, Offset> {
            var u = dir(ua); var f = dir(fa)
            if (mirror) { u = Offset(-u.x, u.y); f = Offset(-f.x, f.y) }
            val el = Offset(from.x + u.x * UPPER_ARM * scale, from.y + u.y * UPPER_ARM)
            return el to (el + f * FOREARM)
        }
        fun leg(from: Offset, th: Float, sh: Float, scale: Float, mirror: Boolean): Pair<Offset, Offset> {
            var u = dir(th); var f = dir(sh)
            if (mirror) { u = Offset(-u.x, u.y); f = Offset(-f.x, f.y) }
            val kn = from + u * (THIGH * scale)
            return kn to (kn + f * SHIN)
        }

        return if (!front) {
            val a1 = arm(sho, p.ua, p.fa, 1f, false)
            val a2 = arm(sho, p.ua2 ?: p.ua, p.fa2 ?: p.fa, 1f, false)
            val l1 = leg(hip, p.th, p.sh, 1f, false)
            val l2 = leg(hip, p.th2 ?: p.th, p.sh2 ?: p.sh, 1f, false)
            Joints(hip, sho, neck, head, listOf(sho to a1.first, sho to a2.first),
                listOf(a1.first, a2.first), listOf(a1.second, a2.second),
                listOf(hip, hip), listOf(l1.first, l2.first), listOf(l1.second, l2.second))
        } else {
            val shR = Offset(sho.x + SHOULDERS, sho.y); val shL = Offset(sho.x - SHOULDERS, sho.y)
            val hpR = Offset(hip.x + HIPS, hip.y); val hpL = Offset(hip.x - HIPS, hip.y)
            val aR = arm(shR, p.ua, p.fa, p.uas, false); val aL = arm(shL, p.ua, p.fa, p.uas, true)
            val lR = leg(hpR, p.th, p.sh, p.ths, false); val lL = leg(hpL, p.th, p.sh, p.ths, true)
            Joints(hip, sho, neck, head, listOf(shR to aR.first, shL to aL.first),
                listOf(aR.first, aL.first), listOf(aR.second, aL.second),
                listOf(hpR, hpL), listOf(lR.first, lL.first), listOf(lR.second, lL.second))
        }
    }
}
