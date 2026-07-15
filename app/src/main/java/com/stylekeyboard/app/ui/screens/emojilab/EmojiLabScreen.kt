package com.stylekeyboard.app.ui.screens.emojilab

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stylekeyboard.app.data.db.entity.ShortcutEntity
import com.stylekeyboard.app.ui.components.GlowingButton
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientEnd
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary

@Composable
fun EmojiLabScreen(vm: EmojiLabViewModel = viewModel()) {
    val context = LocalContext.current
    val shortcuts by vm.shortcuts.collectAsState()
    var editing by remember { mutableStateOf<ShortcutEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(vm.export().toByteArray())
        }
    }
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { input ->
            val json = input.bufferedReader().readText()
            vm.import(json)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Emoji Lab", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Text-to-emoji shortcuts. Type a trigger word in any app and tap the emoji to insert.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowingButton("Add", onClick = { creating = true }, modifier = Modifier.weight(1f))
            GlowingButton("Export", onClick = {
                createDocLauncher.launch("emoji_shortcuts.json")
            }, modifier = Modifier.weight(1f), outlineColor = GradientEnd)
            GlowingButton("Import", onClick = {
                openDocLauncher.launch(arrayOf("application/json"))
            }, modifier = Modifier.weight(1f), outlineColor = GradientEnd)
        }
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clickable { creating = true },
            colors = CardDefaults.cardColors(containerColor = Elevated),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = GradientEnd)
                Spacer(Modifier.width(12.dp))
                Text("Add new shortcut", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))

        shortcuts.forEach { sc ->
            ShortcutRow(sc, onEdit = { editing = sc }, onDelete = { vm.delete(sc) })
            Spacer(Modifier.height(8.dp))
        }
    }

    if (creating) {
        ShortcutEditorDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { trigger, emojis, mode ->
                vm.add(trigger, emojis, mode)
                creating = false
            }
        )
    }
    editing?.let { sc ->
        ShortcutEditorDialog(
            initial = sc,
            onDismiss = { editing = null },
            onSave = { trigger, emojis, mode ->
                vm.update(sc, trigger, emojis, mode)
                editing = null
            }
        )
    }
}

@Composable
private fun ShortcutRow(sc: ShortcutEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sc.trigger,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (sc.triggerMode == "whole") "whole-word" else "partial",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(sc.emojis, color = TextPrimary, fontSize = 22.sp)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = TextPrimary) }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = TextPrimary) }
        }
    }
}

@Composable
private fun ShortcutEditorDialog(
    initial: ShortcutEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var trigger by remember { mutableStateOf(initial?.trigger ?: "") }
    var emojis by remember { mutableStateOf(initial?.emojis ?: "") }
    var mode by remember { mutableStateOf(initial?.triggerMode ?: "whole") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (trigger.isNotBlank() && emojis.isNotBlank()) onSave(trigger.trim(), emojis.trim(), mode)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (initial == null) "New Shortcut" else "Edit Shortcut", color = TextPrimary) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = trigger, onValueChange = { trigger = it },
                    label = { Text("Trigger word") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = emojis, onValueChange = { emojis = it },
                    label = { Text("Emoji sequence (space-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
                Spacer(Modifier.height(12.dp))
                Text("Trigger mode", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "whole",
                        onClick = { mode = "whole" },
                        label = { Text("Whole word") }
                    )
                    FilterChip(
                        selected = mode == "partial",
                        onClick = { mode = "partial" },
                        label = { Text("Partial / prefix") }
                    )
                }
            }
        }
    )
}
