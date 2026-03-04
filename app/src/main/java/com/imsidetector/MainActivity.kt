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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imsidetector.ui.theme.IMSIDetectorTheme
import com.imsidetector.ui.screens.MainScreen
import com.imsidetector.ui.screens.HistoryScreen
import com.imsidetector.ui.screens.SettingsScreen
import com.imsidetector.ui.viewmodel.DetectorViewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "MainActivity onCreate started")
        
        try {
            Timber.d("MainActivity onCreate started")
            
            setContent {
                IMSIDetectorTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        try {
                            IMSIDetectorApp()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in IMSIDetectorApp", e)
                            Timber.e(e, "Error in IMSIDetectorApp")
                            ErrorScreen(e)
                        }
                    }
                }
            }
            
            Log.d(TAG, "MainActivity onCreate completed successfully")
            Timber.d("MainActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in MainActivity onCreate", e)
            Timber.e(e, "Error in MainActivity onCreate")
            
            // Fallback to a very simple error screen
            try {
                setContent {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ErrorScreen(e)
                    }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Even error screen failed", e2)
            }
        }
    }
}

@Composable
fun ErrorScreen(error: Exception) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️ Error Loading App",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error.message ?: "Unknown error",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error.javaClass.simpleName,
            style = MaterialTheme.typography.bodySmall
        )
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
