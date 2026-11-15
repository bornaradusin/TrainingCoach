package io.github.bornaradusin.trainingcoach.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.bornaradusin.trainingcoach.R

enum class Tab(val label: String, val icon: Int) {
    TRAIN("Train", R.drawable.ic_train),
    EXERCISES("Exercises", R.drawable.ic_exercises),
    PLANS("Plans", R.drawable.ic_plans),
    PROGRESS("Progress", R.drawable.ic_progress),
    SETTINGS("Settings", R.drawable.ic_settings),
}

/** Something opened over the tabs. */
sealed interface Page {
    data class ExerciseInfo(val id: String) : Page
    data class PlanInfo(val id: String) : Page
    data class PlanEdit(val id: String?) : Page
    data object Workout : Page
    data class ExerciseProgress(val id: String) : Page
    data class SessionInfo(val id: String) : Page
}

/** Opening and closing pages, for every screen. */
class Nav(val open: (Page) -> Unit, val back: () -> Unit, val tab: (Tab) -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home() {
    var tab by rememberSaveable { mutableStateOf(Tab.TRAIN) }
    var pages by remember { mutableStateOf(listOf<Page>()) }
    val navLive = Nav(open = { pages = pages + it }, back = { pages = pages.dropLast(1) }, tab = { tab = it; pages = emptyList() })
    BackHandler(enabled = pages.isNotEmpty()) { pages = pages.dropLast(1) }

    when (val page = pages.lastOrNull()) {
        is Page.ExerciseInfo -> ExerciseScreen(page.id, navLive)
        is Page.PlanInfo -> PlanScreen(page.id, navLive)
        is Page.PlanEdit -> PlanEditor(page.id, navLive)
        is Page.Workout -> WorkoutScreen(navLive)
        is Page.ExerciseProgress -> ExerciseProgressScreen(page.id, navLive)
        is Page.SessionInfo -> SessionScreen(page.id, navLive)
        null -> Scaffold(
            topBar = { TopAppBar(title = { Text(if (tab == Tab.TRAIN) "Training Coach" else tab.label) }) },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(painterResource(t.icon), null) },
                            label = { Text(t.label, maxLines = 1) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Tab.TRAIN -> TrainTab(navLive)
                    Tab.EXERCISES -> ExercisesTab(navLive)
                    Tab.PLANS -> PlansTab(navLive)
                    Tab.PROGRESS -> ProgressTab(navLive)
                    Tab.SETTINGS -> SettingsTab()
                }
            }
        }
    }
}
