package com.stylekeyboard.app.app

import android.content.Context
import com.stylekeyboard.app.data.db.StyleDatabase
import com.stylekeyboard.app.data.repository.AppConfigRepository
import com.stylekeyboard.app.data.repository.AutoSenderLogRepository
import com.stylekeyboard.app.data.repository.PredictionRepository
import com.stylekeyboard.app.data.repository.PresetRepository
import com.stylekeyboard.app.data.repository.ShortcutRepository

/**
 * Hand-rolled service locator. Keeps DI lightweight (no Hilt/Ksp wiring needed
 * for the keyboard-process side) and lets both the Host App and the keyboard
 * service reach the same Room database via the application context.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    val applicationContext: Context
        get() = appContext

    val database: StyleDatabase by lazy { StyleDatabase.get(appContext) }

    val presetRepository: PresetRepository by lazy { PresetRepository(database.presetDao()) }
    val shortcutRepository: ShortcutRepository by lazy { ShortcutRepository(database.shortcutDao()) }
    val appConfigRepository: AppConfigRepository by lazy { AppConfigRepository(database.appConfigDao()) }
    val predictionRepository: PredictionRepository by lazy {
        PredictionRepository(
            database.wordFrequencyDao(),
            database.bigramDao(),
            database.trigramDao(),
            database.userDictionaryDao()
        )
    }
    val autoSenderLogRepository: AutoSenderLogRepository by lazy {
        AutoSenderLogRepository(database.autoSenderLogDao())
    }

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
    }
}
