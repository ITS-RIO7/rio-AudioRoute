package com.example.ui.components

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.AudioStreamConfig
import com.example.data.model.RouteTarget
import com.example.ui.theme.CallAmber
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StreamMatrixCard(
    config: AudioStreamConfig,
    isServiceRunning: Boolean,
    onToggleService: () -> Unit,
    onSetMusicRoute: (RouteTarget) -> Unit,
    onSetCallRoute: (RouteTarget) -> Unit,
    onSetNotificationRoute: (RouteTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stream_matrix_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Routing Matrix",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Stream Routing Rules",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Assign destination hardware per stream",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stream 1: Songs & Music
            StreamSelectorRow(
                icon = Icons.Default.MusicNote,
                title = "Music & Media (Songs)",
                subtitle = "Spotify, YouTube, Local tracks",
                currentTarget = config.musicTarget,
                accentColor = ElectricCyan,
                onSelectTarget = onSetMusicRoute
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stream 2: Calls & Telecom
            StreamSelectorRow(
                icon = Icons.Default.Call,
                title = "Calls & Phone Voice",
                subtitle = "Incoming calls, Dialer, WhatsApp voice",
                currentTarget = config.callTarget,
                accentColor = CallAmber,
                onSelectTarget = onSetCallRoute
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stream 3: Notifications & Alarms
            StreamSelectorRow(
                icon = Icons.Default.Notifications,
                title = "Notifications & Ringtone",
                subtitle = "Alert sounds, Messages, Timer",
                currentTarget = config.notificationTarget,
                accentColor = Color(0xFFA855F7),
                onSelectTarget = onSetNotificationRoute
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Persistent Routing Service Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A))
                    .border(
                        1.dp,
                        if (isServiceRunning) StatusGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceRunning) StatusGreen else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Background Audio Guard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = if (isServiceRunning)
                                "Actively maintaining split audio during background playback"
                            else
                                "Run service to enforce call speaker routing across all apps",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { onToggleService() },
                        modifier = Modifier.testTag("service_toggle_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StatusGreen
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamSelectorRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    currentTarget: RouteTarget,
    accentColor: Color,
    onSelectTarget: (RouteTarget) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0B101D))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MiniTargetOption(
                label = "Bluetooth",
                icon = Icons.Default.Headphones,
                isSelected = currentTarget == RouteTarget.BLUETOOTH,
                color = ElectricCyan,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTarget(RouteTarget.BLUETOOTH) }
            )
            MiniTargetOption(
                label = "Phone Speaker",
                icon = Icons.Default.PhoneAndroid,
                isSelected = currentTarget == RouteTarget.SPEAKER,
                color = CallAmber,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTarget(RouteTarget.SPEAKER) }
            )
            MiniTargetOption(
                label = "Default",
                icon = Icons.Default.Widgets,
                isSelected = currentTarget == RouteTarget.DEFAULT,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTarget(RouteTarget.DEFAULT) }
            )
        }
    }
}

@Composable
private fun MiniTargetOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else TextMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextMuted
            )
        }
    }
}
