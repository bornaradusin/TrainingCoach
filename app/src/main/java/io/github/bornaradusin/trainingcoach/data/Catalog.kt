package io.github.bornaradusin.trainingcoach.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The exercises the Ironman IM-309 can do, from assets/exercises.json. That file is made by
 * tools/animations/exercises.py, which also draws every animation for checking.
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

/** How the exercise's animation moves, between two poses. */
@Serializable
data class Anim(
    /** side or front */
    val view: String,
    val start: Pose,
    val end: Pose,
    /** Pulley the cable runs from: HP (high), LP (low), MP (behind the backrest). */
    val cable: String? = null,
    /** What the cable is clipped to: hands, neck or ankle. */
    val attach: String = "hands",
    /** The press arms' lever from their pivot to the hands. */
    val lever: String? = null,
    val pads: Boolean = false,
    val bar: Boolean = false,
    val barw: Float = 24f,
    val roller: String? = null,
    val pivot: String? = null,
)

/** A pose of the figure: hip position and the angles of its parts (0° right, 90° down). */
@Serializable
data class Pose(
    val hx: Float, val hy: Float, val torso: Float,
    val ua: Float = 90f, val fa: Float = 90f,
    val th: Float = 90f, val sh: Float = 90f,
    val ua2: Float? = null, val fa2: Float? = null,
    val th2: Float? = null, val sh2: Float? = null,
    /** Front view: how much of the upper arm shows (it points away when < 1). */
    val uas: Float = 1f,
    val ths: Float = 1f,
    /** Shoulders lifted (shrug). */
    val sy: Float = 0f,
)

@Serializable
private data class CatalogFile(val machine: String, val exercises: List<Exercise>)

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
    val exercises: List<Exercise> = Json { ignoreUnknownKeys = true }
        .decodeFromString(CatalogFile.serializer(), context.assets.open("exercises.json").bufferedReader().use { it.readText() })
        .exercises
    private val byId = exercises.associateBy { it.id }
    operator fun get(id: String) = byId[id]
}
