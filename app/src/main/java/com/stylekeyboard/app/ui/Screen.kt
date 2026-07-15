package com.stylekeyboard.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BorderStyle
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Outlined.BorderStyle)
    data object Presets : Screen("presets", "My Presets", Icons.Outlined.Palette)
    data object EmojiLab : Screen("emoji_lab", "Emoji Lab", Icons.Outlined.EmojiEmotions)
    data object SmartTyping : Screen("smart_typing", "Smart Typing", Icons.Outlined.SmartToy)
    data object Appearance : Screen("appearance", "Appearance", Icons.Outlined.Palette)
    data object AutoSender : Screen("auto_sender", "Auto Sender", Icons.Outlined.Send)
    data object EnableKeyboard : Screen("enable_keyboard", "Enable Keyboard", Icons.Outlined.Keyboard)
    data object AutoAwesome : Screen("auto_awesome", "Shortcuts", Icons.Outlined.AutoAwesome)
}
