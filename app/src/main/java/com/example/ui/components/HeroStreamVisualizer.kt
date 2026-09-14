package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioHardwareStatus
import com.example.data.model.AudioStreamConfig
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CallAmber
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HeroStreamVisualizer(
    settings: AudioStreamConfig,
    hardwareStatus: AudioHardwareStatus,
    onToggleDualRouting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_stream_visualizer_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (settings.isDualRoutingActive) ElectricCyan.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (settings.isDualRoutingActive) StatusGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (settings.isDualRoutingActive) "DUAL AUDIO ROUTING ACTIVE" else "ROUTING PAUSED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = if (settings.isDualRoutingActive) StatusGreen else TextMuted
                        )
                    }
                    Text(
                        text = "Split Audio Engine",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Switch(
                    checked = settings.isDualRoutingActive,
                    onCheckedChange = { onToggleDualRouting() },
                    modifier = Modifier.testTag("master_dual_route_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ElectricCyan,
                        uncheckedTrackColor = Color(0xFF374151)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Split Branches
            // Stream A: Music -> Bluetooth
            StreamBranchCard(
                icon = Icons.Default.MusicNote,
                streamTitle = "SONGS & MUSIC",
                streamSubtitle = "Media audio stream",
                targetIcon = Icons.Default.BluetoothConnected,
                targetTitle = hardwareStatus.connectedDeviceName ?: "Bluetooth Device",
                targetSubtitle = if (hardwareStatus.isBluetoothA2dpConnected) "Connected (A2DP)" else "Ready to pair",
                accentColor = ElectricCyan,
                glowColor = CyanGlow,
                isActive = settings.isDualRoutingActive,
                pulseModifier = if (settings.isDualRoutingActive) Modifier.scale(pulseScale) else Modifier
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stream B: Calls -> Phone Speaker
            StreamBranchCard(
                icon = Icons.Default.Call,
                streamTitle = "CALLS & OTHERS",
                streamSubtitle = "Voice & communication stream",
                targetIcon = Icons.Default.PhoneAndroid,
                targetTitle = "Phone Built-in Speaker",
                targetSubtitle = if (hardwareStatus.isCommunicationDeviceSpeaker) "Active on Speaker" else "Enforced on call",
                accentColor = CallAmber,
                glowColor = AmberGlow,
                isActive = settings.isDualRoutingActive,
                pulseModifier = Modifier
            )
        }
    }
}

@Composable
private fun StreamBranchCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    streamTitle: String,
    streamSubtitle: String,
    targetIcon: androidx.compose.ui.graphics.vector.ImageVector,
    targetTitle: String,
    targetSubtitle: String,
    accentColor: Color,
    glowColor: Color,
    isActive: Boolean,
    pulseModifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(
                width = 1.dp,
                color = if (isActive) accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Source Stream
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = streamTitle,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = streamTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = streamSubtitle,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Glowing Arrow / Flow Connector
            Text(
                text = "➔",
                fontSize = 18.sp,
                color = if (isActive) accentColor else TextMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Destination Device
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.1f),
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = targetTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = targetSubtitle,
                        fontSize = 11.sp,
                        color = if (isActive) accentColor else TextMuted
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .then(pulseModifier)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = targetIcon,
                        contentDescription = targetTitle,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
