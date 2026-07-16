package com.stylekeyboard.app.keyboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stylekeyboard.app.data.db.entity.ShortcutEntity

/**
 * Gboard-style keyboard screen.
 *
 * Layers (back to front):
 *   1. Theme background (color) + optional GIF/video
 *   2. Scrim (semi-transparent overlay so keys stay legible)
 *   3. Glint sweep (optional, animated gradient)
 *   4. Suggestion strip (Gboard-style: 3 word slots + emoji shortcuts strip)
 *   5. Toolbar (preset switcher pill + emoji / settings / globe icons)
 *   6. QWERTY rows (number row + 4 letter rows) OR symbol rows
 *   7. Long-press popup (alternates palette above the held key)
 *   8. Tap-preview popup (single char preview above the tapped key)
 */
@Composable
fun KeyboardScreen(
    controller: KeyboardController,
    onKey: (Key) -> Unit,
    onSuggestionTap: (String) -> Unit,
    onEmojiShortcutTap: (ShortcutEntity) -> Unit,
    onGlobe: () -> Unit,
    onSettings: () -> Unit,
    onSwitchPreset: () -> Unit,
    onOpenEmojiPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by controller.config.collectAsState()
    val preset by controller.activePreset.collectAsState()
    val shiftOn by controller.shiftOn.collectAsState()
    val suggestions by controller.suggestions.collectAsState()
    val emojiSuggestions by controller.emojiSuggestions.collectAsState()

    val theme = KeyboardTheme.byId(config.themeId)

    var symbolMode by remember { mutableStateOf(false) }
    var symbol2Mode by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    // Rows to display in current mode
    val rows: List<List<Key>> = when {
        symbol2Mode -> KeyboardLayout.symbol2Rows
        symbolMode -> KeyboardLayout.symbolRows
        else -> KeyboardLayout.letterRows(shiftOn)
    }

    // Active key popup state (single-tap preview)
    var previewKey by remember { mutableStateOf<Key?>(null) }
    // Long-press alternates popup
    var alternatesKey by remember { mutableStateOf<Key.Letter?>(null) }
    var alternatesList by remember { mutableStateOf<List<String>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(theme.background)
    ) {
        // Layer 1: GIF background
        if (config.gifBackgroundUri.isNotBlank()) {
            KeyboardBackground(uri = config.gifBackgroundUri, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        } else if (theme.scrimAlpha > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = theme.scrimAlpha)))
        }

        // Layer 3: Glint
        if (config.glint.enabled) {
            GlintSweep(
                color = Color(config.glint.color),
                speedMs = config.glint.speedMs,
                opacity = config.glint.opacity,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Toolbar (Gboard-style: thin row with icons)
            Toolbar(
                presetName = preset?.name ?: "Default",
                theme = theme,
                onSwitchPreset = onSwitchPreset,
                onOpenThemePicker = { showThemePicker = !showThemePicker },
                onOpenEmoji = onOpenEmojiPanel,
                onSettings = onSettings,
                onGlobe = onGlobe
            )

            // Suggestion strip
            SuggestionStrip(
                suggestions = suggestions,
                emojiSuggestions = emojiSuggestions,
                onSuggestionTap = onSuggestionTap,
                onEmojiShortcutTap = onEmojiShortcutTap
            )

            // Rows
            rows.forEach { row ->
                KeyRow(
                    keys = row,
                    theme = theme,
                    keyShape = config.keyShape,
                    shiftOn = shiftOn,
                    onKeyTap = { key ->
                        // Handle symbol/ABC toggle here so the layout switches instantly
                        when (key) {
                            is Key.Action -> when (key.type) {
                                Key.ActionType.Symbol -> {
                                    when {
                                        symbol2Mode -> { symbol2Mode = false; symbolMode = false }
                                        symbolMode -> { symbol2Mode = true }
                                        else -> { symbolMode = true }
                                    }
                                }
                                else -> onKey(key)
                            }
                            else -> onKey(key)
                        }
                    },
                    onKeyPressStart = { key -> previewKey = key },
                    onKeyPressEnd = { previewKey = null },
                    onLongPress = { key ->
                        if (key is Key.Letter && key.alternates.isNotEmpty()) {
                            alternatesKey = key
                            alternatesList = key.alternates
                        }
                    }
                )
            }
        }

        // Layer 7: alternates popup
        if (alternatesKey != null && alternatesList.isNotEmpty()) {
            AlternatesPopup(
                alternates = alternatesList,
                theme = theme,
                onPick = { picked ->
                    onKey(Key.Letter(picked))
                    alternatesKey = null
                    alternatesList = emptyList()
                },
                onDismiss = {
                    alternatesKey = null
                    alternatesList = emptyList()
                }
            )
        }

        // Layer 8: tap preview popup
        if (previewKey is Key.Letter) {
            TapPreview(
                key = previewKey as Key.Letter,
                theme = theme,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
            )
        }

        // Theme picker overlay
        if (showThemePicker) {
            ThemePicker(
                themes = KeyboardTheme.all,
                activeId = theme.id,
                onPick = {
                    controller.setThemeId(it.id)
                    showThemePicker = false
                },
                onDismiss = { showThemePicker = false }
            )
        }
    }
}

// ---------- Toolbar ----------

@Composable
private fun Toolbar(
    presetName: String,
    theme: KeyboardTheme,
    onSwitchPreset: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onOpenEmoji: () -> Unit,
    onSettings: () -> Unit,
    onGlobe: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(34.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: preset pill
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(theme.accent)
                .clickable(onClick = onSwitchPreset)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(presetName, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        // Right: icon row
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolbarIcon("🎨", onClick = onOpenThemePicker, theme = theme)
            Spacer(Modifier.width(4.dp))
            ToolbarIcon("😀", onClick = onOpenEmoji, theme = theme)
            Spacer(Modifier.width(4.dp))
            ToolbarIcon("🌐", onClick = onGlobe, theme = theme)
            Spacer(Modifier.width(4.dp))
            ToolbarIcon("⚙", onClick = onSettings, theme = theme)
        }
    }
}

@Composable
private fun ToolbarIcon(glyph: String, onClick: () -> Unit, theme: KeyboardTheme) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(theme.keyBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = theme.keyFg, fontSize = 14.sp)
    }
}

// ---------- Suggestion strip ----------

@Composable
private fun SuggestionStrip(
    suggestions: List<String>,
    emojiSuggestions: List<ShortcutEntity>,
    onSuggestionTap: (String) -> Unit,
    onEmojiShortcutTap: (ShortcutEntity) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (suggestions.isEmpty() && emojiSuggestions.isEmpty()) {
            Text("Type to see suggestions…", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
        }
        suggestions.forEach { word ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable { onSuggestionTap(word) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(word, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (emojiSuggestions.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            emojiSuggestions.take(3).forEach { sc ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FF4D9D))
                        .clickable { onEmojiShortcutTap(sc) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(sc.emojis, fontSize = 14.sp)
                }
            }
        }
    }
}

// ---------- Key row ----------

@Composable
private fun KeyRow(
    keys: List<Key>,
    theme: KeyboardTheme,
    keyShape: String,
    shiftOn: Boolean,
    onKeyTap: (Key) -> Unit,
    onKeyPressStart: (Key) -> Unit,
    onKeyPressEnd: () -> Unit,
    onLongPress: (Key) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            KeyCap(
                key = key,
                theme = theme,
                shape = keyShape,
                shiftActive = shiftOn,
                modifier = when (key) {
                    is Key.Action -> when (key.type) {
                        Key.ActionType.Space -> Modifier.weight(2.5f)
                        Key.ActionType.Delete -> Modifier.weight(1.5f)
                        Key.ActionType.Shift -> Modifier.weight(1.5f)
                        Key.ActionType.Enter -> Modifier.weight(1.5f)
                        Key.ActionType.Symbol -> Modifier.weight(1.5f)
                        else -> Modifier.weight(1f)
                    }
                    else -> Modifier.weight(1f)
                },
                onTap = { onKeyTap(key) },
                onPressStart = { onKeyPressStart(key) },
                onPressEnd = onKeyPressEnd,
                onLongPress = { onLongPress(key) }
            )
        }
    }
}

@Composable
private fun KeyCap(
    key: Key,
    theme: KeyboardTheme,
    shape: String,
    modifier: Modifier = Modifier,
    shiftActive: Boolean = false,
    onTap: () -> Unit,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onLongPress: () -> Unit
) {
    val baseShape = when (shape) {
        "circle" -> CircleShape
        else -> RoundedCornerShape(6.dp)
    }
    val isAction = key is Key.Action
    val isShift = key is Key.Action && key.type == Key.ActionType.Shift
    val isSpace = key is Key.Action && key.type == Key.ActionType.Space
    val isDelete = key is Key.Action && key.type == Key.ActionType.Delete
    val isEnter = key is Key.Action && key.type == Key.ActionType.Enter
    val isSymbolToggle = key is Key.Action && key.type == Key.ActionType.Symbol

    // Long-press detection
    var longPressFired by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(baseShape)
            .then(
                if (theme.keyBorder != null) {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.dp, theme.keyBorder, baseShape)
                } else {
                    Modifier.background(if (isAction) theme.keyBgActive else theme.keyBg)
                }
            )
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        longPressFired = false
                        onPressStart()
                        tryAwaitRelease()
                        onPressEnd()
                    },
                    onTap = {
                        if (!longPressFired) onTap()
                    },
                    onLongPress = {
                        longPressFired = true
                        onLongPress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val label = when {
            isShift && shiftActive -> "⇪"
            isSpace -> "space"
            key is Key.Action -> key.label
            key is Key.Letter && shiftActive -> key.label.uppercase()
            else -> (key as? Key.Letter)?.label ?: ""
        }
        val textColor = when {
            isShift && shiftActive -> theme.accent
            isEnter -> Color.Black
            else -> theme.keyFg
        }
        val fontSize = when {
            isSpace -> 11.sp
            isSymbolToggle -> 12.sp
            else -> 17.sp
        }
        Text(label, color = textColor, fontSize = fontSize, fontWeight = if (isAction) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ---------- Long-press alternates popup ----------

@Composable
private fun AlternatesPopup(
    alternates: List<String>,
    theme: KeyboardTheme,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.0f)) // capture taps outside
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        LazyRow(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.keyBgActive)
                .border(1.dp, theme.accent, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(alternates) { alt ->
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.keyBg)
                        .clickable { onPick(alt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(alt, color = theme.keyFg, fontSize = 18.sp)
                }
            }
        }
    }
}

// ---------- Tap preview popup ----------

@Composable
private fun TapPreview(
    key: Key.Letter,
    theme: KeyboardTheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(theme.keyBgActive)
            .border(1.dp, theme.accent, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(key.label.uppercase(), color = theme.keyFg, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------- Theme picker ----------

@Composable
private fun ThemePicker(
    themes: List<KeyboardTheme>,
    activeId: String,
    onPick: (KeyboardTheme) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
    ) {
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            themes.forEach { th ->
                val selected = th.id == activeId
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(th.background)
                        .then(
                            if (selected) Modifier.border(2.dp, th.accent, RoundedCornerShape(8.dp))
                            else Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        )
                        .clickable { onPick(th) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(th.accent)
                    )
                }
            }
        }
    }
}

// ---------- Glint sweep ----------

@Composable
private fun GlintSweep(
    color: Color,
    speedMs: Int,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "glint")
    val position by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(speedMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glint_pos"
    )
    Box(
        modifier = modifier.drawBehind {
            val sweepWidth = size.width * 0.25f
            val start = position * size.width
            val end = start + sweepWidth
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = opacity),
                    Color.Transparent
                ),
                start = Offset(start, 0f),
                end = Offset(end, size.height)
            )
            drawRect(brush)
        }
    )
}
