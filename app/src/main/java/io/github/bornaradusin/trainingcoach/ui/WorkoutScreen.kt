package io.github.bornaradusin.trainingcoach.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bornaradusin.trainingcoach.R
import io.github.bornaradusin.trainingcoach.anim.ExerciseAnimation
import io.github.bornaradusin.trainingcoach.app
import io.github.bornaradusin.trainingcoach.data.PlanItem
import io.github.bornaradusin.trainingcoach.data.SetLog
import io.github.bornaradusin.trainingcoach.data.Stack
import kotlinx.coroutines.delay

/** The workout under way: one exercise at a time, the pin to use, logging sets and resting. */
@Composable
fun WorkoutScreen(nav: Nav) {
    val context = LocalContext.current
    val store = context.app.store
    val catalog = context.app.catalog
    val data by store.data.collectAsStateWithLifecycle()
    val session = data.current
    if (session == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val settings = data.settings

    // Keep the screen on while training
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var index by rememberSaveable { mutableIntStateOf(session.logs.indexOfFirst { it.sets.isEmpty() && !it.skipped }.coerceAtLeast(0)) }
    index = index.coerceIn(0, (session.items.size - 1).coerceAtLeast(0))
    var restEnd by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmFinish by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<Int?>(null) }   // -1: add, else swap that index
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(250) } }

    // Rest is over: buzz and beep
    var alerted by remember { mutableLongStateOf(0L) }
    if (restEnd in 1..now && alerted != restEnd) {
        alerted = restEnd
        LaunchedEffect(restEnd) { restOver(context, settings.vibrate, settings.sound) }
    }

    Scaffold(
        topBar = {
            BackBar(session.dayName.ifBlank { "Workout" } + " · " + clock(now - session.start), nav.back) {
                TextButton(onClick = { confirmFinish = true }) { Text("Finish") }
            }
        },
    ) { padding ->
        if (session.items.isEmpty()) {
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text("No exercises in this workout.")
                Button(onClick = { picking = -1 }) { Text("Add an exercise") }
            }
            return@Scaffold
        }
        val item = session.items[index]
        val log = session.logs[index]
        val ex = catalog[item.exercise]
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Exercises of the workout, done ones ticked
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                session.items.forEachIndexed { i, it ->
                    val l = session.logs[i]
                    val mark = when {
                        l.skipped -> "– "
                        l.sets.size >= it.sets -> "✓ "
                        l.sets.isNotEmpty() -> "${l.sets.size}/${it.sets} "
                        else -> ""
                    }
                    FilterChip(i == index, { index = i }, { Text(mark + (catalog[it.exercise]?.name ?: it.exercise), maxLines = 1) })
                }
                FilterChip(false, { picking = -1 }, { Text("+ Add") })
            }
            if (ex == null) return@Column

            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseAnimation(ex, Modifier.size(150.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ex.name, style = MaterialTheme.typography.titleLarge)
                        Text(ex.attachment, style = MaterialTheme.typography.bodySmall)
                        Text("Target: ${repsText(item)}" + if (ex.unilateral) " each side" else "", fontWeight = FontWeight.Medium)
                        Row {
                            TextButton(onClick = { nav.open(Page.ExerciseInfo(ex.id)) }) { Text("How to") }
                            TextButton(onClick = { picking = index }) { Text("Swap") }
                        }
                    }
                }
            }

            // Rest timer
            val left = ((restEnd - now) / 1000L).toInt()
            if (restEnd > 0 && left >= 0) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rest", style = MaterialTheme.typography.titleMedium)
                        Text("${left / 60}:${"%02d".format(left % 60)}", style = MaterialTheme.typography.displayMedium)
                        LinearProgressIndicator(progress = { 1f - left / item.restSec.toFloat().coerceAtLeast(1f) }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { restEnd += 30_000L }) { Text("+30 s") }
                            TextButton(onClick = { restEnd = 0L }) { Text("Skip rest") }
                        }
                    }
                }
            }

            // Sets logged
            if (log.sets.isNotEmpty()) {
                Section("Sets") {
                    log.sets.forEachIndexed { i, s ->
                        Row {
                            Text("Set ${i + 1}", Modifier.width(64.dp))
                            Text(if (s.pin == 0) "Bodyweight" else "Pin ${s.pin} · " + Stack.format(s.kg, settings.units), Modifier.weight(1f))
                            Text("× ${s.reps}", fontWeight = FontWeight.Medium)
                        }
                    }
                    TextButton(onClick = { store.undoSet(index) }) { Text("Undo last set") }
                }
            }

            SetEntry(item, ex.bodyweight, log.sets, index, settings.units) { set ->
                store.logSet(index, set)
                val doneNow = log.sets.size + 1 >= item.sets
                restEnd = System.currentTimeMillis() + item.restSec * 1000L
                if (doneNow) {
                    // On to the next exercise not done yet
                    val next = session.items.indices.firstOrNull { i -> i > index && session.logs[i].sets.size < session.items[i].sets && !session.logs[i].skipped }
                    if (next != null) index = next
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (index > 0) index-- }, enabled = index > 0) { Text("Previous") }
                OutlinedButton(onClick = { store.skip(index, !log.skipped); if (!log.skipped && index < session.items.size - 1) index++ }) {
                    Text(if (log.skipped) "Unskip" else "Skip")
                }
                OutlinedButton(onClick = { if (index < session.items.size - 1) index++ }, enabled = index < session.items.size - 1) { Text("Next") }
            }
            if (session.logs.indices.all { i -> session.logs[i].skipped || session.logs[i].sets.size >= session.items[i].sets }) {
                Button(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth()) { Text("All done: finish workout") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmFinish) {
        val sets = session.logs.sumOf { it.sets.size }
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish workout?") },
            text = { Text(if (sets == 0) "No sets were logged, so nothing will be saved." else "$sets sets in ${clock(now - session.start)} will be saved.") },
            confirmButton = { TextButton(onClick = { confirmFinish = false; store.finish(); nav.back() }) { Text("Finish") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep training") } },
        )
    }
    picking?.let { at ->
        ExercisePicker(onDismiss = { picking = null }) { picked ->
            if (at < 0) {
                store.addToWorkout(PlanItem(picked.id, 3, picked.reps.substringBefore('-').toIntOrNull() ?: 8, picked.reps.substringAfter('-').toIntOrNull() ?: 12, settings.restSec))
                index = session.items.size
            } else {
                store.swap(at, picked.id)
            }
            picking = null
        }
    }
}

/** Choosing the pin and the reps, and logging the set. */
@Composable
private fun SetEntry(item: PlanItem, bodyweight: Boolean, done: List<SetLog>, index: Int, units: io.github.bornaradusin.trainingcoach.data.Units, log: (SetLog) -> Unit) {
    val store = LocalContext.current.app.store
    val suggestion = remember(item.exercise, index) { store.suggest(item, bodyweight) }
    val stack = store.data.value.settings.stack
    var pin by remember(item.exercise, index) { mutableIntStateOf(done.lastOrNull()?.pin ?: suggestion.pin) }
    var reps by remember(item.exercise, index) { mutableIntStateOf(done.lastOrNull()?.reps ?: item.repMax) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Set ${done.size + 1} of ${item.sets}", style = MaterialTheme.typography.titleMedium)
            if (!bodyweight) {
                if (done.isEmpty()) Text(suggestion.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Stepper(
                    label = "Pin",
                    value = "Pin $pin",
                    sub = Stack.format(stack.getOrElse(pin - 1) { 0f }, units),
                    minus = { if (pin > 1) pin-- },
                    plus = { if (pin < stack.size) pin++ },
                )
            }
            Stepper(label = "Reps", value = "$reps", sub = "target ${item.repMin}–${item.repMax}", minus = { if (reps > 0) reps-- }, plus = { reps++ })
            Button(
                onClick = { log(SetLog(if (bodyweight) 0 else pin, if (bodyweight) 0f else stack.getOrElse(pin - 1) { 0f }, reps)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Log set", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
fun Stepper(label: String, value: String, sub: String, minus: () -> Unit, plus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = minus, modifier = Modifier.size(56.dp)) { Icon(painterResource(R.drawable.ic_minus), "Less $label") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = plus, modifier = Modifier.size(56.dp)) { Icon(painterResource(R.drawable.ic_plus), "More $label") }
    }
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s >= 3600) "${s / 3600}:${"%02d".format((s / 60) % 60)}:${"%02d".format(s % 60)}" else "${s / 60}:${"%02d".format(s % 60)}"
}

private fun restOver(context: Context, vibrate: Boolean, sound: Boolean) {
    if (vibrate) runCatching {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    }
    if (sound) runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 800)
    }
}

