package io.github.bornaradusin.trainingcoach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bornaradusin.trainingcoach.R
import io.github.bornaradusin.trainingcoach.app
import io.github.bornaradusin.trainingcoach.data.Plan
import io.github.bornaradusin.trainingcoach.data.PlanDay
import io.github.bornaradusin.trainingcoach.data.PlanItem
import java.util.UUID

@Composable
fun PlansTab(nav: Nav) {
    val store = LocalContext.current.app.store
    val data by store.data.collectAsStateWithLifecycle()
    val plans = store.allPlans
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(plans, key = { it.id }) { plan ->
            val active = plan.id == data.activePlan
            Card(
                Modifier.fillMaxWidth().clickable { nav.open(Page.PlanInfo(plan.id)) },
                colors = if (active) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (active) Text("In use", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        else if (!plan.builtIn) Text("Yours", style = MaterialTheme.typography.labelMedium)
                    }
                    if (plan.description.isNotBlank()) Text(plan.description, style = MaterialTheme.typography.bodySmall)
                    Text(plan.days.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(onClick = { nav.open(Page.PlanEdit(null)) }, modifier = Modifier.fillMaxWidth()) { Text("Make your own plan") }
        }
    }
}

@Composable
fun PlanScreen(id: String, nav: Nav) {
    val context = LocalContext.current
    val store = context.app.store
    val catalog = context.app.catalog
    val data by store.data.collectAsStateWithLifecycle()
    val plan = store.plan(id)
    if (plan == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { nav.back() }
        return
    }
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        BackBar(plan.name, nav.back) {
            if (!plan.builtIn) TextButton(onClick = { nav.open(Page.PlanEdit(plan.id)) }) { Text("Edit") }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (plan.description.isNotBlank()) Text(plan.description)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plan.id != data.activePlan) Button(onClick = { store.usePlan(plan); nav.tab(Tab.TRAIN) }) { Text("Use this plan") }
                else Text("This is the plan you're using.", color = MaterialTheme.colorScheme.primary)
            }
            plan.days.forEachIndexed { d, day ->
                Section(day.name) {
                    day.items.forEach { item ->
                        val ex = catalog[item.exercise] ?: return@forEach
                        Row(Modifier.fillMaxWidth().clickable { nav.open(Page.ExerciseInfo(ex.id)) }.padding(vertical = 2.dp)) {
                            Text(ex.name, Modifier.weight(1f))
                            Text(repsText(item) + " · " + restText(item.restSec), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(enabled = data.current == null, onClick = { store.start(plan, d); nav.open(Page.Workout) }) { Text("Start ${day.name} now") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val copy = plan.copy(id = UUID.randomUUID().toString(), name = plan.name + " (mine)", builtIn = false)
                    store.savePlan(copy)
                    nav.open(Page.PlanEdit(copy.id))
                }) { Text("Copy and change") }
                if (!plan.builtIn) TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete plan?") },
            text = { Text("Workouts you've done with it stay in your history.") },
            confirmButton = { TextButton(onClick = { store.deletePlan(plan); confirmDelete = false; nav.back() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Making or changing your own plan: its days, and each day's exercises, sets, reps and rest. */
@Composable
fun PlanEditor(id: String?, nav: Nav) {
    val context = LocalContext.current
    val store = context.app.store
    val catalog = context.app.catalog
    val start = remember { store.plan(id)?.takeIf { !it.builtIn } ?: Plan(UUID.randomUUID().toString(), "My plan", "", listOf(PlanDay("Day 1", emptyList()))) }
    var name by remember { mutableStateOf(start.name) }
    var description by remember { mutableStateOf(start.description) }
    var days by remember { mutableStateOf(start.days) }
    var addingTo by remember { mutableStateOf<Int?>(null) }
    val rest = store.data.value.settings.restSec

    fun setDay(i: Int, day: PlanDay) { days = days.mapIndexed { j, d -> if (j == i) day else d } }

    Scaffold(topBar = {
        BackBar(if (id == null) "New plan" else "Edit plan", nav.back) {
            TextButton(enabled = name.isNotBlank() && days.any { it.items.isNotEmpty() }, onClick = {
                store.savePlan(start.copy(name = name.trim(), description = description.trim(), days = days.filter { it.items.isNotEmpty() }))
                nav.back()
            }) { Text("Save") }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Plan name") }, singleLine = true)
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Notes (optional)") })
            days.forEachIndexed { d, day ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(day.name, { setDay(d, day.copy(name = it)) }, Modifier.weight(1f), label = { Text("Day") }, singleLine = true)
                            if (days.size > 1) IconButton(onClick = { days = days.filterIndexed { j, _ -> j != d } }) { Icon(painterResource(R.drawable.ic_delete), "Remove day") }
                        }
                        day.items.forEachIndexed { i, item ->
                            ItemEditor(
                                catalog[item.exercise]?.name ?: item.exercise, item,
                                change = { n -> setDay(d, day.copy(items = day.items.mapIndexed { j, it -> if (j == i) n else it })) },
                                remove = { setDay(d, day.copy(items = day.items.filterIndexed { j, _ -> j != i })) },
                                up = if (i > 0) ({ setDay(d, day.copy(items = day.items.toMutableList().also { l -> l.add(i - 1, l.removeAt(i)) })) }) else null,
                            )
                        }
                        OutlinedButton(onClick = { addingTo = d }) { Text("Add exercise") }
                    }
                }
            }
            OutlinedButton(onClick = { days = days + PlanDay("Day ${days.size + 1}", emptyList()) }) { Text("Add day") }
        }
    }
    addingTo?.let { d ->
        ExercisePicker(onDismiss = { addingTo = null }) { ex ->
            val min = ex.reps.substringBefore('-').toIntOrNull() ?: 8
            val max = ex.reps.substringAfter('-').toIntOrNull() ?: 12
            setDay(d, days[d].copy(items = days[d].items + PlanItem(ex.id, 3, min, max, rest)))
            addingTo = null
        }
    }
}

@Composable
private fun ItemEditor(name: String, item: PlanItem, change: (PlanItem) -> Unit, remove: () -> Unit, up: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (up != null) IconButton(onClick = up) { Icon(painterResource(R.drawable.ic_up), "Move up") }
            IconButton(onClick = remove) { Icon(painterResource(R.drawable.ic_delete), "Remove") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallNumber("Sets", item.sets, 1..10) { change(item.copy(sets = it)) }
            SmallNumber("Min reps", item.repMin, 1..50) { change(item.copy(repMin = it, repMax = maxOf(it, item.repMax))) }
            SmallNumber("Max reps", item.repMax, 1..50) { change(item.copy(repMax = it, repMin = minOf(it, item.repMin))) }
            RestPicker(item.restSec) { change(item.copy(restSec = it)) }
        }
    }
}

@Composable
private fun SmallNumber(label: String, value: Int, range: IntRange, set: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Column(Modifier.clickable { open = true }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("$value", style = MaterialTheme.typography.titleMedium)
        }
        DropdownMenu(open, { open = false }) {
            range.forEach { v -> DropdownMenuItem(text = { Text("$v") }, onClick = { set(v); open = false }) }
        }
    }
}

val REST_CHOICES = listOf(30, 45, 60, 75, 90, 120, 150, 180, 240)

@Composable
fun RestPicker(value: Int, set: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Column(Modifier.clickable { open = true }.padding(4.dp).width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Rest", style = MaterialTheme.typography.labelSmall)
            Text(restText(value), style = MaterialTheme.typography.titleMedium)
        }
        DropdownMenu(open, { open = false }) {
            REST_CHOICES.forEach { v -> DropdownMenuItem(text = { Text(restText(v)) }, onClick = { set(v); open = false }) }
        }
    }
}
