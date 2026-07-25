package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.FloatingBubbleScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReshaperHomeScreen
import com.example.ui.theme.ArabicReshaperTheme

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            mainViewModel = viewModel()

            val isDarkModePref by mainViewModel.isDarkMode.collectAsState()
            val useDarkTheme = isDarkModePref ?: isSystemInDarkTheme()

            ArabicReshaperTheme(darkTheme = useDarkTheme) {
                // Force Right-To-Left layout direction for Arabic interface
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var selectedTab by remember { mutableStateOf(0) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .testTag("main_navigation_bar")
                                ) {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = { Icon(Icons.Default.TextFields, contentDescription = "Reshaper") },
                                        label = { Text("المشكّل") },
                                        modifier = Modifier.testTag("tab_reshaper")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = { Icon(Icons.Default.Widgets, contentDescription = "Floating Bubble") },
                                        label = { Text("الفقاعة العائمة") },
                                        modifier = Modifier.testTag("tab_bubble")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                        label = { Text("السجل") },
                                        modifier = Modifier.testTag("tab_history")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (selectedTab) {
                                    0 -> ReshaperHomeScreen(viewModel = mainViewModel)
                                    1 -> FloatingBubbleScreen(viewModel = mainViewModel)
                                    2 -> HistoryScreen(viewModel = mainViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mainViewModel.isInitialized) {
            mainViewModel.checkOverlayPermission()
        }
    }
}
