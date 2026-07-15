package com.stylekeyboard.app.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.stylekeyboard.app.data.model.JsonCodec
import com.stylekeyboard.app.ui.components.GradientOutlinePanel
import com.stylekeyboard.app.ui.theme.Charcoal
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientEnd
import com.stylekeyboard.app.ui.theme.GradientStart
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary
import com.stylekeyboard.app.util.TextConverter

@Composable
fun HomeScreen(navController: NavController? = null, vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val presets by vm.presets.collectAsState()
    val activePresetId by vm.activePresetId.collectAsState()
    var inputText by remember { mutableStateOf("") }

    val activePreset = presets.firstOrNull { it.id == activePresetId } ?: presets.firstOrNull()
    val activeMap = activePreset?.let { JsonCodec.decodeMap(it.mappingJson) } ?: emptyMap()
    val convertedText = remember(inputText, activeMap) {
        TextConverter.convertText(inputText, activeMap)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Font Style Converter", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Type → Tap a preset → Copy. Max 3 taps to copy a converted message.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        GradientOutlinePanel {
            Column {
                Text("INPUT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type your message here…", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, fontFamily = FontFamily.SansSerif),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Charcoal,
                        unfocusedContainerColor = Charcoal,
                        focusedBorderColor = GradientEnd,
                        unfocusedBorderColor = Elevated
                    )
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("PRESETS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(presets, key = { it.id }) { preset ->
                PresetChip(
                    name = preset.name,
                    description = preset.description,
                    selected = preset.id == activePreset?.id,
                    onClick = { vm.setActivePreset(preset.id) }
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        GradientOutlinePanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("CONVERTED PREVIEW", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = convertedText.ifBlank { "— converted text appears here —" },
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(Modifier.weight(1f), "Copy", Icons.Outlined.ContentCopy, enabled = convertedText.isNotBlank(), onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("style", convertedText))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            })
            ActionButton(Modifier.weight(1f), "Share", Icons.Outlined.Share, enabled = convertedText.isNotBlank(), onClick = {
                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, convertedText) }
                context.startActivity(Intent.createChooser(send, "Share via"))
            })
            ActionButton(Modifier.weight(1f), "Clear", Icons.Outlined.Delete, enabled = inputText.isNotBlank(), onClick = { inputText = "" })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PresetChip(name: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val borderBrush = if (selected) Brush.linearGradient(listOf(GradientStart, GradientEnd)) else null
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(72.dp)
            .background(Elevated, RoundedCornerShape(14.dp))
            .then(if (borderBrush != null) Modifier.border(2.dp, borderBrush, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextSecondary, fontSize = 10.sp, maxLines = 2)
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(64.dp).clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = text, tint = if (enabled) GradientEnd else TextSecondary)
            Spacer(Modifier.width(8.dp))
            Text(text, color = if (enabled) TextPrimary else TextSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}
