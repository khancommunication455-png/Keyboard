package com.stylekeyboard.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stylekeyboard.app.ui.screens.appearance.AppearanceScreen
import com.stylekeyboard.app.ui.screens.autosender.AutoSenderScreen
import com.stylekeyboard.app.ui.screens.enablekeyboard.EnableKeyboardScreen
import com.stylekeyboard.app.ui.screens.emojilab.EmojiLabScreen
import com.stylekeyboard.app.ui.screens.home.HomeScreen
import com.stylekeyboard.app.ui.screens.presets.PresetsScreen
import com.stylekeyboard.app.ui.screens.smarttyping.SmartTypingScreen
import com.stylekeyboard.app.ui.theme.Charcoal
import com.stylekeyboard.app.ui.theme.StyleKeyboardTheme
import com.stylekeyboard.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StyleKeyboardTheme {
                AppScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: Screen.Home.route

    val drawerItems = listOf(
        Screen.Home, Screen.Presets, Screen.EmojiLab,
        Screen.SmartTyping, Screen.Appearance, Screen.AutoSender, Screen.EnableKeyboard
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Charcoal) {
                Text(
                    text = "StyleKeyboard",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp)
                )
                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                popUpTo(Screen.Home.route) { saveState = true }
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(screen.icon, contentDescription = null) },
                        colors = NavigationDrawerItemDefaults.colors()
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(drawerItems.firstOrNull { it.route == currentRoute }?.title ?: "StyleKeyboard") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Charcoal,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Charcoal)
                    .padding(inner)
            ) {
                NavHost(navController = navController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) { HomeScreen(navController) }
                    composable(Screen.Presets.route) { PresetsScreen() }
                    composable(Screen.EmojiLab.route) { EmojiLabScreen() }
                    composable(Screen.SmartTyping.route) { SmartTypingScreen() }
                    composable(Screen.Appearance.route) { AppearanceScreen() }
                    composable(Screen.AutoSender.route) { AutoSenderScreen() }
                    composable(Screen.EnableKeyboard.route) { EnableKeyboardScreen() }
                }
            }
        }
    }
}
