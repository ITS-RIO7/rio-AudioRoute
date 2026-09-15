package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppRuleItemCard(
    rule: AppAudioRule,
    onSelectTarget: (RouteTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
            .testTag("app_rule_card_${rule.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (rule.routeTarget) {
                RouteTarget.BLUETOOTH -> ElectricCyan.copy(alpha = 0.4f)
                RouteTarget.SPEAKER -> CallAmber.copy(alpha = 0.4f)
                RouteTarget.DEFAULT -> Color(0xFF1F2937)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                if (appIconState.value != null) {
                    Image(
                        bitmap = appIconState.value!!,
                        contentDescription = rule.appName,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (rule.category) {
                                    AppCategory.MUSIC -> ElectricCyan.copy(alpha = 0.2f)
                                    AppCategory.COMMUNICATION -> CallAmber.copy(alpha = 0.2f)
                                    else -> Color(0xFF374151)
                                }
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
                            tint = when (rule.category) {
                                AppCategory.MUSIC -> ElectricCyan
                                AppCategory.COMMUNICATION -> CallAmber
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = rule.appName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category pill
                        Text(
                            text = rule.category.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (rule.category) {
                                AppCategory.MUSIC -> ElectricCyan
                                AppCategory.COMMUNICATION -> CallAmber
                                else -> TextMuted
                            },
                            modifier = Modifier
                                .background(
                                    when (rule.category) {
                                        AppCategory.MUSIC -> ElectricCyan.copy(alpha = 0.12f)
                                        AppCategory.COMMUNICATION -> CallAmber.copy(alpha = 0.12f)
                                        else -> Color(0xFF1E293B)
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        Text(
                            text = rule.packageName,
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Bluetooth Switch
                Switch(
                    checked = rule.routeTarget == RouteTarget.BLUETOOTH,
                    onCheckedChange = { isBt ->
                        onSelectTarget(if (isBt) RouteTarget.BLUETOOTH else RouteTarget.SPEAKER)
                    },
                    modifier = Modifier.testTag("app_rule_switch_${rule.packageName}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Way Route Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D131F))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteSegmentButton(
                    icon = Icons.Default.Headphones,
                    label = "Bluetooth",
                    isSelected = rule.routeTarget == RouteTarget.BLUETOOTH,
                    selectedColor = ElectricCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTarget(RouteTarget.BLUETOOTH) }
                )

                RouteSegmentButton(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Speaker",
                    isSelected = rule.routeTarget == RouteTarget.SPEAKER,
                    selectedColor = CallAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTarget(RouteTarget.SPEAKER) }
                )

                RouteSegmentButton(
                    icon = Icons.Default.Settings,
                    label = "Default",
                    isSelected = rule.routeTarget == RouteTarget.DEFAULT,
                    selectedColor = TextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTarget(RouteTarget.DEFAULT) }
                )
            }
        }
    }
}

@Composable
private fun RouteSegmentButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) selectedColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) selectedColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) selectedColor else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextMuted
            )
        }
    }
}
