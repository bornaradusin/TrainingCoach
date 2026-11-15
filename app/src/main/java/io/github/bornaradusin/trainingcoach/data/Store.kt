package io.github.bornaradusin.trainingcoach.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** What the pin should be next time, and why. */
data class Suggestion(val pin: Int, val reason: String)

/**
 * Everything the app keeps: settings, your own plans, the workouts you've done and the one under
 * way. Saved to one file in the app's own storage, one save at a time and in order.
 */
class Store(context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(context.filesDir, "training.json")
    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val lock = Any()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<Data> = _data.asStateFlow()

    private fun load(): Data =
        runCatching { json.decodeFromString(Data.serializer(), file.readText()) }.getOrNull() ?: Data(activePlan = "beginner")

    private fun update(change: (Data) -> Data): Data = synchronized(lock) {
        val next = change(_data.value)
        if (next != _data.value) {
            _data.value = next
            val text = json.encodeToString(Data.serializer(), next)
            writer.launch {
                runCatching {
                    val temp = File(file.parentFile, "training.json.tmp")
                    temp.writeText(text)
                    temp.renameTo(file)
                }
            }
        }
        next
    }

    // ---- plans ----

    val allPlans: List<Plan> get() = BuiltInPlans.all + _data.value.plans

    fun plan(id: String?) = allPlans.firstOrNull { it.id == id }

    fun activePlan(): Plan? = plan(_data.value.activePlan)

    fun usePlan(plan: Plan) = update { it.copy(activePlan = plan.id, nextDay = 0) }

    fun savePlan(plan: Plan) = update { d ->
        val p = plan.copy(builtIn = false)
        d.copy(plans = if (d.plans.any { it.id == p.id }) d.plans.map { if (it.id == p.id) p else it } else d.plans + p)
    }

    fun deletePlan(plan: Plan) = update { d ->
        d.copy(plans = d.plans.filterNot { it.id == plan.id }, activePlan = if (d.activePlan == plan.id) "beginner" else d.activePlan)
    }

    fun setNextDay(day: Int) = update { it.copy(nextDay = day) }

    // ---- settings ----

    fun setSettings(change: (Settings) -> Settings) = update { it.copy(settings = change(it.settings)) }

    fun kg(pin: Int): Float = _data.value.settings.stack.getOrElse(pin - 1) { 0f }

    // ---- a workout ----

    fun start(plan: Plan?, day: Int) = update { d ->
        val pd = plan?.days?.getOrNull(day)
        d.copy(current = Session(
            id = UUID.randomUUID().toString(),
            start = System.currentTimeMillis(),
            planId = plan?.id,
            planName = plan?.name.orEmpty(),
            dayIndex = day,
            dayName = pd?.name.orEmpty(),
            items = pd?.items.orEmpty(),
            logs = pd?.items.orEmpty().map { ExerciseLog(it.exercise) },
        ))
    }

    /** A workout of just the exercises you pick. */
    fun startFree(items: List<PlanItem>) = update { d ->
        d.copy(current = Session(
            id = UUID.randomUUID().toString(), start = System.currentTimeMillis(), dayName = "Free workout",
            items = items, logs = items.map { ExerciseLog(it.exercise) },
        ))
    }

    fun logSet(index: Int, set: SetLog) = updateLog(index) { it.copy(sets = it.sets + set, skipped = false) }

    fun undoSet(index: Int) = updateLog(index) { it.copy(sets = it.sets.dropLast(1)) }

    fun skip(index: Int, skipped: Boolean) = updateLog(index) { it.copy(skipped = skipped) }

    /** Swaps an exercise in the workout under way for another. */
    fun swap(index: Int, exercise: String) = update { d ->
        val c = d.current ?: return@update d
        d.copy(current = c.copy(
            items = c.items.mapIndexed { i, it -> if (i == index) it.copy(exercise = exercise) else it },
            logs = c.logs.mapIndexed { i, it -> if (i == index) ExerciseLog(exercise) else it },
        ))
    }

    fun addToWorkout(item: PlanItem) = update { d ->
        val c = d.current ?: return@update d
        d.copy(current = c.copy(items = c.items + item, logs = c.logs + ExerciseLog(item.exercise)))
    }

    private fun updateLog(index: Int, change: (ExerciseLog) -> ExerciseLog) = update { d ->
        val c = d.current ?: return@update d
        d.copy(current = c.copy(logs = c.logs.mapIndexed { i, l -> if (i == index) change(l) else l }))
    }

    /** Saves the workout; the plan moves on to its next day. */
    fun finish() = update { d ->
        val c = d.current ?: return@update d
        val done = c.copy(end = System.currentTimeMillis(), logs = c.logs.filter { it.sets.isNotEmpty() })
        val plan = plan(c.planId)
        d.copy(
            current = null,
            sessions = if (done.logs.isEmpty()) d.sessions else d.sessions + done,
            nextDay = if (plan != null && c.planId == d.activePlan) (c.dayIndex + 1) % plan.days.size else d.nextDay,
        )
    }

    fun discard() = update { it.copy(current = null) }

    fun deleteSession(id: String) = update { d -> d.copy(sessions = d.sessions.filterNot { it.id == id }) }

    // ---- history ----

    /** The last time an exercise was done (not counting the workout under way). */
    fun last(exercise: String): ExerciseLog? =
        _data.value.sessions.asReversed().asSequence()
            .mapNotNull { s -> s.logs.firstOrNull { it.exercise == exercise && it.sets.isNotEmpty() } }
            .firstOrNull()

    /**
     * Double progression: hit the top of the rep range on every set and the pin moves down a plate;
     * miss the bottom on every set and it moves back up one.
     */
    fun suggest(item: PlanItem, bodyweight: Boolean): Suggestion {
        if (bodyweight) return Suggestion(0, "Bodyweight")
        val stackSize = _data.value.settings.stack.size
        val last = last(item.exercise) ?: return Suggestion(2, "First time: start light and find your weight")
        val pin = last.sets.maxOf { it.pin }.coerceAtLeast(1)
        val top = last.sets.filter { it.pin == pin }
        return when {
            top.size >= minOf(item.sets, last.sets.size) && top.all { it.reps >= item.repMax } && pin < stackSize ->
                Suggestion(pin + 1, "You hit ${item.repMax} reps on every set last time: one plate more")
            top.isNotEmpty() && top.all { it.reps < item.repMin } && pin > 1 ->
                Suggestion(pin - 1, "Last time was below ${item.repMin} reps: one plate less")
            else -> Suggestion(pin, "Same as last time; aim for ${item.repMax} reps on every set")
        }
    }

    /** Best set per workout for an exercise, oldest first: (time, kg, reps). */
    fun history(exercise: String): List<Triple<Long, Float, Int>> =
        _data.value.sessions.mapNotNull { s ->
            val log = s.logs.firstOrNull { it.exercise == exercise && it.sets.isNotEmpty() } ?: return@mapNotNull null
            val best = log.sets.maxWith(compareBy<SetLog>({ it.kg }, { it.reps }))
            Triple(s.start, best.kg, best.reps)
        }

    companion object {
        /** Estimated one-rep max (Epley). */
        fun oneRepMax(kg: Float, reps: Int) = if (reps <= 1) kg else kg * (1f + reps / 30f)
    }
}
