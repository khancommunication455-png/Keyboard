package com.stylekeyboard.app.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Renders a looping GIF or short video as the keyboard background.
 *
 * - GIF path: uses Android's built-in [Movie] decoder inside a custom [View]
 *   that invalidates itself at ~30fps. The Movie is scaled to fill the view.
 * - Video path: uses ExoPlayer inside a [PlayerView], muted and looping.
 *
 * Memory budget: the keyboard process is capped at ~50MB. We don't decode the
 * GIF into a full bitmap atlas — [Movie.draw] decodes one frame at a time, so
 * peak memory is the single frame, not the whole animation.
 */
@Composable
fun KeyboardBackground(
    uri: String,
    modifier: Modifier = Modifier
) {
    if (uri.isBlank()) return

    val context = LocalContextSafe()
    val isVideo = remember(uri) {
        val mimeType = runCatching { context.contentResolver.getType(Uri.parse(uri)) }.getOrNull()
        mimeType?.startsWith("video/") == true
    }

    if (isVideo) {
        VideoBackground(uri, modifier)
    } else {
        GifBackground(uri, modifier)
    }
}

@Composable
private fun VideoBackground(uri: String, modifier: Modifier) {
    val context = LocalContextSafe()
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(uri) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            }
        }
    )
}

@Composable
private fun GifBackground(uri: String, modifier: Modifier) {
    val context = LocalContextSafe()
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val input: java.io.InputStream? = runCatching {
                ctx.contentResolver.openInputStream(Uri.parse(uri))
            }.getOrNull()
            val movie = input?.use { Movie.decodeStream(it) }
            GifMovieView(ctx, movie)
        }
    )
}

/** Custom View that draws one GIF frame per [postInvalidateDelayed] cycle. */
private class GifMovieView(context: Context, private val movie: Movie?) : View(context) {

    private var movieStart = 0L

    init {
        if (movie != null && movie.duration() > 0) {
            postInvalidateDelayed(33)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (movie == null || movie.duration() <= 0) return
        if (movieStart == 0L) movieStart = android.os.SystemClock.uptimeMillis()
        val relTime = ((android.os.SystemClock.uptimeMillis() - movieStart) % movie.duration()).toInt()
        movie.setTime(relTime)

        val scale = minOf(width.toFloat() / movie.width(), height.toFloat() / movie.height())
        canvas.save()
        canvas.scale(scale, scale)
        movie.draw(canvas, 0f, 0f)
        canvas.restore()
        postInvalidateDelayed(33)
    }
}

@Composable
private fun LocalContextSafe(): Context = androidx.compose.ui.platform.LocalContext.current
