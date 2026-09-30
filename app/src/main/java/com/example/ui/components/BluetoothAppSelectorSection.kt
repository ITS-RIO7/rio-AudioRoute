package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.AppAudioRule
import com.example.data.model.AppCategory
import com.example.data.model.RouteTarget
import com.example.ui.theme.CallAmber
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BluetoothAppSelectorHeader(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val totalApps = uiState.appRules.size
    val btAppsCount = uiState.appRules.count { it.routeTarget == RouteTarget.BLUETOOTH }
    val speakerAppsCount = uiState.appRules.count { it.routeTarget != RouteTarget.BLUETOOTH }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bluetooth_app_selector_header"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.5f))
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bluetooth App Sound Router",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select apps to output audio via Bluetooth",
                            fontSize = 12.sp,
                            color = ElectricCyan
                        )
                    }
                }

                // Counter pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$btAppsCount / $totalApps ON BT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Jis app ko aap select karenge, sirf usi app ka sound Bluetooth me aayega. Baaki sabhi unselected apps ka sound phone ke built-in speaker me bajega.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Batch Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.setAllAppsBluetoothRouting(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_all_bt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF0A0E17)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0E17))
                }

                Button(
                    onClick = { viewModel.setAllAppsBluetoothRouting(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("deselect_all_bt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveDone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deselect All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Button(
                    onClick = { viewModel.setMusicOnlyBluetoothRouting() },
                    colors = ButtonDefaults.buttonColors(containerColor = CallAmber.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("music_only_bt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CallAmber
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Music Only", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CallAmber)
                }
            }
        }
    }
}

@Composable
fun BluetoothAppSelectorItem(
    rule: AppAudioRule,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBluetooth = rule.routeTarget == RouteTarget.BLUETOOTH

    val appIconState = produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, rule.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(rule.packageName)
                val bitmap = drawable.toBitmap(width = 96, height = 96)
                bitmap.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bt_app_item_${rule.packageName}")
            .clickable { onToggle(!isBluetooth) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBluetooth) Color(0xFF0F2236) else DarkSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isBluetooth) 1.5.dp else 1.dp,
            color = if (isBluetooth) ElectricCyan.copy(alpha = 0.6f) else Color(0xFF1F2937)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (appIconState.value != null) {
                Image(
                    bitmap = appIconState.value!!,
                    contentDescription = rule.appName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isBluetooth) ElectricCyan.copy(alpha = 0.2f) else Color(0xFF374151)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (rule.category) {
                            AppCategory.MUSIC -> Icons.Default.MusicNote
                            AppCategory.COMMUNICATION -> Icons.Default.Call
                            AppCategory.VIDEO -> Icons.Default.Videocam
                            else -> Icons.Default.Apps
                        },
                        contentDescription = null,
                        tint = if (isBluetooth) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Name & Status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.appName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isBluetooth) ElectricCyan else CallAmber)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBluetooth) "Bluetooth 🎧  •  ${rule.audioReason}" else "Phone Speaker 🔊  •  ${rule.audioReason}",
                        fontSize = 10.sp,
                        fontWeight = if (isBluetooth) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isBluetooth) ElectricCyan else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Toggle Switch
            Switch(
                checked = isBluetooth,
                onCheckedChange = { onToggle(it) },
                modifier = Modifier.testTag("toggle_bt_${rule.packageName}"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ElectricCyan,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF1E293B)
                )
            )
        }
    }
}
