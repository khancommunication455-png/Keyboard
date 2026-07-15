package com.stylekeyboard.app.ui.screens.appearance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stylekeyboard.app.data.model.GlintConfig
import com.stylekeyboard.app.data.model.SoundConfig
import com.stylekeyboard.app.ui.components.SectionCard
import com.stylekeyboard.app.ui.theme.AccentCyan
import com.stylekeyboard.app.ui.theme.AccentPink
import com.stylekeyboard.app.ui.theme.AccentPurple
import com.stylekeyboard.app.ui.theme.Charcoal
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientEnd
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary

@Composable
fun AppearanceScreen(vm: AppearanceViewModel = viewModel()) {
    val config by vm.config.collectAsState()
    val cfg = config ?: return

    val pickGif = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        // persist permission so the keyboard service can read it later
        runCatching {
            // context granted via launcher; the keyboard will take persistable permission separately
        }
        vm.setGif(uri.toString())
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Appearance", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("GIF backgrounds, glint animation, key shapes, sounds.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        // GIF background
        SectionCard("GIF / Video Background") {
            Text(
                "Pick a GIF or short video from your gallery. It loops behind the keys, hardware-accelerated. A ~50MB soft ceiling applies to the keyboard process so large files are downsampled.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlowingButtonOutlined(
                    text = if (cfg.gifBackgroundUri.isBlank()) "Pick file" else "Change file",
                    onClick = { pickGif.launch(arrayOf("image/gif", "image/*", "video/*")) }
                )
                Spacer(Modifier.width(8.dp))
                if (cfg.gifBackgroundUri.isNotBlank()) {
                    GlowingButtonOutlined(text = "Clear", onClick = { vm.setGif("") }, outlineColor = AccentPink)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Glint animation
        SectionCard("Key Glint / Shimmer") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(checked = cfg.glint.enabled, onCheckedChange = { vm.setGlint(cfg.glint.copy(enabled = it)) })
            }
            Spacer(Modifier.height(8.dp))
            Text("Speed: ${(cfg.glint.speedMs / 1000.0)}s per sweep", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = cfg.glint.speedMs.toFloat(),
                onValueChange = { vm.setGlint(cfg.glint.copy(speedMs = it.toInt())) },
                valueRange = 800f..5000f
            )
            Text("Opacity: ${(cfg.glint.opacity * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = cfg.glint.opacity,
                onValueChange = { vm.setGlint(cfg.glint.copy(opacity = it)) },
                valueRange = 0.1f..0.9f
            )
            Text("Color", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorSwatch(AccentPurple, selected = cfg.glint.color == AccentPurple.value.toLong()) {
                    vm.setGlint(cfg.glint.copy(color = AccentPurple.value.toLong()))
                }
                ColorSwatch(AccentCyan, selected = cfg.glint.color == AccentCyan.value.toLong()) {
                    vm.setGlint(cfg.glint.copy(color = AccentCyan.value.toLong()))
                }
                ColorSwatch(AccentPink, selected = cfg.glint.color == AccentPink.value.toLong()) {
                    vm.setGlint(cfg.glint.copy(color = AccentPink.value.toLong()))
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Key shape
        SectionCard("Key Shape / Theme") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyShapeOption("Square", Icons.Outlined.Square, cfg.keyShape == "square") { vm.setKeyShape("square") }
                KeyShapeOption("Circle", Icons.Outlined.Circle, cfg.keyShape == "circle") { vm.setKeyShape("circle") }
                KeyShapeOption("Neon", Icons.Outlined.Bolt, cfg.keyShape == "transparent_neon") { vm.setKeyShape("transparent_neon") }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Sounds
        SectionCard("Key Click Sounds") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mute", color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(checked = cfg.sound.muted, onCheckedChange = { vm.setSound(cfg.sound.copy(muted = it)) })
            }
            Spacer(Modifier.height(8.dp))
            Text("Volume: ${(cfg.sound.volume * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = cfg.sound.volume,
                onValueChange = { vm.setSound(cfg.sound.copy(volume = it)) },
                valueRange = 0f..1f
            )
            Spacer(Modifier.height(8.dp))
            Text("Sound pack", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("mechanical", "soft_pop", "marimba", "custom").forEach { pack ->
                    KeyShapeOption(
                        label = pack.replace('_', ' ').replaceFirstChar { ch: Char -> ch.uppercaseChar().toString() },
                        icon = Icons.Outlined.MusicNote,
                        selected = cfg.sound.pack == pack,
                        onClick = { vm.setSound(cfg.sound.copy(pack = pack)) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Haptics (vibration)", color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(checked = cfg.hapticsEnabled, onCheckedChange = { vm.setHaptics(it) })
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color, CircleShape)
            .then(if (selected) Modifier.border(3.dp, TextPrimary, CircleShape) else Modifier)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun KeyShapeOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(96.dp)
            .height(72.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (selected) GradientEnd else TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(label, color = if (selected) TextPrimary else TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GlowingButtonOutlined(text: String, onClick: () -> Unit, outlineColor: Color = AccentPurple) {
    Box(
        modifier = Modifier
            .background(Elevated, RoundedCornerShape(10.dp))
            .border(1.dp, outlineColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
