package com.stylekeyboard.app.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.stylekeyboard.app.R
import com.stylekeyboard.app.data.db.StyleDatabase
import com.stylekeyboard.app.data.repository.AppConfigRepository
import com.stylekeyboard.app.data.repository.AutoSenderLogRepository
import com.stylekeyboard.app.data.repository.PredictionRepository
import com.stylekeyboard.app.data.repository.PresetRepository
import com.stylekeyboard.app.data.repository.ShortcutRepository
import com.stylekeyboard.app.util.BaseDictionary
import com.stylekeyboard.app.util.DefaultPresets
import com.stylekeyboard.app.util.DefaultShortcuts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. Owns the single [ServiceLocator] (manual DI) and
 * performs first-launch seeding of default presets, shortcuts, the base
 * dictionary, and the app-config row.
 */
class StyleApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        createNotificationChannels()
        appScope.launch { seedIfNeeded() }
    }

    private suspend fun seedIfNeeded() {
        val db = StyleDatabase.get(this)
        val presetRepo = PresetRepository(db.presetDao())
        val shortcutRepo = ShortcutRepository(db.shortcutDao())
        val configRepo = AppConfigRepository(db.appConfigDao())
        val predictionRepo = PredictionRepository(
            db.wordFrequencyDao(), db.bigramDao(), db.trigramDao(), db.userDictionaryDao()
        )

        configRepo.ensureSeeded()

        if (presetRepo.count() == 0) {
            DefaultPresets.all().forEach { seed ->
                presetRepo.insert(seed.name, seed.mapping, seed.description)
            }
        }

        if (shortcutRepo.getAll().isEmpty()) {
            DefaultShortcuts.all().forEach { shortcut ->
                db.shortcutDao().insert(shortcut)
            }
        }

        runCatching { predictionRepo.seedBaseDictionary(BaseDictionary.words()) }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                "auto_sender",
                getString(R.string.auto_sender_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.auto_sender_channel_desc)
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }
}
