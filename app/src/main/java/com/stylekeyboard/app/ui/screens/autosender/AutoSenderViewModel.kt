package com.stylekeyboard.app.ui.screens.autosender

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.autosender.AutoSenderManager
import com.stylekeyboard.app.data.db.entity.AutoSenderLogEntity
import com.stylekeyboard.app.data.model.AutoSenderScript
import com.stylekeyboard.app.data.model.JsonCodec
import com.stylekeyboard.app.data.model.ScriptMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutoSenderViewModel : ViewModel() {

    val log: StateFlow<List<AutoSenderLogEntity>> = ServiceLocator.autoSenderLogRepository
        .observeRecent(200)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var script by mutableStateOf(loadScript())
        private set

    fun updateScript(newScript: AutoSenderScript) {
        script = newScript
        saveScript(newScript)
    }

    fun addMessage(text: String, delayMs: Long) {
        val updated = script.copy(messages = script.messages + ScriptMessage(text, delayMs))
        updateScript(updated)
    }

    fun removeMessage(index: Int) {
        val updated = script.copy(messages = script.messages.toMutableList().also { it.removeAt(index) })
        updateScript(updated)
    }

    fun setTargetPackage(pkg: String, useAccessibility: Boolean) {
        updateScript(script.copy(targetPackage = pkg, useAccessibility = useAccessibility))
    }

    fun setLoop(mode: String, count: Int, intervalMs: Long, perMessageDelayMs: Long) {
        updateScript(script.copy(loopMode = mode, loopCount = count, intervalMs = intervalMs, perMessageDelayMs = perMessageDelayMs))
    }

    private fun loadScript(): AutoSenderScript {
        val json = AutoSenderPrefs.get().getString("script_json", "") ?: ""
        return JsonCodec.decodeScript(json)
    }

    private fun saveScript(s: AutoSenderScript) {
        AutoSenderPrefs.get().edit().putString("script_json", JsonCodec.encodeScript(s)).apply()
    }

    fun start(context: Context) {
        AutoSenderManager.start(context, script)
    }

    fun pause(context: Context) {
        AutoSenderManager.pause(context)
    }

    fun resume(context: Context) {
        AutoSenderManager.resume(context)
    }

    fun stop(context: Context) {
        AutoSenderManager.stop(context)
    }
}

object AutoSenderPrefs {
    fun get(): SharedPreferences =
        ServiceLocator.applicationContext.getSharedPreferences("auto_sender", Context.MODE_PRIVATE)
}

/** Check whether the StyleKeyboard AccessibilityService is currently enabled. */
fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(context, "com.stylekeyboard.app.autosender.AutoSenderAccessibilityService").flattenToString()
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
    while (splitter.hasNext()) {
        if (splitter.next().equals(expected, ignoreCase = true)) return true
    }
    return false
}

fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
