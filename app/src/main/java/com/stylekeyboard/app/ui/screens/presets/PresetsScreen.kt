package com.stylekeyboard.app.ui.screens.presets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stylekeyboard.app.data.db.entity.PresetEntity
import com.stylekeyboard.app.data.model.JsonCodec
import com.stylekeyboard.app.ui.components.GlowingButton
import com.stylekeyboard.app.ui.components.GradientOutlinePanel
import com.stylekeyboard.app.ui.theme.AccentPurple
import com.stylekeyboard.app.ui.theme.Charcoal
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientEnd
import com.stylekeyboard.app.ui.theme.GradientStart
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary
import com.stylekeyboard.app.util.DefaultPresets
import com.stylekeyboard.app.util.TextConverter

@Composable
fun PresetsScreen(vm: PresetsViewModel = viewModel()) {
    val presets by vm.presets.collectAsState()
    var editing by remember { mutableStateOf<PresetEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("My Presets", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Create, edit, duplicate and delete font presets.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { creating = true },
                colors = CardDefaults.cardColors(containerColor = Elevated),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = GradientEnd)
                    Spacer(Modifier.width(12.dp))
                    Text("Create new preset", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))

            presets.forEach { preset ->
                PresetRow(
                    preset = preset,
                    onEdit = { editing = preset },
                    onDuplicate = { vm.duplicate(preset) },
                    onDelete = { vm.delete(preset) }
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (creating) {
        PresetEditorDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { name, description, mapping ->
                vm.create(name, description, mapping)
                creating = false
            }
        )
    }
    editing?.let { preset ->
        PresetEditorDialog(
            initial = preset,
            onDismiss = { editing = null },
            onSave = { name, description, mapping ->
                vm.update(preset, name, description, mapping)
                editing = null
            }
        )
    }
}

@Composable
private fun PresetRow(
    preset: PresetEntity,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(preset.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (preset.description.isNotBlank()) {
                    Text(preset.description, color = TextSecondary, fontSize = 12.sp)
                }
                if (preset.isBuiltIn) {
                    Spacer(Modifier.height(4.dp))
                    Text("Built-in", color = AccentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = TextPrimary) }
            IconButton(onClick = onDuplicate) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate", tint = TextPrimary) }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = TextPrimary) }
        }
    }
}

@Composable
private fun PresetEditorDialog(
    initial: PresetEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Map<String, String>) -> Unit
) {
    val baseMap = remember {
        initial?.let { JsonCodec.decodeMap(it.mappingJson) } ?: TextConverter.emptyMapping()
    }
    val editableMap = remember { mutableStateMapOf<String, String>().apply { putAll(baseMap) } }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var bulkPasteInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(name, description, editableMap.toMap())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (initial == null) "New Preset" else "Edit Preset", color = TextPrimary) },
        text = {
            Column(Modifier.fillMaxWidth().height(560.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Preset name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlowingButton(
                        text = "Randomize",
                        onClick = {
                            val randomized = DefaultPresets.randomize(editableMap.toMap())
                            editableMap.clear()
                            editableMap.putAll(randomized)
                        },
                        outlineColor = AccentPurple
                    )
                    Spacer(Modifier.width(8.dp))
                    GlowingButton(
                        text = "Reset",
                        onClick = {
                            val fresh = TextConverter.emptyMapping()
                            editableMap.clear()
                            editableMap.putAll(fresh)
                        },
                        outlineColor = GradientStart
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Bulk paste — assign symbols to Aa-Zz, 0-9 sequentially:", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = bulkPasteInput,
                        onValueChange = { bulkPasteInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("symbols", fontSize = 12.sp) }
                    )
                    Spacer(Modifier.width(8.dp))
                    GlowingButton(
                        text = "Assign",
                        onClick = {
                            if (bulkPasteInput.isNotBlank()) {
                                val assigned = TextConverter.bulkPasteAssign(bulkPasteInput, editableMap.toMap())
                                editableMap.clear()
                                editableMap.putAll(assigned)
                                bulkPasteInput = ""
                            }
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("MAPPING TABLE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                // Mapping table — monospaced 2-column rows
                val keys = editableMap.keys.toList()
                keys.forEach { key ->
                    val value = editableMap[key] ?: ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayKey(key),
                            modifier = Modifier.width(60.dp),
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = { editableMap[key] = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    )
}

private fun displayKey(key: String): String = when (key) {
    " " -> "␣"
    "\n" -> "⏎"
    else -> key
}
