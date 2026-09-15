package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.AppCategory
import com.example.data.model.RouteTarget
import com.example.ui.components.AppRuleItemCard
import com.example.ui.theme.CallAmber
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AppRulesScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val bluetoothCount = uiState.appRules.count { it.routeTarget == RouteTarget.BLUETOOTH }
    val speakerCount = uiState.appRules.count { it.routeTarget == RouteTarget.SPEAKER }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary & Fast Action Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_rules_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "rio Per-App Audio Routing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Jis app ko aap select karenge, sirf usi app ka sound Bluetooth me aayega. Baaki sabhi apps phone speaker me bajenge.",
                        fontSize = 12.sp,
                        color = ElectricCyan,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Route Counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricCyan.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$bluetoothCount Apps",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "to Bluetooth",
                                        fontSize = 10.sp,
                                        color = ElectricCyan
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CallAmber.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = CallAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$speakerCount Apps",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "to Phone Speaker",
                                        fontSize = 10.sp,
                                        color = CallAmber
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fast 1-Tap Preset Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.setAllAppsBluetoothRouting(true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("batch_all_bt_button")
                        ) {
                            Text(
                                text = "All -> BT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A0E17)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.setMusicOnlyBluetoothRouting()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("batch_music_bt_button")
                        ) {
                            Text(
                                text = "🎵 Music Only",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricCyan
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.setAllAppsBluetoothRouting(false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CallAmber.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("batch_all_speaker_button")
                        ) {
                            Text(
                                text = "🔊 All Speaker",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CallAmber
                            )
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_text_field"),
                placeholder = { Text("Search apps by name or package...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0xFF1F2937),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("All (${uiState.appRules.size})", fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = Color(0xFF0A0E17),
                        containerColor = Color(0xFF0F172A),
                        labelColor = TextSecondary
                    )
                )

                AppCategory.values().forEach { cat ->
                    val count = uiState.appRules.count { it.category == cat }
                    if (count > 0) {
                        FilterChip(
                            selected = uiState.selectedCategory == cat,
                            onClick = { viewModel.setCategoryFilter(if (uiState.selectedCategory == cat) null else cat) },
                            label = { Text("${cat.label} ($count)", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (cat) {
                                    AppCategory.MUSIC -> ElectricCyan
                                    AppCategory.COMMUNICATION -> CallAmber
                                    else -> Color(0xFF38BDF8)
                                },
                                selectedLabelColor = Color(0xFF0A0E17),
                                containerColor = Color(0xFF0F172A),
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }

        // App List
        if (uiState.filteredApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching apps found",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(uiState.filteredApps, key = { it.packageName }) { rule ->
                AppRuleItemCard(
                    rule = rule,
                    onSelectTarget = { target ->
                        viewModel.updateAppRule(rule.packageName, rule.appName, rule.category.name, target)
                    }
                )
            }
        }
    }
}
