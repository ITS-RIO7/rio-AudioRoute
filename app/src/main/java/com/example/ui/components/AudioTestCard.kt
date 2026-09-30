package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.data.model.AudioFocusStatus
import com.example.data.model.AudioHardwareStatus
import com.example.ui.theme.CallAmber
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AudioTestCard(
    isMusicPlaying: Boolean,
    isSpeakerPlaying: Boolean,
    isCallPlaying: Boolean,
    hardwareStatus: AudioHardwareStatus,
    onPlayMusicBt: () -> Unit,
    onPlaySpeakerOnly: () -> Unit,
    onPlayCall: () -> Unit,
    onStopAll: () -> Unit,
    onToggleForceSpeaker: () -> Unit,
    onRequestFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnyPlaying = isMusicPlaying || isSpeakerPlaying || isCallPlaying
    val isSpeakerForced = hardwareStatus.isSpeakerphoneForced || hardwareStatus.isCommunicationDeviceSpeaker
    val isFocusHeld = hardwareStatus.audioFocusStatus == AudioFocusStatus.FOCUS_GAINED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_test_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
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
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Test Audio",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Audio Routing & Focus Tester",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Bluetooth aur Phone Speaker ka audio test karein",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (isAnyPlaying) {
                    OutlinedButton(
                        onClick = onStopAll,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("stop_all_audio_button")
                    ) {
                        Text("Stop", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Focus & Route Status Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio Focus Indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isFocusHeld) StatusGreen.copy(alpha = 0.15f) else Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = if (isFocusHeld) StatusGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isFocusHeld) "Audio Focus: Held" else "Focus: Standby",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFocusHeld) StatusGreen else TextSecondary
                            )
                            Text(
                                text = "System Audio Control",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Speaker Override Indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSpeakerForced) CallAmber.copy(alpha = 0.15f) else Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isSpeakerForced) CallAmber else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isSpeakerForced) "Speaker: Forced" else "Route: Default",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpeakerForced) CallAmber else TextSecondary
                            )
                            Text(
                                text = if (isSpeakerForced) "Bypassing BT" else "Following BT",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test 1: Phone Speaker Direct Output (SOLVES: "audio nhi aa rha gab blootooth connect hota")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CallAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = CallAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Test on Phone Speaker 🔊",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "BT connect hone par bhi direct phone speaker me bajega",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = onPlaySpeakerOnly,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeakerPlaying) Color(0xFFEF4444) else CallAmber
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("test_speaker_only_button")
                ) {
                    Icon(
                        imageVector = if (isSpeakerPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF0A0E17)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSpeakerPlaying) "Playing" else "Speaker",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A0E17)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test 2: Music on Bluetooth
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Test on Bluetooth 🎧",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Synth arpeggio connected Bluetooth me bajega",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = onPlayMusicBt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMusicPlaying) Color(0xFFEF4444) else ElectricCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("test_music_bt_button")
                ) {
                    Icon(
                        imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF0A0E17)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMusicPlaying) "Playing" else "Bluetooth",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A0E17)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test 3: Call Ringtone
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(StatusGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Test Call Ringtone 📞",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Voice stream routed to Phone Speaker",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = onPlayCall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCallPlaying) Color(0xFFEF4444) else StatusGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("test_call_button")
                ) {
                    Icon(
                        imageVector = if (isCallPlaying) Icons.Default.Pause else Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF0A0E17)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCallPlaying) "Ringing" else "Ring",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A0E17)
                    )
                }
            }
        }
    }
}
