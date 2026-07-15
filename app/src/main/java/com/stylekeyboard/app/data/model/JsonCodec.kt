package com.stylekeyboard.app.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types

/**
 * JSON helpers for the AppConfig compound fields. Using simple data classes +
 * Moshi keeps schema changes additive without DB migrations.
 */
@JsonClass(generateAdapter = true)
data class GlintConfig(
    val enabled: Boolean = true,
    val color: Long = 0xFF06E6FF,
    val speedMs: Int = 2200,
    val opacity: Float = 0.35f
)

@JsonClass(generateAdapter = true)
data class SoundConfig(
    val pack: String = "mechanical", // "mechanical" | "soft_pop" | "marimba" | "custom"
    val volume: Float = 0.6f,
    val muted: Boolean = false,
    val customUri: String = ""
)

@JsonClass(generateAdapter = true)
data class AutoSenderScript(
    val targetPackage: String = "",
    val targetClassName: String = "",
    val useAccessibility: Boolean = false,
    val messages: List<ScriptMessage> = emptyList(),
    val loopMode: String = "once", // "once" | "n_times" | "infinite"
    val loopCount: Int = 1,
    val intervalMs: Long = 5000L,
    val perMessageDelayMs: Long = 1000L
)

@JsonClass(generateAdapter = true)
data class ScriptMessage(
    val text: String,
    val delayMs: Long = 0L
)

object JsonCodec {
    val moshi: Moshi by lazy { Moshi.Builder().build() }

    private val mapAdapter by lazy {
        moshi.adapter<Map<String, String>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                String::class.java
            )
        )
    }
    private val glintAdapter by lazy { moshi.adapter(GlintConfig::class.java) }
    private val soundAdapter by lazy { moshi.adapter(SoundConfig::class.java) }
    private val scriptAdapter by lazy { moshi.adapter(AutoSenderScript::class.java) }
    private val shortcutListAdapter by lazy {
        moshi.adapter<List<ShortcutExport>>(
            Types.newParameterizedType(List::class.java, ShortcutExport::class.java)
        )
    }

    fun encodeMap(map: Map<String, String>): String = mapAdapter.toJson(map)
    fun decodeMap(json: String): Map<String, String> =
        if (json.isBlank()) emptyMap() else mapAdapter.fromJson(json) ?: emptyMap()

    fun encodeGlint(cfg: GlintConfig): String = glintAdapter.toJson(cfg)
    fun decodeGlint(json: String): GlintConfig =
        if (json.isBlank()) GlintConfig() else glintAdapter.fromJson(json) ?: GlintConfig()

    fun encodeSound(cfg: SoundConfig): String = soundAdapter.toJson(cfg)
    fun decodeSound(json: String): SoundConfig =
        if (json.isBlank()) SoundConfig() else soundAdapter.fromJson(json) ?: SoundConfig()

    fun encodeScript(script: AutoSenderScript): String = scriptAdapter.toJson(script)
    fun decodeScript(json: String): AutoSenderScript =
        if (json.isBlank()) AutoSenderScript() else scriptAdapter.fromJson(json) ?: AutoSenderScript()

    fun encodeShortcutList(list: List<ShortcutExport>): String = shortcutListAdapter.toJson(list)
    fun decodeShortcutList(json: String): List<ShortcutExport> =
        if (json.isBlank()) emptyList() else shortcutListAdapter.fromJson(json) ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class ShortcutExport(
    val trigger: String,
    val emojis: String,
    val triggerMode: String = "whole"
)
