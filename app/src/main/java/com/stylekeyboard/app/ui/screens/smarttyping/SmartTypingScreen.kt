package com.stylekeyboard.app.ui.screens.smarttyping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Broom
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stylekeyboard.app.data.db.entity.UserDictionaryEntity
import com.stylekeyboard.app.ui.components.GlowingButton
import com.stylekeyboard.app.ui.components.SectionCard
import com.stylekeyboard.app.ui.theme.DangerRed
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientEnd
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary

@Composable
fun SmartTypingScreen(vm: SmartTypingViewModel = viewModel()) {
    val topWords by vm.topWords.collectAsState()
    val topBigrams by vm.topBigrams.collectAsState()
    val userDict by vm.userDictionary.collectAsState()

    var addDialog by remember { mutableStateOf(false) }
    var clearConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Smart Typing", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("On-device predictive text. Nothing leaves your phone.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowingButton("Add word", onClick = { addDialog = true }, modifier = Modifier.weight(1f))
            GlowingButton(
                "Clear History",
                onClick = { clearConfirm = true },
                modifier = Modifier.weight(1f),
                outlineColor = DangerRed
            )
        }
        Spacer(Modifier.height(16.dp))

        SectionCard("User Dictionary") {
            if (userDict.isEmpty()) {
                Text("No user-added words yet. Add names, slang or anything you type often.", color = TextSecondary, fontSize = 12.sp)
            } else {
                userDict.forEach { entry ->
                    UserDictRow(entry, onDelete = { vm.removeWord(entry.word) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionCard("Top Learned Words (${topWords.size})") {
            if (topWords.isEmpty()) {
                Text("No learned words yet. As you type with the keyboard, words you confirm will appear here ranked by frequency and recency.", color = TextSecondary, fontSize = 12.sp)
            } else {
                topWords.take(40).forEach { w ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(w.word, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                        Text("×${w.frequency}", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionCard("Top Bigrams (${topBigrams.size})") {
            if (topBigrams.isEmpty()) {
                Text("Bigrams build as you type consecutive words (e.g. \"good morning\").", color = TextSecondary, fontSize = 12.sp)
            } else {
                topBigrams.take(30).forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${b.firstWord} → ${b.secondWord}", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text("×${b.frequency}", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (addDialog) {
        var newWord by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (newWord.isNotBlank()) vm.addWord(newWord.trim())
                    addDialog = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Cancel") } },
            title = { Text("Add to user dictionary", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    label = { Text("Word") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    if (clearConfirm) {
        AlertDialog(
            onDismissRequest = { clearConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearHistory()
                    clearConfirm = false
                }) { Text("Wipe", color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("Cancel") } },
            title = { Text("Clear learned history?", color = TextPrimary) },
            text = { Text("Removes all learned word frequencies and bigrams/trigrams. User-added words and the seeded base dictionary are kept.", color = TextSecondary, fontSize = 13.sp) }
        )
    }
}

@Composable
private fun UserDictRow(entry: UserDictionaryEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(entry.word, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = TextSecondary) }
        }
    }
}
