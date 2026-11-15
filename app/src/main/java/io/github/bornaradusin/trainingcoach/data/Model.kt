package io.github.bornaradusin.trainingcoach.data

import kotlinx.serialization.Serializable

/** One exercise in a plan's day: how many sets, the rep range and the rest between sets. */
@Serializable
data class PlanItem(
    val exercise: String,
    val sets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
    val restSec: Int = 90,
)

@Serializable
data class PlanDay(val name: String, val items: List<PlanItem>)

@Serializable
data class Plan(
    val id: String,
    val name: String,
    val description: String = "",
    val days: List<PlanDay>,
    /** Comes with the app (can't be changed, but can be copied). */
    val builtIn: Boolean = false,
)

/** A set done: the pin's place in the stack (1 = top plate; 0 = bodyweight) and the reps. */
@Serializable
data class SetLog(val pin: Int, val kg: Float, val reps: Int)

@Serializable
data class ExerciseLog(val exercise: String, val sets: List<SetLog> = emptyList(), val skipped: Boolean = false)

@Serializable
data class Session(
    val id: String,
    val start: Long,
    val end: Long? = null,
    val planId: String? = null,
    val planName: String = "",
    val dayIndex: Int = 0,
    val dayName: String = "",
    /** What was planned, in order. */
    val items: List<PlanItem> = emptyList(),
    val logs: List<ExerciseLog> = emptyList(),
)

enum class Units { KG, LB }

@Serializable
data class Settings(
    val units: Units = Units.KG,
    /** The weight at each pin position, top plate first (kg). */
    val stack: List<Float> = Stack.IM309,
    val restSec: Int = 90,
    val vibrate: Boolean = true,
    val sound: Boolean = true,
    val keepScreenOn: Boolean = true,
)

@Serializable
data class Data(
    val settings: Settings = Settings(),
    val plans: List<Plan> = emptyList(),
    val activePlan: String? = null,
    /** The day of the active plan to do next. */
    val nextDay: Int = 0,
    val sessions: List<Session> = emptyList(),
    /** A workout under way, kept so it survives closing the app. */
    val current: Session? = null,
)

object Stack {
    /** IM-309: a 3.5 kg top plate, then 11 plates of 5.7 kg (145 lb in all). */
    val IM309: List<Float> = List(12) { i -> 3.5f + 5.7f * i }

    fun kgToLb(kg: Float) = kg * 2.20462f

    fun format(kg: Float, units: Units): String {
        val v = if (units == Units.LB) kgToLb(kg) else kg
        val s = if (v % 1f == 0f) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
        return s + if (units == Units.LB) " lb" else " kg"
    }
}
