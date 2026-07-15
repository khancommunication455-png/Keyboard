package com.stylekeyboard.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stylekeyboard.app.data.db.dao.AutoSenderLogDao
import com.stylekeyboard.app.data.db.dao.AppConfigDao
import com.stylekeyboard.app.data.db.dao.BigramDao
import com.stylekeyboard.app.data.db.dao.PresetDao
import com.stylekeyboard.app.data.db.dao.ShortcutDao
import com.stylekeyboard.app.data.db.dao.TrigramDao
import com.stylekeyboard.app.data.db.dao.UserDictionaryDao
import com.stylekeyboard.app.data.db.dao.WordFrequencyDao
import com.stylekeyboard.app.data.db.entity.AppConfigEntity
import com.stylekeyboard.app.data.db.entity.AutoSenderLogEntity
import com.stylekeyboard.app.data.db.entity.BigramEntity
import com.stylekeyboard.app.data.db.entity.PresetEntity
import com.stylekeyboard.app.data.db.entity.ShortcutEntity
import com.stylekeyboard.app.data.db.entity.TrigramEntity
import com.stylekeyboard.app.data.db.entity.UserDictionaryEntity
import com.stylekeyboard.app.data.db.entity.WordFrequencyEntity

/**
 * Single Room database shared by the Host App, the keyboard service and the
 * Auto Sender service. Reads/writes are all on background coroutines.
 */
@Database(
    entities = [
        PresetEntity::class,
        ShortcutEntity::class,
        AppConfigEntity::class,
        WordFrequencyEntity::class,
        BigramEntity::class,
        TrigramEntity::class,
        UserDictionaryEntity::class,
        AutoSenderLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StyleDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun wordFrequencyDao(): WordFrequencyDao
    abstract fun bigramDao(): BigramDao
    abstract fun trigramDao(): TrigramDao
    abstract fun userDictionaryDao(): UserDictionaryDao
    abstract fun autoSenderLogDao(): AutoSenderLogDao

    companion object {
        @Volatile private var INSTANCE: StyleDatabase? = null

        fun get(context: Context): StyleDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                StyleDatabase::class.java,
                "style_keyboard.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
