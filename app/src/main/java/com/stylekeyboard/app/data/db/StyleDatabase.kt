package com.stylekeyboard.app.data.db

import android.content.Context
import android.util.Log
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
import java.io.File

/**
 * Single Room database shared by the Host App, the keyboard service and the
 * Auto Sender service.
 *
 * Versioning notes:
 *   - v1 → v2: added [AppConfigEntity.themeId] column.
 *   - We use [fallbackToDestructiveMigration] so missing migration paths just
 *     drop and recreate the tables. We also handle the edge case where the
 *     schema hash changes but the version didn't bump (happens during dev):
 *     we catch the exception, delete the DB file, and rebuild.
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
    version = 2,
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
        private const val TAG = "StyleDatabase"
        private const val DB_NAME = "style_keyboard.db"

        @Volatile private var INSTANCE: StyleDatabase? = null

        fun get(context: Context): StyleDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context).also { INSTANCE = it }
        }

        private fun build(context: Context): StyleDatabase {
            val appContext = context.applicationContext
            val db = Room.databaseBuilder(appContext, StyleDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
            // Force-open the DB to trigger any schema check now, so we can
            // catch a mismatch and nuke the file before the app uses it.
            return try {
                db.openHelper.writableDatabase  // triggers schema validation
                db
            } catch (t: Throwable) {
                Log.e(TAG, "DB schema check failed, wiping and retrying", t)
                runCatching { db.close() }
                try {
                    appContext.deleteDatabase(DB_NAME)
                    listOf("$DB_NAME-journal", "$DB_NAME-wal", "$DB_NAME-shm").forEach { suffix ->
                        val f = File(appContext.getDatabasePath(DB_NAME).parentFile, suffix)
                        if (f.exists()) f.delete()
                    }
                } catch (cleanup: Throwable) {
                    Log.w(TAG, "DB file cleanup failed", cleanup)
                }
                Room.databaseBuilder(appContext, StyleDatabase::class.java, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
            }
        }

        /**
         * Force-clear the DB. Useful for development when the schema changes
         * without a version bump.
         */
        fun reset(context: Context) {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                context.applicationContext.deleteDatabase(DB_NAME)
            }
        }
    }
}
