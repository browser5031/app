package com.imsidetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imsidetector.ui.theme.IMSIDetectorTheme
import com.imsidetector.ui.screens.MainScreen
import com.imsidetector.ui.screens.HistoryScreen
import com.imsidetector.ui.screens.SettingsScreen
import com.imsidetector.ui.viewmodel.DetectorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IMSIDetectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IMSIDetectorApp()
                }
            }
        }
    }
}

@Composable
fun IMSIDetectorApp() {
    val navController = rememberNavController()
    val viewModel: DetectorViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen()
        }
        composable("history") {
            HistoryScreen(
                threats = emptyList(), // TODO: Get from ViewModel
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onClearDataClick = { /* TODO: Clear data */ }
            )
        }
    }
}
