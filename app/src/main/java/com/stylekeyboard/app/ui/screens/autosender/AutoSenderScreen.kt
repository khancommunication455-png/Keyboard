package com.stylekeyboard.app.ui.screens.autosender

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Stop
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
import com.stylekeyboard.app.data.model.AutoSenderScript
import com.stylekeyboard.app.ui.components.GlowingButton
import com.stylekeyboard.app.ui.components.SectionCard
import com.stylekeyboard.app.ui.theme.AccentPurple
import com.stylekeyboard.app.ui.theme.Charcoal
import com.stylekeyboard.app.ui.theme.DangerRed
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientEnd
import com.stylekeyboard.app.ui.theme.SuccessGreen
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary
import com.stylekeyboard.app.ui.theme.WarnAmber

@Composable
fun AutoSenderScreen(vm: AutoSenderViewModel = viewModel()) {
    val context = LocalContext.current
    val log by vm.log.collectAsState()
    var addDialog by remember { mutableStateOf(false) }
    var intervalDialog by remember { mutableStateOf(false) }

    val accessibilityOn = remember { mutableStateOf(isAccessibilityEnabled(context)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Auto Sender", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Runs a user-supplied list of messages on a schedule or loop. You author the script; the app only executes it.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        // Target
        SectionCard("Target") {
            OutlinedTextField(
                value = vm.script.targetPackage,
                onValueChange = { vm.setTargetPackage(it, vm.script.useAccessibility) },
                label = { Text("Target package (e.g. com.whatsapp)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = !vm.script.useAccessibility,
                    onClick = { vm.setTargetPackage(vm.script.targetPackage, false) },
                    label = { Text("Intent.ACTION_SEND (share-intent)") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = vm.script.useAccessibility,
                    onClick = { vm.setTargetPackage(vm.script.targetPackage, true) },
                    label = { Text("Accessibility (simulated taps)") }
                )
            }
            if (vm.script.useAccessibility) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Accessibility, contentDescription = null, tint = if (accessibilityOn.value) SuccessGreen else WarnAmber)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (accessibilityOn.value) "Accessibility enabled" else "Accessibility NOT enabled — tap to open settings",
                        color = if (accessibilityOn.value) SuccessGreen else WarnAmber,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { openAccessibilitySettings(context) }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Loop controls
        SectionCard("Loop") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = vm.script.loopMode == "once", onClick = { vm.setLoop("once", 1, vm.script.intervalMs, vm.script.perMessageDelayMs) }, label = { Text("Once") })
                FilterChip(selected = vm.script.loopMode == "n_times", onClick = { vm.setLoop("n_times", vm.script.loopCount, vm.script.intervalMs, vm.script.perMessageDelayMs) }, label = { Text("N times") })
                FilterChip(selected = vm.script.loopMode == "infinite", onClick = { vm.setLoop("infinite", vm.script.loopCount, vm.script.intervalMs, vm.script.perMessageDelayMs) }, label = { Text("Infinite") })
            }
            Spacer(Modifier.height(8.dp))
            Text("Interval between sends: ${vm.script.intervalMs / 1000}s  (minimum 3s enforced)", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("Per-message delay: ${vm.script.perMessageDelayMs}ms", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            GlowingButton("Edit timing", onClick = { intervalDialog = true })
        }
        Spacer(Modifier.height(16.dp))

        // Script messages
        SectionCard("Script (${vm.script.messages.size} messages)") {
            if (vm.script.messages.isEmpty()) {
                Text("No messages yet. Add the messages you want to send — the app does NOT author content for you.", color = TextSecondary, fontSize = 12.sp)
            } else {
                vm.script.messages.forEachIndexed { i, m ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Charcoal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}.", color = TextSecondary, modifier = Modifier.width(24.dp))
                            Text(m.text, color = TextPrimary, modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 2)
                            IconButton(onClick = { vm.removeMessage(i) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = DangerRed)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            GlowingButton("Add message", onClick = { addDialog = true })
        }
        Spacer(Modifier.height(16.dp))

        // Run controls
        SectionCard("Run") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RunButton("Start", Icons.Outlined.PlayArrow, SuccessGreen, Modifier.weight(1f)) { vm.start(context) }
                RunButton("Pause", Icons.Outlined.Pause, WarnAmber, Modifier.weight(1f)) { vm.pause(context) }
                RunButton("Stop", Icons.Outlined.Stop, DangerRed, Modifier.weight(1f)) { vm.stop(context) }
            }
            Spacer(Modifier.height(8.dp))
            Text("A persistent notification will appear while running. The STOP ALL button is always visible there.", color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(16.dp))

        // Log
        SectionCard("Run Log (${log.size})") {
            if (log.isEmpty()) {
                Text("Nothing sent yet.", color = TextSecondary, fontSize = 12.sp)
            } else {
                log.take(40).forEach { entry ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = entry.message,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            text = entry.status,
                            color = when (entry.status) { "sent" -> SuccessGreen; "skipped" -> WarnAmber; else -> DangerRed },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (addDialog) {
        var msg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (msg.isNotBlank()) vm.addMessage(msg, vm.script.perMessageDelayMs)
                    addDialog = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Cancel") } },
            title = { Text("Add message", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = msg,
                    onValueChange = { msg = it },
                    label = { Text("Message text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        )
    }

    if (intervalDialog) {
        var intervalSec by remember { mutableStateOf((vm.script.intervalMs / 1000).toString()) }
        var perMsgMs by remember { mutableStateOf(vm.script.perMessageDelayMs.toString()) }
        var loopCount by remember { mutableStateOf(vm.script.loopCount.toString()) }
        AlertDialog(
            onDismissRequest = { intervalDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val interval = (intervalSec.toLongOrNull() ?: 5L).coerceAtLeast(3L)
                    val perMsg = (perMsgMs.toLongOrNull() ?: 1000L).coerceAtLeast(200L)
                    val count = (loopCount.toIntOrNull() ?: 1).coerceAtLeast(1)
                    vm.setLoop(vm.script.loopMode, count, interval * 1000L, perMsg)
                    intervalDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { intervalDialog = false }) { Text("Cancel") } },
            title = { Text("Timing", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(value = intervalSec, onValueChange = { intervalSec = it }, label = { Text("Interval between sends (seconds, min 3)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = perMsgMs, onValueChange = { perMsgMs = it }, label = { Text("Per-message delay (ms)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = loopCount, onValueChange = { loopCount = it }, label = { Text("Loop count (if N times)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        )
    }
}

@Composable
private fun RunButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = text, tint = color)
            Spacer(Modifier.width(6.dp))
            Text(text, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
