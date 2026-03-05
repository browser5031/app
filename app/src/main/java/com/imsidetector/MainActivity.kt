package com.imsidetector

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imsidetector.ui.theme.IMSIDetectorTheme
import com.imsidetector.ui.screens.MainScreen
import com.imsidetector.ui.screens.HistoryScreen
import com.imsidetector.ui.screens.SettingsScreen
import timber.log.Timber

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate started")

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

        Timber.d("MainActivity onCreate completed")
    }
}

@Composable
fun IMSIDetectorApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen()
        }
        composable("history") {
            HistoryScreen(
                threats = emptyList(),
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onClearDataClick = { }
            )
        }
    }
}
