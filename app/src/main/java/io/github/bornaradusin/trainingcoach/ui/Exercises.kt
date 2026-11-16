package io.github.bornaradusin.trainingcoach.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bornaradusin.trainingcoach.R
import io.github.bornaradusin.trainingcoach.anim.ExerciseAnimation
import io.github.bornaradusin.trainingcoach.app
import io.github.bornaradusin.trainingcoach.data.Exercise
import io.github.bornaradusin.trainingcoach.data.Stack
import io.github.bornaradusin.trainingcoach.data.Stations
import io.github.bornaradusin.trainingcoach.data.Store

@Composable
fun ExercisesTab(nav: Nav) {
    val catalog = LocalContext.current.app.catalog
    val groups = catalog.exercises.groupBy { it.station }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Everything your Ironman IM-309 can do, by station. Tap one to see how.", style = MaterialTheme.typography.bodyMedium)
        }
        Stations.order.forEach { station ->
            val list = groups[station].orEmpty()
            if (list.isEmpty()) return@forEach
            item(key = "h-$station") { Text(Stations.name(station), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(list, key = { it.id }) { ex -> ExerciseRow(ex) { nav.open(Page.ExerciseInfo(ex.id)) } }
        }
    }
}

@Composable
fun ExerciseRow(ex: Exercise, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseAnimation(ex, Modifier.size(64.dp), playing = false)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(ex.name, fontWeight = FontWeight.Medium)
                Text(ex.muscles.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseScreen(id: String, nav: Nav) {
    val context = LocalContext.current
    val ex = context.app.catalog[id] ?: return
    val store = context.app.store
    val data by store.data.collectAsStateWithLifecycle()
    val history = store.history(id)
    Scaffold(topBar = { BackBar(ex.name, nav.back) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.End) {
                    ExerciseAnimation(ex, Modifier.fillMaxWidth().padding(8.dp))
                    // Small, for anyone who wants to see a real person do it
                    TextButton(onClick = {
                        val url = "https://www.youtube.com/results?search_query=" + Uri.encode(ex.youtube)
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    }) {
                        Icon(painterResource(R.drawable.ic_youtube), null, Modifier.size(16.dp))
                        Text(" YouTube", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip({}, { Text(Stations.name(ex.station)) })
                AssistChip({}, { Text(ex.attachment) })
                AssistChip({}, { Text("${ex.reps} reps" + if (ex.unilateral) " each side" else "") })
            }
            // The same colours as the muscles in the animation
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(212, 48, 30))) { append("● ") }
                    append("Works: " + ex.muscles.joinToString(", "))
                })
                if (ex.secondary.isNotEmpty()) Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(240, 132, 66))) { append("● ") }
                    append("Also: " + ex.secondary.joinToString(", "))
                })
            }
            Section("Setting up") { ex.setup.forEachIndexed { i, s -> Text("${i + 1}. $s") } }
            Section("How to do it") { ex.steps.forEachIndexed { i, s -> Text("${i + 1}. $s") } }
            if (ex.tips.isNotEmpty()) Section("Tips") { ex.tips.forEach { Text("• $it") } }
            if (history.isNotEmpty()) {
                val best = history.maxBy { Store.oneRepMax(it.second, it.third) }
                Section("Your progress") {
                    Text(
                        if (ex.bodyweight) "Best: ${history.maxOf { it.third }} reps"
                        else "Best set: ${Stack.format(best.second, data.settings.units)} × ${best.third}",
                    )
                    Text("Done ${history.size} times, last on ${dateText(history.last().first)}", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { nav.open(Page.ExerciseProgress(id)) }) { Text("See progress") }
                }
            }
        }
    }
}

/** Picks an exercise, grouped by station. */
@Composable
fun ExercisePicker(onDismiss: () -> Unit, onPick: (Exercise) -> Unit) {
    val catalog = LocalContext.current.app.catalog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick an exercise") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                Stations.order.forEach { station ->
                    val list = catalog.exercises.filter { it.station == station }
                    if (list.isEmpty()) return@forEach
                    item(key = "h-$station") { Text(Stations.name(station), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                    items(list, key = { it.id }) { ex ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExerciseAnimation(ex, Modifier.size(40.dp), playing = false)
                            Text(ex.name, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
