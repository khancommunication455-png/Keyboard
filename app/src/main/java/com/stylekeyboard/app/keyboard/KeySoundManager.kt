package com.stylekeyboard.app.keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.stylekeyboard.app.data.model.SoundConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Low-latency key-sound player backed by [SoundPool]. MediaPlayer would add
 * ~200ms of latency per tap; SoundPool preloads samples into memory so the
 * audible click happens within a frame of the tap.
 *
 * Sounds are built-in short WAV/OGG clips chosen per [SoundConfig.pack]. When
 * the user picks "custom", the file at [SoundConfig.customUri] is copied into
 * the app's cache dir and loaded once.
 *
 * Haptics are independent and use [Vibrator] (or [VibratorManager] on API 31+).
 */
class KeySoundManager(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var currentPack: String = ""
    private var soundId: Int = 0
    private var customCachedPath: String? = null

    private val vibrator: Vibrator? = run {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun applyConfig(config: SoundConfig) {
        if (config.pack == currentPack && (config.pack != "custom" || customCachedPath != null)) return
        currentPack = config.pack
        try {
            val rawResId = when (config.pack) {
                "mechanical" -> context.resources.getIdentifier("key_mech", "raw", context.packageName)
                "soft_pop" -> context.resources.getIdentifier("key_soft", "raw", context.packageName)
                "marimba" -> context.resources.getIdentifier("key_marimba", "raw", context.packageName)
                "custom" -> 0
                else -> context.resources.getIdentifier("key_mech", "raw", context.packageName)
            }
            if (rawResId != 0) {
                soundId = soundPool.load(context, rawResId, 1)
            } else if (config.pack == "custom" && config.customUri.isNotBlank()) {
                val cached = ensureCachedCopy(config.customUri)
                customCachedPath = cached
                soundId = soundPool.load(cached, 1)
            } else {
                // No sample available; sounds will simply be silent. User can drop
                // key_mech.ogg / key_soft.ogg / key_marimba.ogg into res/raw/.
                soundId = 0
            }
        } catch (t: Throwable) {
            Log.w("KeySoundManager", "Failed to load sound pack", t)
        }
    }

    fun playClick(config: SoundConfig, hapticsEnabled: Boolean) {
        if (!config.muted && soundId != 0) {
            soundPool.play(soundId, config.volume, config.volume, 1, 0, 1f)
        }
        if (hapticsEnabled && vibrator != null && vibrator.hasVibrator()) {
            val effect = VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        }
    }

    fun release() {
        soundPool.release()
    }

    private fun ensureCachedCopy(uriStr: String): String {
        val target = File(context.cacheDir, "key_custom.wav")
        if (target.exists()) return target.absolutePath
        context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { input ->
            FileOutputStream(target).use { out -> input.copyTo(out) }
        }
        return target.absolutePath
    }
}
