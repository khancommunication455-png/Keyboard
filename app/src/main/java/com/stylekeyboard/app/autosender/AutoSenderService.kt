package com.stylekeyboard.app.autosender

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.stylekeyboard.app.R
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.data.db.entity.AutoSenderLogEntity
import com.stylekeyboard.app.data.model.AutoSenderScript
import com.stylekeyboard.app.data.model.JsonCodec
import com.stylekeyboard.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service that runs a user-supplied [AutoSenderScript].
 *
 * Modes:
 *   - "once"     : send each message once, then stop.
 *   - "n_times"  : run the whole script [script.loopCount] times.
 *   - "infinite" : loop until the user taps Stop.
 *
 * Safety rails:
 *   - Minimum interval floor of 3 seconds enforced via [MIN_INTERVAL_MS].
 *   - Foreground notification always visible, with a "STOP ALL" action.
 *   - Wake lock held only while the loop is actively running; released on pause/stop.
 */
class AutoSenderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var runJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val _state = MutableStateFlow(RunState.Idle)
    val state get() = _state.value

    enum class RunState { Idle, Running, Paused, Stopping }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("Auto Sender ready", running = false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val json = intent.getStringExtra(EXTRA_SCRIPT) ?: return START_STICKY
                val script = JsonCodec.decodeScript(json)
                startRun(script)
            }
            ACTION_PAUSE -> pauseRun()
            ACTION_RESUME -> resumeRun()
            ACTION_STOP -> {
                stopRun()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startRun(script: AutoSenderScript) {
        runJob?.cancel()
        acquireWakeLock()
        _state.value = RunState.Running
        updateNotification("Auto Sender is running")
        runJob = scope.launch {
            val effectiveInterval = script.intervalMs.coerceAtLeast(MIN_INTERVAL_MS)
            val maxIterations = when (script.loopMode) {
                "once" -> 1
                "n_times" -> script.loopCount.coerceAtLeast(1)
                else -> Int.MAX_VALUE
            }
            var iteration = 0
            outer@ while (iteration < maxIterations && _state.value != RunState.Stopping) {
                iteration++
                for (msg in script.messages) {
                    // Pause gate
                    while (_state.value == RunState.Paused) {
                        delay(200)
                    }
                    if (_state.value == RunState.Stopping) break@outer

                    val sent = runCatching { SenderStrategy.send(this@AutoSenderService, script, msg.text) }
                        .getOrElse { false }

                    ServiceLocator.autoSenderLogRepository.insert(
                        AutoSenderLogEntity(
                            sentAt = System.currentTimeMillis(),
                            targetPackage = script.targetPackage,
                            message = msg.text,
                            status = if (sent) "sent" else "failed"
                        )
                    )
                    updateNotification("Sent ${iteration}/${if (maxIterations == Int.MAX_VALUE) "∞" else maxIterations}")

                    if (msg.delayMs > 0) delay(msg.delayMs)
                    else delay(script.perMessageDelayMs)
                }
                if (iteration < maxIterations) delay(effectiveInterval)
            }
            _state.value = RunState.Idle
            releaseWakeLock()
            updateNotification("Auto Sender finished")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun pauseRun() {
        if (_state.value == RunState.Running) {
            _state.value = RunState.Paused
            releaseWakeLock()
            updateNotification("Auto Sender paused")
        }
    }

    private fun resumeRun() {
        if (_state.value == RunState.Paused) {
            acquireWakeLock()
            _state.value = RunState.Running
            updateNotification("Auto Sender is running")
        }
    }

    private fun stopRun() {
        _state.value = RunState.Stopping
        runJob?.cancel()
        runJob = null
        releaseWakeLock()
        _state.value = RunState.Idle
    }

    override fun onDestroy() {
        super.onDestroy()
        runJob?.cancel()
        releaseWakeLock()
        scope.cancel()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StyleKeyboard:AutoSender")
        wakeLock?.acquire(10 * 60 * 1000L) // 10 min auto-release safety net
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun buildNotification(text: String, running: Boolean = true): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AutoSenderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, AutoSenderService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, "auto_sender")
            .setContentTitle(getString(R.string.auto_sender_running))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(openIntent)
            .setOngoing(running)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.auto_sender_stop_all), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        mgr.notify(NOTIF_ID, buildNotification(text))
    }

    companion object {
        const val NOTIF_ID = 4242
        const val ACTION_START = "com.stylekeyboard.autosender.START"
        const val ACTION_PAUSE = "com.stylekeyboard.autosender.PAUSE"
        const val ACTION_RESUME = "com.stylekeyboard.autosender.RESUME"
        const val ACTION_STOP = "com.stylekeyboard.autosender.STOP"
        const val EXTRA_SCRIPT = "script_json"

        // Safety rail: enforce a minimum interval between script iterations so
        // the user can't accidentally hammer another app's UI.
        const val MIN_INTERVAL_MS = 3000L
    }
}
