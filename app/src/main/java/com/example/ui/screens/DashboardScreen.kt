package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.AudioTestCard
import com.example.ui.components.BluetoothDeviceCard
import com.example.ui.components.HeroStreamVisualizer
import com.example.ui.components.StreamMatrixCard
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroStreamVisualizer(
                settings = uiState.settings,
                hardwareStatus = uiState.hardwareStatus,
                onToggleDualRouting = { viewModel.toggleDualRouting() }
            )
        }

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

        item {
            StreamMatrixCard(
                config = uiState.settings,
                isServiceRunning = uiState.isServiceRunning,
                onToggleService = { viewModel.toggleService() },
                onSetMusicRoute = { viewModel.setMusicRoute(it) },
                onSetCallRoute = { viewModel.setCallRoute(it) },
                onSetNotificationRoute = { viewModel.setNotificationRoute(it) }
            )
        }

        item {
            AudioTestCard(
                isMusicPlaying = uiState.isMusicTestPlaying,
                isSpeakerPlaying = uiState.isSpeakerTestPlaying,
                isCallPlaying = uiState.isCallToneTestPlaying,
                hardwareStatus = uiState.hardwareStatus,
                onPlayMusicBt = { viewModel.playMusicTest(com.example.data.model.RouteTarget.BLUETOOTH) },
                onPlaySpeakerOnly = { viewModel.playSpeakerOnlyTest() },
                onPlayCall = { viewModel.playCallTest() },
                onStopAll = { viewModel.stopAllTests() },
                onToggleForceSpeaker = { viewModel.forceSpeakerOutput(!uiState.hardwareStatus.isSpeakerphoneForced) },
                onRequestFocus = { viewModel.requestAudioFocus(true) }
            )
        }
    }
}
