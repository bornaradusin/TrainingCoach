package io.github.bornaradusin.trainingcoach.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bornaradusin.trainingcoach.app
import io.github.bornaradusin.trainingcoach.data.Stack
import io.github.bornaradusin.trainingcoach.data.Store
import io.github.bornaradusin.trainingcoach.data.Units

@Composable
fun ProgressTab(nav: Nav) {
    val context = LocalContext.current
    val store = context.app.store
    val catalog = context.app.catalog
    val data by store.data.collectAsStateWithLifecycle()
    val units = data.settings.units
    val done = catalog.exercises.filter { ex -> data.sessions.any { s -> s.logs.any { it.exercise == ex.id && it.sets.isNotEmpty() } } }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Section("All time") {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat("${data.sessions.size}", "workouts")
                    Stat("${data.sessions.sumOf { s -> s.logs.sumOf { it.sets.size } }}", "sets")
                    val tonnage = data.sessions.sumOf { s -> s.logs.sumOf { l -> l.sets.sumOf { (it.kg * it.reps).toDouble() } } }
                    Stat(
                        if (units == Units.KG) String.format(java.util.Locale.US, "%.1f t", tonnage / 1000.0)
                        else String.format(java.util.Locale.US, "%.1fk lb", tonnage * 2.20462 / 1000.0),
                        "lifted",
                    )
                }
            }
        }
        if (data.sessions.isEmpty()) {
            item { Text("Your progress shows here after your first workout.", style = MaterialTheme.typography.bodyMedium) }
        }
        if (done.isNotEmpty()) item { Text("Exercises", style = MaterialTheme.typography.titleMedium) }
        items(done, key = { it.id }) { ex ->
            val h = store.history(ex.id)
            val best = h.maxBy { Store.oneRepMax(it.second, it.third) }
            Card(Modifier.fillMaxWidth().clickable { nav.open(Page.ExerciseProgress(ex.id)) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(ex.name, fontWeight = FontWeight.Medium)
                    Text(
                        (if (ex.bodyweight) "Best: ${h.maxOf { it.third }} reps" else "Best: ${Stack.format(best.second, units)} × ${best.third}") +
                            " · ${h.size} workouts · last ${dateText(h.last().first)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (!ex.bodyweight && h.size >= 2) Chart(h.map { it.second }, Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp))
                }
            }
        }
        if (data.sessions.isNotEmpty()) item { Text("Workouts", style = MaterialTheme.typography.titleMedium) }
        items(data.sessions.asReversed(), key = { "s-" + it.id }) { s ->
            Card(Modifier.fillMaxWidth().clickable { nav.open(Page.SessionInfo(s.id)) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.dayName.ifBlank { "Workout" }, fontWeight = FontWeight.Medium)
                    Text(dateTimeText(s.start) + " · ${s.logs.sumOf { it.sets.size }} sets", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ExerciseProgressScreen(id: String, nav: Nav) {
    val context = LocalContext.current
    val store = context.app.store
    val ex = context.app.catalog[id] ?: return
    val data by store.data.collectAsStateWithLifecycle()
    val units = data.settings.units
    val h = store.history(id)
    Scaffold(topBar = { BackBar(ex.name, nav.back) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (h.isEmpty()) { Text("Not done yet."); return@Column }
            if (ex.bodyweight) {
                Section("Reps per workout (best set)") { Chart(h.map { it.third.toFloat() }, Modifier.fillMaxWidth().height(180.dp)) }
            } else {
                Section("Weight (best set)") {
                    Chart(h.map { if (units == Units.LB) Stack.kgToLb(it.second) else it.second }, Modifier.fillMaxWidth().height(180.dp))
                    Text("From ${Stack.format(h.first().second, units)} to ${Stack.format(h.last().second, units)}", style = MaterialTheme.typography.bodySmall)
                }
                Section("Estimated one-rep max") {
                    val orm = h.map { Store.oneRepMax(it.second, it.third) }
                    Chart(orm.map { if (units == Units.LB) Stack.kgToLb(it) else it }, Modifier.fillMaxWidth().height(140.dp))
                    Text("Now about ${Stack.format(orm.last(), units)}; best ${Stack.format(orm.max(), units)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Section("Every workout") {
                data.sessions.asReversed().forEach { s ->
                    val log = s.logs.firstOrNull { it.exercise == id && it.sets.isNotEmpty() } ?: return@forEach
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(dateText(s.start), fontWeight = FontWeight.Medium)
                        Text(log.sets.joinToString("   ") { if (it.pin == 0) "${it.reps}" else "${Stack.format(it.kg, units)} × ${it.reps}" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionScreen(id: String, nav: Nav) {
    val context = LocalContext.current
    val store = context.app.store
    val catalog = context.app.catalog
    val data by store.data.collectAsStateWithLifecycle()
    val s = data.sessions.firstOrNull { it.id == id }
    if (s == null) { LaunchedEffect(Unit) { nav.back() }; return }
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(topBar = { BackBar(s.dayName.ifBlank { "Workout" }, nav.back) { TextButton(onClick = { confirmDelete = true }) { Text("Delete") } } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(dateTimeText(s.start) + (s.end?.let { " · " + durationText(it - s.start) } ?: "") + if (s.planName.isNotBlank()) " · ${s.planName}" else "")
            s.logs.forEach { log ->
                Section(catalog[log.exercise]?.name ?: log.exercise) {
                    log.sets.forEachIndexed { i, set ->
                        Row {
                            Text("Set ${i + 1}", Modifier.width(64.dp))
                            Text(if (set.pin == 0) "Bodyweight" else "Pin ${set.pin} · " + Stack.format(set.kg, data.settings.units), Modifier.weight(1f))
                            Text("× ${set.reps}", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this workout?") },
            confirmButton = { TextButton(onClick = { store.deleteSession(s.id); confirmDelete = false; nav.back() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** A simple line chart of values in order, with a dot on each. */
@Composable
fun Chart(values: List<Float>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val min = values.min(); val max = values.max()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val pad = 6.dp.toPx()
        fun pt(i: Int, v: Float) = Offset(
            pad + (size.width - 2 * pad) * (if (values.size == 1) 0.5f else i / (values.size - 1f)),
            pad + (size.height - 2 * pad) * (1f - (v - min) / span),
        )
        drawLine(grid, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f))
        val path = Path()
        values.forEachIndexed { i, v -> val p = pt(i, v); if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        values.forEachIndexed { i, v -> drawCircle(line, 3.dp.toPx(), pt(i, v)) }
    }
}
