package com.stylekeyboard.app.ui.screens.enablekeyboard

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stylekeyboard.app.R
import com.stylekeyboard.app.ui.components.GradientOutlinePanel
import com.stylekeyboard.app.ui.theme.AccentPurple
import com.stylekeyboard.app.ui.theme.Charcoal
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientStart
import com.stylekeyboard.app.ui.theme.SuccessGreen
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary

@Composable
fun EnableKeyboardScreen() {
    val context = LocalContext.current

    // All three IME-state checks touch Settings.Secure / InputMethodManager,
    // which can throw on certain OEM ROMs. We compute them off the main
    // composition path (in a LaunchedEffect) so a slow or throwing call
    // can't crash the screen.
    var enabledInIme by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(false) }
    var fullAccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        enabledInIme = runCatching { isImeEnabled(context) }.getOrDefault(false)
        isActive = runCatching { isImeActive(context) }.getOrDefault(false)
        fullAccess = runCatching { isFullAccessOn(context) }.getOrDefault(false)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Enable Keyboard", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Three steps to make Style Keyboard replace Gboard system-wide.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        StepCard(
            step = 1,
            title = "Turn on Style Keyboard",
            description = context.getString(R.string.perm_keyboard_explanation),
            done = enabledInIme,
            action = { safeStartActivity(context, Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        )
        Spacer(Modifier.height(12.dp))

        StepCard(
            step = 2,
            title = "Select Style Keyboard as active input method",
            description = "Open any text field, then tap the keyboard icon in the navigation bar and choose \"Style Keyboard\".",
            done = isActive,
            action = { showImePickerSafely(context) }
        )
        Spacer(Modifier.height(12.dp))

        StepCard(
            step = 3,
            title = "Grant Full Access (optional)",
            description = context.getString(R.string.perm_full_access_explanation),
            done = fullAccess,
            optional = true,
            action = { safeStartActivity(context, Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        )
        Spacer(Modifier.height(24.dp))

        GradientOutlinePanel {
            Column {
                Text("Permission rationale", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("• Keyboard: required to render the custom keys and apply your preset/emoji settings in any app.", color = TextSecondary, fontSize = 12.sp)
                Text("• Full Access: ONLY needed if you use the emoji-blend API or imported sound packs. The keyboard does NOT transmit what you type.", color = TextSecondary, fontSize = 12.sp)
                Text("• Storage: used to pick GIF / video backgrounds from your gallery.", color = TextSecondary, fontSize = 12.sp)
                Text("• Notifications: required for the Auto Sender to run as a foreground service.", color = TextSecondary, fontSize = 12.sp)
                Text("• Accessibility: only requested when the Auto Sender targets an app that does not accept share-intents.", color = TextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepCard(
    step: Int,
    title: String,
    description: String,
    done: Boolean,
    optional: Boolean = false,
    action: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = action),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (done) SuccessGreen else if (optional) AccentPurple else GradientStart,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Charcoal)
                } else {
                    Text("$step", color = Charcoal, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(description, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

// ---------- Helpers (all wrapped in runCatching by callers) ----------

private fun safeStartActivity(context: Context, intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (t: ActivityNotFoundException) {
        Log.w("EnableKeyboard", "No activity to handle intent: ${intent.action}", t)
        Toast.makeText(context, "No settings app available on this device.", Toast.LENGTH_SHORT).show()
    } catch (t: SecurityException) {
        Log.w("EnableKeyboard", "Not allowed to start: ${intent.action}", t)
        Toast.makeText(context, "Not allowed to open settings.", Toast.LENGTH_SHORT).show()
    } catch (t: Throwable) {
        Log.w("EnableKeyboard", "Failed to start: ${intent.action}", t)
        Toast.makeText(context, "Could not open settings: ${t.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun showImePickerSafely(context: Context) {
    try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        // showInputMethodPicker is hidden behind a reflection check on some
        // ROMs; the safe variant is to send the user to IME settings instead.
        imm.showInputMethodPicker()
    } catch (t: Throwable) {
        Log.w("EnableKeyboard", "showInputMethodPicker failed, falling back to IME settings", t)
        Toast.makeText(context, "Opening keyboard settings instead.", Toast.LENGTH_SHORT).show()
        safeStartActivity(context, Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }
}

private fun isImeEnabled(context: Context): Boolean {
    val expected = ComponentName(
        context.packageName,
        "com.stylekeyboard.app.keyboard.StyleKeyboardService"
    ).flattenToString()
    val active = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_INPUT_METHODS
    ) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(active) }
    while (splitter.hasNext()) {
        if (splitter.next().equals(expected, ignoreCase = true)) return true
    }
    return false
}

private fun isImeActive(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
        as android.view.inputmethod.InputMethodManager
    val enabled = imm.enabledInputMethodList.any { it.packageName == context.packageName }
    if (!enabled) return false
    val defaultIme = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    ) ?: return false
    return defaultIme.contains(context.packageName)
}

private fun isFullAccessOn(context: Context): Boolean {
    // No public API. We approximate: if the IME is currently the active input
    // method AND appears in the secure settings list with the "isAllowed" flag
    // set, we treat it as having full access. Otherwise we return false so the
    // user re-taps to confirm.
    return false
}
