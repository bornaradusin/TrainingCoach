package io.github.bornaradusin.trainingcoach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bornaradusin.trainingcoach.BuildConfig
import io.github.bornaradusin.trainingcoach.app
import io.github.bornaradusin.trainingcoach.data.AppUpdate
import io.github.bornaradusin.trainingcoach.data.Stack
import io.github.bornaradusin.trainingcoach.data.Units
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab() {
    val store = LocalContext.current.app.store
    val data by store.data.collectAsStateWithLifecycle()
    val s = data.settings
    var editingStack by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section("Units") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(s.units == Units.KG, { store.setSettings { it.copy(units = Units.KG) } }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("kg") }
                SegmentedButton(s.units == Units.LB, { store.setSettings { it.copy(units = Units.LB) } }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("lb") }
            }
        }
        Section("Weight stack") {
            Text("Ironman IM-309: a 3.5 kg top plate and 11 plates of 5.7 kg. The pin in the top plate lifts the first weight; each plate down adds one plate.", style = MaterialTheme.typography.bodySmall)
            Text(s.stack.mapIndexed { i, kg -> "${i + 1}: ${Stack.format(kg, s.units)}" }.joinToString("   "), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editingStack = !editingStack }) { Text(if (editingStack) "Done" else "Change weights") }
                if (s.stack != Stack.IM309) TextButton(onClick = { store.setSettings { it.copy(stack = Stack.IM309) } }) { Text("Back to IM-309") }
            }
            if (editingStack) {
                s.stack.forEachIndexed { i, kg ->
                    var text by remember(i, kg) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", kg)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pin ${i + 1}", Modifier.width(64.dp))
                        OutlinedTextField(
                            text,
                            { v ->
                                text = v
                                v.replace(',', '.').toFloatOrNull()?.let { n -> store.setSettings { st -> st.copy(stack = st.stack.mapIndexed { j, x -> if (j == i) n else x }) } }
                            },
                            Modifier.weight(1f),
                            singleLine = true,
                            suffix = { Text("kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                }
            }
        }
        Section("Rest timer") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rest for exercises you add to a workout", Modifier.weight(1f))
                RestPicker(s.restSec) { v -> store.setSettings { it.copy(restSec = v) } }
            }
            Toggle("Vibrate when rest is over", s.vibrate) { v -> store.setSettings { it.copy(vibrate = v) } }
            Toggle("Beep when rest is over", s.sound) { v -> store.setSettings { it.copy(sound = v) } }
            Toggle("Keep the screen on during a workout", s.keepScreenOn) { v -> store.setSettings { it.copy(keepScreenOn = v) } }
        }
        UpdateCard()
        Text("Training Coach ${BuildConfig.VERSION_NAME} · for the Ironman IM-309", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val updates = context.app.updates
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<AppUpdate?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Updates", style = MaterialTheme.typography.titleMedium)
            status?.let { Text(it) }
            progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
            val update = found
            if (update == null) {
                OutlinedButton(onClick = {
                    status = "Checking…"
                    scope.launch {
                        status = runCatching { updates.check() }.fold(
                            onSuccess = { u -> found = u; if (u == null) "You have the newest version" else "Version ${u.version} is out" },
                            onFailure = { "Couldn't check: ${it.message}" },
                        )
                    }
                }) { Text("Check for updates") }
            } else {
                if (update.notes.isNotBlank()) Text(update.notes, style = MaterialTheme.typography.bodySmall)
                Button(enabled = progress == null, onClick = {
                    if (!updates.canInstall()) {
                        context.startActivity(updates.installPermission())
                        return@Button
                    }
                    scope.launch {
                        progress = 0f
                        runCatching { updates.download(update) { p -> progress = p } }
                            .onSuccess { updates.install(it) }
                            .onFailure { status = "Download failed: ${it.message}" }
                        progress = null
                    }
                }) { Text("Download and install ${update.version}") }
            }
        }
    }
}

