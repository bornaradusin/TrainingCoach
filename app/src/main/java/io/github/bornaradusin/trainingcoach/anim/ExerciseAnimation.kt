package io.github.bornaradusin.trainingcoach.anim

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.bornaradusin.trainingcoach.data.Anim
import io.github.bornaradusin.trainingcoach.data.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * An exercise on the IM-309 in 3D: frames rendered by tools/animations/3d (assets/anim3d/<id>/),
 * from the start position (00) to the end (12). Played as a loop: start, the lift, a hold and
 * the slower way back, with a caption for each part of the movement.
 */
@Composable
fun ExerciseAnimation(exercise: Exercise, modifier: Modifier = Modifier, playing: Boolean = true) {
    val context = LocalContext.current
    val frames by produceState(Frames.peek(exercise.id, playing), exercise.id, playing) {
        value = Frames.load(context, exercise.id, playing)
    }
    val time = if (playing) {
        val transition = rememberInfiniteTransition(label = exercise.id)
        val time by transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "time",
        )
        time
    } else 0.25f
    Box(modifier.aspectRatio(4f / 3f).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F0F0))) {
        val list = frames
        if (!list.isNullOrEmpty()) {
            val frame = if (list.size == 1) list[0] else list[(progress(time) * (list.size - 1)).roundToInt()]
            Image(
                frame, contentDescription = exercise.name,
                // Thumbnails show the person close up, the full view the whole machine
                contentScale = if (playing) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (playing) {
            Text(
                caption(exercise.anim, time),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF202020),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** The rendered frames, kept for the last few exercises shown. */
private object Frames {
    const val COUNT = 13
    private const val STILL = 4
    private val full = LruCache<String, List<ImageBitmap>>(4)
    private val stills = LruCache<String, List<ImageBitmap>>(40)

    fun peek(id: String, all: Boolean): List<ImageBitmap>? =
        if (all) full.get(id) else stills.get(id) ?: full.get(id)?.let { listOf(it[STILL]) }

    suspend fun load(context: Context, id: String, all: Boolean): List<ImageBitmap> = withContext(Dispatchers.IO) {
        peek(id, all)?.let { return@withContext it }
        val indices = if (all) (0 until COUNT).toList() else listOf(STILL)
        val opts = BitmapFactory.Options().apply { if (!all) inSampleSize = 2 }
        val list = indices.mapNotNull { i ->
            runCatching {
                context.assets.open("anim3d/$id/%02d.webp".format(i)).use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
            }.getOrNull()
        }
        if (list.isNotEmpty()) (if (all) full else stills).put(id, list)
        list
    }
}

/** Start position, lift (1.4 s), hold at the top, lower slowly (1.8 s). */
private fun progress(time: Float): Float {
    fun ease(x: Float) = (0.5f - 0.5f * cos(PI * x)).toFloat()
    return when {
        time < 0.08f -> 0f
        time < 0.42f -> ease((time - 0.08f) / 0.34f)
        time < 0.52f -> 1f
        time < 0.96f -> 1f - ease((time - 0.52f) / 0.44f)
        else -> 0f
    }
}

private fun caption(a: Anim, time: Float) = when {
    time < 0.08f || time >= 0.96f -> "Start position"
    time < 0.42f -> a.captions.lift
    time < 0.52f -> "Hold"
    else -> a.captions.lower
}
