package io.github.bornaradusin.trainingcoach.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The exercises the Ironman IM-309 can do, from assets/exercises.json. That file is made by
 * tools/animations/exercises.py; tools/animations/render.py draws every animation
 * the same way the app does, for checking.
 */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    /** press, high, low, mid, leg or bench */
    val station: String,
    val attachment: String,
    val muscles: List<String>,
    val secondary: List<String> = emptyList(),
    val setup: List<String>,
    val steps: List<String>,
    val tips: List<String> = emptyList(),
    /** Suggested reps, like "8-12". */
    val reps: String,
    val bodyweight: Boolean = false,
    /** Done one side at a time (reps are per side). */
    val unilateral: Boolean = false,
    val youtube: String,
    val anim: Anim,
)

/** How the exercise's animation moves, between two poses. Everything is in centimetres, to scale. */
@Serializable
data class Anim(
    /** side or front */
    val view: String,
    val start: Pose,
    val end: Pose,
    /** Pulley the cable runs from: HP (top), LP (low), MP (behind the backrest). */
    val cable: String? = null,
    /** What the cable or lever is attached to: hands, neck or ankle. */
    val attach: String = "hands",
    /** Moving machine parts to draw: press, pads, bar, rope, lever, heel. */
    val parts: List<String> = emptyList(),
    /** Body parts drawn as working: torso, ua, fa, th, sh. */
    val work: List<String> = emptyList(),
    /** How far (cm) the lifted plates rise. */
    val travel: Float = 25f,
    val barw: Float = 60f,
    /** Standing in front of the machine (front view): the whole machine is behind the person. */
    val infront: Boolean = false,
    val captions: Captions = Captions(),
    @SerialName("arrow_off") val arrowOff: List<Float> = listOf(0f, 0f),
)

@Serializable
data class Captions(val lift: String = "Lift", val lower: String = "Lower slowly")

/**
 * A pose: hip position (cm, x towards the back of the machine, y up from the floor) and the
 * angles of the body parts (0° towards the back, 90° up).
 */
@Serializable
data class Pose(
    val hx: Float, val hy: Float, val torso: Float,
    val ua: Float, val fa: Float,
    val th: Float = 270f, val sh: Float = 270f,
    val ua2: Float? = null, val fa2: Float? = null,
    val th2: Float? = null, val sh2: Float? = null,
    /** Which way the person faces in side view: -1 the front of the machine, 1 the back. */
    val face: Float = -1f,
    /** Head tilt. */
    val nk: Float = 0f,
    /** Front view: how much of the upper arm shows (it points towards or away from you when < 1). */
    val uas: Float = 1f,
    /** Front view: how much of the thigh and shin show (sitting: the thighs point at you). */
    val ths: Float = 1f,
    val shs: Float = 1f,
    /** Front view: knees out from the hips. */
    val kx: Float = 2f,
    /** Shoulders lifted (shrug). */
    val sy: Float = 0f,
)

/** The machine drawn to scale, from assets/exercises.json. */
@Serializable
data class MachineView(
    val pulleys: Map<String, List<Float>> = emptyMap(),
    val pivots: Map<String, List<Float>> = emptyMap(),
    val stack: PlateStack? = null,
    val rest: Map<String, List<Float>> = emptyMap(),
    val shapes: List<Shape> = emptyList(),
)

@Serializable
data class PlateStack(val x: Float, val y: Float, val w: Float, val plates: Int, val h: Float, val gap: Float)

/** rect, rrect, line or circle; l is the layer, back (behind the person) or front. */
@Serializable
data class Shape(
    val t: String,
    val x: Float = 0f, val y: Float = 0f, val w: Float = 0f, val h: Float = 0f, val r: Float = 0f,
    val a: List<Float>? = null, val b: List<Float>? = null,
    val c: String, val l: String = "back",
)

@Serializable
private data class CatalogFile(
    val machine: String,
    val side: MachineView,
    val front: MachineView,
    val exercises: List<Exercise>,
)

object Stations {
    val order = listOf("press", "high", "low", "mid", "leg", "bench")
    fun name(station: String) = when (station) {
        "press" -> "Press arms"
        "high" -> "Top pulley"
        "low" -> "Low pulley"
        "mid" -> "Backrest pulley"
        "leg" -> "Leg station"
        "bench" -> "Bodyweight"
        else -> station
    }
}

class Catalog(context: Context) {
    private val file = Json { ignoreUnknownKeys = true }
        .decodeFromString(CatalogFile.serializer(), context.assets.open("exercises.json").bufferedReader().use { it.readText() })
    val exercises: List<Exercise> = file.exercises
    /** The machine from the side and from the front, for the animations. */
    val side: MachineView = file.side
    val front: MachineView = file.front
    private val byId = exercises.associateBy { it.id }
    operator fun get(id: String) = byId[id]
}
