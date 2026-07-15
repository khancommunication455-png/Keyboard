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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush as GraphicsBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stylekeyboard.app.data.db.entity.ShortcutEntity
import com.stylekeyboard.app.ui.theme.AccentCyan
import com.stylekeyboard.app.ui.theme.AccentPink
import com.stylekeyboard.app.ui.theme.AccentPurple
import com.stylekeyboard.app.ui.theme.Charcoal
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientEnd
import com.stylekeyboard.app.ui.theme.GradientStart
import com.stylekeyboard.app.ui.theme.KeyActive
import com.stylekeyboard.app.ui.theme.KeyDefault
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary

/**
 * The Compose-rendered keyboard surface. [controller] holds the active preset,
 * shortcuts and config; the service feeds tap events back to it.
 *
 * Layers, back to front:
 *   1. GIF / video background (optional)
 *   2. Semi-transparent scrim so keys stay legible
 *   3. Suggestion strip (word predictions + emoji shortcuts)
 *   4. Toolbar (active preset name, globe icon, settings icon)
 *   5. QWERTY rows
 *   6. Glint sweep (optional, animated linear gradient over the key area)
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
    modifier: Modifier = Modifier
) {
    val config by controller.config.collectAsState()
    val preset by controller.activePreset.collectAsState()
    val shiftOn by controller.shiftOn.collectAsState()
    val suggestions by controller.suggestions.collectAsState()
    val emojiSuggestions by controller.emojiSuggestions.collectAsState()

    Box(modifier = modifier.fillMaxWidth().background(Charcoal)) {
        // Layer 1: GIF background
        if (config.gifBackgroundUri.isNotBlank()) {
            KeyboardBackground(uri = config.gifBackgroundUri, modifier = Modifier.fillMaxSize())
            // Layer 2: scrim
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }

        // Glint sweep drawn behind the keys (above scrim, below keys)
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
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Toolbar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarPill(
                    text = preset?.name ?: "Default",
                    onClick = onSwitchPreset
                )
                Row {
                    ToolbarIcon("🌐", onClick = onGlobe)
                    Spacer(Modifier.width(6.dp))
                    ToolbarIcon("⚙", onClick = onSettings)
                }
            }

            // Suggestion strip
            SuggestionStrip(
                suggestions = suggestions,
                emojiSuggestions = emojiSuggestions,
                onSuggestionTap = onSuggestionTap,
                onEmojiShortcutTap = onEmojiShortcutTap
            )

            // QWERTY
            KeyRow(KeyboardLayout.row1, config.keyShape, shiftOn, onKey)
            KeyRow(KeyboardLayout.row2, config.keyShape, shiftOn, onKey)
            KeyRow(KeyboardLayout.row3, config.keyShape, shiftOn, onKey)
            KeyRow(KeyboardLayout.row4, config.keyShape, shiftOn, onKey)
        }
    }
}

@Composable
private fun ToolbarPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GraphicsBrush.linearGradient(listOf(GradientStart, GradientEnd)))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ToolbarIcon(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(Elevated)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(glyph, color = TextPrimary, fontSize = 16.sp)
    }
}

@Composable
private fun SuggestionStrip(
    suggestions: List<String>,
    emojiSuggestions: List<ShortcutEntity>,
    onSuggestionTap: (String) -> Unit,
    onEmojiShortcutTap: (ShortcutEntity) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        suggestions.forEach { word ->
            SuggestionPill(word, onClick = { onSuggestionTap(word) })
        }
        Spacer(Modifier.width(6.dp))
        emojiSuggestions.take(3).forEach { sc ->
            EmojiPill(sc.emojis, onClick = { onEmojiShortcutTap(sc) })
        }
    }
}

@Composable
private fun SuggestionPill(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Elevated)
            .border(1.dp, AccentPurple, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmojiPill(emojis: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Elevated)
            .border(1.dp, AccentPink, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(emojis, fontSize = 16.sp)
    }
}

@Composable
private fun KeyRow(
    keys: List<Key>,
    shape: String,
    shiftOn: Boolean,
    onKey: (Key) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { key ->
            KeyCap(
                key = key,
                shape = shape,
                modifier = Modifier.weight(1f),
                shiftActive = shiftOn,
                onClick = { onKey(key) }
            )
        }
    }
}

@Composable
private fun KeyCap(
    key: Key,
    shape: String,
    modifier: Modifier = Modifier,
    shiftActive: Boolean = false,
    onClick: () -> Unit
) {
    val baseShape = when (shape) {
        "circle" -> CircleShape
        else -> RoundedCornerShape(8.dp)
    }
    val isAction = key is Key.Action
    val isShift = key is Key.Action && key.type == Key.ActionType.Shift
    val isSpace = key is Key.Action && key.type == Key.ActionType.Space

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(baseShape)
            .then(
                if (shape == "transparent_neon") {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.5.dp, AccentCyan, baseShape)
                } else {
                    Modifier.background(if (isAction) KeyActive else KeyDefault)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val label = when {
            isShift && shiftActive -> "⇪"
            isSpace -> "space"
            key is Key.Action -> key.label
            key is Key.Letter && shiftActive -> key.label.uppercase()
            else -> (key as? Key.Letter)?.label ?: ""
        }
        Text(
            text = label,
            color = if (isShift && shiftActive) AccentCyan else TextPrimary,
            fontSize = if (isSpace) 12.sp else 16.sp,
            fontWeight = if (isAction) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

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
            val brush = GraphicsBrush.linearGradient(
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
