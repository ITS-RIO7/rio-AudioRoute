package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BluetoothDeviceCard
import com.example.ui.theme.CallAmber
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun BluetoothDevicesScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Paired Devices Card
        item {
            BluetoothDeviceCard(
                devices = uiState.bluetoothDevices,
                hardwareStatus = uiState.hardwareStatus,
                preferredAddress = uiState.settings.preferredBluetoothAddress,
                onSelectDevice = { viewModel.selectBluetoothDevice(it) },
                onRefresh = { viewModel.refreshBluetoothDevices() },
                onOpenSettings = { viewModel.openBluetoothSettings() }
            )
        }

        // Quick System Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.openBluetoothSettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("system_bt_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF0A0E17)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Pair New Device",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A0E17)
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.openSoundSettings() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("system_sound_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sound Settings", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }

        // Audio Hardware Diagnostics Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hardware_diagnostics_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Real-Time Audio Hardware Status",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Active system sink states queried from Android AudioManager",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DiagnosticRow(
                        title = "Bluetooth Media (A2DP)",
                        value = if (uiState.hardwareStatus.isBluetoothA2dpConnected)
                            "Connected (${uiState.hardwareStatus.connectedDeviceName ?: "Active"})"
                        else "No Bluetooth Audio Sink",
                        isGood = uiState.hardwareStatus.isBluetoothA2dpConnected
                    )

                    DiagnosticRow(
                        title = "Communication Device (Calls)",
                        value = uiState.hardwareStatus.communicationDeviceName,
                        isGood = uiState.hardwareStatus.isCommunicationDeviceSpeaker
                    )

                    DiagnosticRow(
                        title = "Speaker Enforced for Calls",
                        value = if (uiState.hardwareStatus.isCommunicationDeviceSpeaker) "Yes (Built-in Speaker)" else "Default Sink",
                        isGood = uiState.hardwareStatus.isCommunicationDeviceSpeaker
                    )

                    DiagnosticRow(
                        title = "Audio Manager Mode",
                        value = uiState.hardwareStatus.audioMode,
                        isGood = true
                    )

                    DiagnosticRow(
                        title = "Telephony Call State",
                        value = uiState.hardwareStatus.currentCallState,
                        isGood = true
                    )
                }
            }
        }

        // How It Works Explainer Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("how_it_works_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How Split Sound Works",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "1. Songs & Media: Routed to your selected Bluetooth device via A2DP high-definition audio.\n\n" +
                               "2. Phone Calls & Voip: Routed to your Phone Built-in Speaker using AudioManager setCommunicationDevice, so you can converse hands-free on phone speaker without disconnecting your headphones.\n\n" +
                               "3. Per-App Selector: Set rules for each app on your phone in the 'App Rules' tab.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    value: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isGood) StatusGreen else TextMuted
        )
    }
}
