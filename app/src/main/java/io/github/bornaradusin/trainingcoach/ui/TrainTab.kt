package io.github.bornaradusin.trainingcoach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bornaradusin.trainingcoach.app
import java.util.Calendar

@Composable
fun TrainTab(nav: Nav) {
    val context = LocalContext.current
    val store = context.app.store
    val catalog = context.app.catalog
    val data by store.data.collectAsStateWithLifecycle()
    val plan = store.plan(data.activePlan)
    var confirmDiscard by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        data.current?.let { c ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Workout under way", style = MaterialTheme.typography.titleMedium)
                        val done = c.logs.count { it.sets.isNotEmpty() }
                        Text("${c.dayName.ifBlank { "Workout" }} · $done of ${c.items.size} exercises started")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { nav.open(Page.Workout) }) { Text("Continue") }
                            TextButton(onClick = { confirmDiscard = true }) { Text("Discard") }
                        }
                    }
                }
            }
        }
        item {
            Section(if (plan == null) "No plan chosen" else plan.name) {
                if (plan == null) {
                    Text("Pick a plan to train with.")
                    Button(onClick = { nav.tab(Tab.PLANS) }) { Text("Choose a plan") }
                    return@Section
                }
                val day = data.nextDay.coerceIn(0, plan.days.size - 1)
                if (plan.days.size > 1) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        plan.days.forEachIndexed { i, d ->
                            FilterChip(i == day, { store.setNextDay(i) }, { Text(d.name) })
                        }
                    }
                }
                val pd = plan.days[day]
                Text("Next: ${pd.name}", style = MaterialTheme.typography.titleSmall)
                pd.items.forEach { item ->
                    val ex = catalog[item.exercise] ?: return@forEach
                    Row(Modifier.fillMaxWidth().clickable { nav.open(Page.ExerciseInfo(ex.id)) }.padding(vertical = 2.dp)) {
                        Text(ex.name, Modifier.weight(1f))
                        Text(repsText(item), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        enabled = data.current == null,
                        onClick = { store.start(plan, day); nav.open(Page.Workout) },
                    ) { Text("Start ${pd.name}") }
                    TextButton(onClick = { nav.tab(Tab.PLANS) }) { Text("Change plan") }
                }
            }
        }
        item {
            val now = Calendar.getInstance()
            val weekStart = (now.clone() as Calendar).apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }.timeInMillis
            val thisWeek = data.sessions.count { it.start >= weekStart }
            val sets = data.sessions.filter { it.start >= weekStart }.sumOf { s -> s.logs.sumOf { it.sets.size } }
            Section("This week") {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat("$thisWeek", if (thisWeek == 1) "workout" else "workouts")
                    Stat("$sets", "sets")
                    Stat("${data.sessions.size}", "in all")
                }
            }
        }
        if (data.sessions.isNotEmpty()) {
            item { Text("Recent workouts", style = MaterialTheme.typography.titleMedium) }
            items(data.sessions.asReversed().take(5), key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().clickable { nav.open(Page.SessionInfo(s.id)) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(s.dayName.ifBlank { "Workout" } + if (s.planName.isNotBlank()) " · ${s.planName}" else "", fontWeight = FontWeight.Medium)
                        Text(
                            dateText(s.start) + " · ${s.logs.size} exercises · ${s.logs.sumOf { it.sets.size }} sets" +
                                (s.end?.let { " · " + durationText(it - s.start) } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { nav.tab(Tab.EXERCISES) }, modifier = Modifier.fillMaxWidth()) { Text("See all exercises for your machine") }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard workout?") },
            text = { Text("The sets logged in this workout won't be saved.") },
            confirmButton = { TextButton(onClick = { store.discard(); confirmDiscard = false }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep") } },
        )
    }
}

@Composable
fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
