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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.theme.DarkSurface
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
    val isRunning = settings.isDualRoutingActive && settings.isServiceRunning

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
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
            if (isRunning) StatusGreen.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with status indicator
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
                                .background(if (isRunning) StatusGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "ENGINE RUNNING NON-STOP" else "ROUTER INACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = if (isRunning) StatusGreen else TextMuted
                        )
                    }
                    Text(
                        text = "Split Audio Engine",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Switch(
                    checked = isRunning,
                    onCheckedChange = { onToggleDualRouting() },
                    modifier = Modifier.testTag("master_dual_route_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StatusGreen,
                        uncheckedTrackColor = Color(0xFF374151)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // THE MASTER ACTIVE / DEACTIVATE BUTTON (User Requirement: "Ek active botton do gese hi active kAre to gab tak deactivate nhi tab tak...")
            Button(
                onClick = onToggleDualRouting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("master_engine_active_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFDC2626) else ElectricCyan
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isRunning) Color.White else Color(0xFF0A0E17)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isRunning) "DEACTIVATE SOUND ROUTER" else "ACTIVATE SOUND ROUTER",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = if (isRunning) Color.White else Color(0xFF0A0E17)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Continuous Active Badges (Lock screen & Auto-BT proof)
            if (isRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B192C))
                        .border(1.dp, StatusGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lock Screen Active: Screen lock hone par bhi audio alag alag bajega",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusGreen
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-Bluetooth Reconnect: Bluetooth pass aate hi auto route hoga",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricCyan
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CallAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Audio Focus Protected: Deactivate karne tak background me chalta rahega",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CallAmber
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Active button dabane par service background aur lock screen me lagatar chalegi, jab tak aap Deactivate nahi karenge.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
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
                targetSubtitle = if (hardwareStatus.isBluetoothA2dpConnected) "Connected (A2DP)" else "Ready to auto-connect",
                accentColor = ElectricCyan,
                glowColor = CyanGlow,
                isActive = isRunning,
                pulseModifier = if (isRunning) Modifier.scale(pulseScale) else Modifier
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stream B: Calls -> Phone Speaker
            StreamBranchCard(
                icon = Icons.Default.Call,
                streamTitle = "CALLS & OTHERS",
                streamSubtitle = "Voice & communication stream",
                targetIcon = Icons.Default.PhoneAndroid,
                targetTitle = "Phone Built-in Speaker",
                targetSubtitle = if (hardwareStatus.isCommunicationDeviceSpeaker || hardwareStatus.isSpeakerphoneForced)
                    "Routed to Speaker (${hardwareStatus.audioFocusStatus.label})"
                else
                    "Enforced on call & lock screen",
                accentColor = CallAmber,
                glowColor = AmberGlow,
                isActive = isRunning,
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
                1.dp,
                if (isActive) accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source stream icon
            Box(
                modifier = pulseModifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isActive) 0.2f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) accentColor else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = streamTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = if (isActive) accentColor else TextMuted
                )
                Text(
                    text = targetTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = targetSubtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Target destination icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = targetIcon,
                    contentDescription = null,
                    tint = if (isActive) accentColor else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
