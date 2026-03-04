package com.imsidetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imsidetector.data.CurrentCellState
import com.imsidetector.ui.theme.GreenThreat
import com.imsidetector.ui.theme.OrangeThreat
import com.imsidetector.ui.theme.RedThreat
import com.imsidetector.ui.theme.YellowThreat

/**
 * Main application screen with threat dashboard.
 * Design Philosophy: Security-focused Material Design 3 with clear threat visualization.
 * Color Coding: Green (safe) → Yellow (caution) → Orange (warning) → Red (critical)
 * Layout: Threat indicator at top, detailed info below, navigation at bottom
 */
@Composable
fun MainScreen() {
    val currentCell = remember { mutableStateOf(CurrentCellState()) }
    val threatLevel = remember { mutableStateOf("GREEN") }
    val threatScore = remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "IMSI Detector",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Threat Level Indicator Card
            ThreatIndicatorCard(
                threatLevel = threatLevel.value,
                threatScore = threatScore.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Current Cell Information Card
            CurrentCellCard(
                cellState = currentCell.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Signal Strength Card
            SignalStrengthCard(
                signalStrength = currentCell.value.signalStrength,
                signalLevel = currentCell.value.signalLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Encryption Status Card
            EncryptionStatusCard(
                cipherStatus = currentCell.value.cipherStatus,
                cipherAlgorithm = currentCell.value.cipherAlgorithm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionCard(
                    title = "Map",
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Open map */ }
                )
                
                ActionCard(
                    title = "History",
                    icon = Icons.Default.Info,
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Open history */ }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Threat indicator card showing current threat level and score.
 */
@Composable
fun ThreatIndicatorCard(
    threatLevel: String,
    threatScore: Int,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (threatLevel) {
        "GREEN" -> GreenThreat.copy(alpha = 0.1f)
        "YELLOW" -> YellowThreat.copy(alpha = 0.1f)
        "ORANGE" -> OrangeThreat.copy(alpha = 0.1f)
        "RED" -> RedThreat.copy(alpha = 0.1f)
        else -> Color.Gray.copy(alpha = 0.1f)
    }
    
    val borderColor = when (threatLevel) {
        "GREEN" -> GreenThreat
        "YELLOW" -> YellowThreat
        "ORANGE" -> OrangeThreat
        "RED" -> RedThreat
        else -> Color.Gray
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Threat Level",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = threatLevel,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = borderColor,
                fontSize = 48.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Threat Score: $threatScore/100",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card displaying current cell information.
 */
@Composable
fun CurrentCellCard(
    cellState: CurrentCellState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Current Cell",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CellInfoRow("Operator", cellState.operatorName)
            CellInfoRow("Network Type", cellState.networkType)
            CellInfoRow("LAC/TAC", "${cellState.lac}/${cellState.tac}")
            CellInfoRow("Cell ID", cellState.cid.toString())
            CellInfoRow("Roaming", if (cellState.roaming) "Yes" else "No")
        }
    }
}

/**
 * Card displaying signal strength information.
 */
@Composable
fun SignalStrengthCard(
    signalStrength: Int,
    signalLevel: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Signal Strength",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CellInfoRow("RSSI/RSRP", "$signalStrength dBm")
            CellInfoRow("Signal Level", "$signalLevel/4 bars")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Signal strength visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                val fillPercentage = ((signalLevel + 1) / 5f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillPercentage)
                        .height(8.dp)
                        .background(
                            color = when {
                                signalLevel >= 3 -> GreenThreat
                                signalLevel >= 2 -> YellowThreat
                                else -> RedThreat
                            },
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}

/**
 * Card displaying encryption status.
 */
@Composable
fun EncryptionStatusCard(
    cipherStatus: String,
    cipherAlgorithm: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (cipherStatus) {
        "ENCRYPTED" -> GreenThreat
        "DOWNGRADED" -> OrangeThreat
        "UNENCRYPTED" -> RedThreat
        else -> Color.Gray
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Encryption Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(statusColor, shape = MaterialTheme.shapes.small)
                        .padding(8.dp)
                ) {
                    Text(
                        text = cipherStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = cipherAlgorithm,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Action card for quick navigation.
 */
@Composable
fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .height(32.dp)
                    .fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Helper composable for displaying cell information rows.
 */
@Composable
fun CellInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
