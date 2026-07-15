package com.stylekeyboard.app.autosender

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.stylekeyboard.app.data.model.AutoSenderScript

/**
 * Front-door for the Auto Sender UI. Builds the start/pause/resume/stop
 * intents and routes them to [AutoSenderService]. Hides the script JSON
 * marshalling from the ViewModel.
 */
object AutoSenderManager {

    fun start(context: Context, script: AutoSenderScript) {
        val json = com.stylekeyboard.app.data.model.JsonCodec.encodeScript(script)
        val intent = Intent(context, AutoSenderService::class.java).apply {
            action = AutoSenderService.ACTION_START
            putExtra(AutoSenderService.EXTRA_SCRIPT, json)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun pause(context: Context) {
        context.startService(
            Intent(context, AutoSenderService::class.java).apply {
                action = AutoSenderService.ACTION_PAUSE
            }
        )
    }

    fun resume(context: Context) {
        context.startService(
            Intent(context, AutoSenderService::class.java).apply {
                action = AutoSenderService.ACTION_RESUME
            }
        )
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, AutoSenderService::class.java).apply {
                action = AutoSenderService.ACTION_STOP
            }
        )
    }
}
