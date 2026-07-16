package com.stylekeyboard.app.data.repository

import com.stylekeyboard.app.data.db.dao.AppConfigDao
import com.stylekeyboard.app.data.db.entity.AppConfigEntity
import com.stylekeyboard.app.data.model.GlintConfig
import com.stylekeyboard.app.data.model.JsonCodec
import com.stylekeyboard.app.data.model.SoundConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppConfigRepository(private val dao: AppConfigDao) {

    fun observe(): Flow<AppConfigEntity?> = dao.observe()

    fun observeResolved(): Flow<ResolvedConfig> = observe().map { c ->
        c?.let {
            ResolvedConfig(
                gifBackgroundUri = it.gifBackgroundUri,
                glint = JsonCodec.decodeGlint(it.glintConfigJson),
                keyShape = it.keyShape,
                sound = JsonCodec.decodeSound(it.soundConfigJson),
                hapticsEnabled = it.hapticsEnabled,
                activePresetId = it.activePresetId,
                emojiShortcutsEnabled = it.emojiShortcutsEnabled,
                predictiveTextEnabled = it.predictiveTextEnabled,
                themeId = it.themeId
            )
        } ?: ResolvedConfig()
    }

    suspend fun get(): AppConfigEntity? = dao.get()

    suspend fun ensureSeeded() {
        if (dao.get() == null) {
            dao.upsert(
                AppConfigEntity(
                    id = 1,
                    gifBackgroundUri = "",
                    glintConfigJson = JsonCodec.encodeGlint(GlintConfig()),
                    keyShape = "square",
                    soundConfigJson = JsonCodec.encodeSound(SoundConfig()),
                    hapticsEnabled = true,
                    activePresetId = 1L,
                    emojiShortcutsEnabled = true,
                    predictiveTextEnabled = true,
                    themeId = "charcoal"
                )
            )
        }
    }

    suspend fun setActivePreset(id: Long) {
        val current = dao.get() ?: return ensureSeeded().let { setActivePreset(id) }
        dao.update(current.copy(activePresetId = id))
    }

    suspend fun setGifBackground(uri: String) {
        val current = dao.get() ?: return
        dao.update(current.copy(gifBackgroundUri = uri))
    }

    suspend fun setGlint(cfg: GlintConfig) {
        val current = dao.get() ?: return
        dao.update(current.copy(glintConfigJson = JsonCodec.encodeGlint(cfg)))
    }

    suspend fun setKeyShape(shape: String) {
        val current = dao.get() ?: return
        dao.update(current.copy(keyShape = shape))
    }

    suspend fun setSound(cfg: SoundConfig) {
        val current = dao.get() ?: return
        dao.update(current.copy(soundConfigJson = JsonCodec.encodeSound(cfg)))
    }

    suspend fun setHaptics(enabled: Boolean) {
        val current = dao.get() ?: return
        dao.update(current.copy(hapticsEnabled = enabled))
    }

    suspend fun setEmojiShortcutsEnabled(enabled: Boolean) {
        val current = dao.get() ?: return
        dao.update(current.copy(emojiShortcutsEnabled = enabled))
    }

    suspend fun setPredictiveTextEnabled(enabled: Boolean) {
        val current = dao.get() ?: return
        dao.update(current.copy(predictiveTextEnabled = enabled))
    }

    suspend fun setThemeId(themeId: String) {
        val current = dao.get() ?: return
        dao.update(current.copy(themeId = themeId))
    }
}

data class ResolvedConfig(
    val gifBackgroundUri: String = "",
    val glint: GlintConfig = GlintConfig(),
    val keyShape: String = "square",
    val sound: SoundConfig = SoundConfig(),
    val hapticsEnabled: Boolean = true,
    val activePresetId: Long = 1L,
    val emojiShortcutsEnabled: Boolean = true,
    val predictiveTextEnabled: Boolean = true,
    val themeId: String = "charcoal"
)
