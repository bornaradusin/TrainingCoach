package io.github.bornaradusin.trainingcoach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.bornaradusin.trainingcoach.R
import io.github.bornaradusin.trainingcoach.data.PlanItem
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, back: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = { IconButton(onClick = back) { Icon(painterResource(R.drawable.ic_back), "Back") } },
        actions = { actions() },
    )
}

@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

fun repsText(item: PlanItem) = "${item.sets} × " + if (item.repMin == item.repMax) "${item.repMin}" else "${item.repMin}–${item.repMax}"

fun restText(sec: Int) = if (sec >= 60 && sec % 60 == 0) "${sec / 60} min" else if (sec > 60) "${sec / 60}:${"%02d".format(sec % 60)}" else "$sec s"

fun dateText(t: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(t))

fun dateTimeText(t: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(t))

fun durationText(ms: Long): String {
    val m = ms / 60000
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}
