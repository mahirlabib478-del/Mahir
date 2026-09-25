package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReplyLog
import com.example.service.WhatsAppNotificationListener
import com.example.ui.ScreenTab
import com.example.ui.components.PlatformBadge
import com.example.ui.components.SenderAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    isMasterEnabled: Boolean,
    isPermissionGranted: Boolean,
    totalRepliesCount: Int,
    enabledRulesCount: Int,
    recentLogs: List<ReplyLog>,
    onToggleMaster: () -> Unit,
    onNavigateTab: (ScreenTab) -> Unit,
    onResetPresets: () -> Unit,
    onCheckPermission: () -> Unit,
    onReconnectService: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isTroubleshootingExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Switch Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMasterEnabled)
                        Color(0xFF075E54)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_switch_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isMasterEnabled) AccentGreen else Color.LightGray,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMasterEnabled) "Auto-Reply is ACTIVE" else "Auto-Reply is PAUSED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isMasterEnabled) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMasterEnabled)
                                "Listening for WhatsApp notifications and sending replies automatically."
                            else
                                "Turn ON to resume automatic WhatsApp responses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMasterEnabled) Color(0xFFD1E7DD) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isMasterEnabled,
                        onCheckedChange = { onToggleMaster() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("master_toggle_switch")
                    )
                }
            }
        }

        // Permission Card
        item {
            if (!isPermissionGranted) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFBEB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permission_warning_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Permission Needed",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Notification Access Required",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "হোয়াটসঅ্যাপের মেসেজে স্বয়ংক্রিয় রিপ্লাই দিতে Android 'Notification Access' পারমিশন দেওয়া বাধ্যতামূলক। নিচের বাটনে ট্যাপ করে পারমিশন দিন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ Android 13/14+ নোট: সেটিংসে পারমিশন সুইচ গ্রে বা বন্ধ থাকলে 'App Info' বাটনে ট্যাপ করে উপরে ৩ ডট (⋮) থেকে 'Allow restricted settings' চালু করে আসুন।",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    WhatsAppNotificationListener.openPermissionSettings(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD97706),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("enable_permission_button")
                            ) {
                                Text("Enable Access", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    WhatsAppNotificationListener.openAppDetailsSettings(context)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("App Info (⋮)", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { onCheckPermission() },
                                modifier = Modifier.testTag("recheck_permission_button")
                            ) {
                                Text("Check", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Permission Granted",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Notification Listener is Active",
                                    color = Color(0xFF1B5E20),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ready to receive WhatsApp notifications",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                onReconnectService()
                                onCheckPermission()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // WhatsApp Testing Troubleshooting Guide
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTroubleshootingExpanded = !isTroubleshootingExpanded }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "WhatsApp এ কাজ না করার প্রধান কারণগুলো",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = if (isTroubleshootingExpanded) "Hide" else "Show",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "টেস্ট করার সময় রিপ্লাই না যাওয়ার সাধারণ ৫টি সমাধান দেখুন।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(visible = isTroubleshootingExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            TroubleItem(
                                number = "১",
                                title = "টেস্ট করার সময় WhatsApp বন্ধ বা স্ক্রিন লক রাখুন",
                                description = "আপনার ফোনে হোয়াটসঅ্যাপ ওপেন বা চ্যাট খোলা থাকলে হোয়াটসঅ্যাপ নোটিফিকেশন পাঠায় না। হোয়াটসঅ্যাপ পুরোপুরি মিনিমাইজ বা স্ক্রিন অফ করে অন্য ফোন থেকে মেসেজ দিন।"
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            TroubleItem(
                                number = "২",
                                title = "WhatsApp নোটিফিকেশন প্রিভিউ চালু থাকতে হবে",
                                description = "হোয়াটসঅ্যাপ সেটিংস -> Notifications -> 'High priority notifications' চালু থাকতে হবে যাতে নোটিফিকেশনে কুইক-রিপ্লাই অপশন আসে।"
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            TroubleItem(
                                number = "৩",
                                title = "কুলডাউন (Cooldown) সময় দেখুন",
                                description = "একই ব্যক্তি পরপর মেসেজ পাঠালে সাথে সাথে বারবার স্প্যাম না করার জন্য ডিফল্ট ৩০ সেকেন্ড কুলডাউন থাকে। ৩০ সেকেন্ড পর মেসেজ দিলে আবার রিপ্লাই যাবে।"
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            TroubleItem(
                                number = "৪",
                                title = "মেসেজ কি রুলের সাথে মিলছে?",
                                description = "ডিফল্ট রুলে 'hi', 'hello', 'salam', 'price' ইত্যাদি সেট করা আছে। হিস্ট্রি (History) ট্যাবে গিয়ে দেখুন আপনার মেসেজটি এসেছে কি না বা কোনো রুল ম্যাচ হয়েছে কি না।"
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            TroubleItem(
                                number = "৫",
                                title = "ইন-অ্যাপ সিমুলেটরে টেস্ট করুন",
                                description = "কোনো ঝামেলা ছাড়াই সাথে সাথে রুল চেক করতে Simulator ট্যাব ব্যবহার করুন। সেখানে যেকোনো মেসেজ লিখে সেন্ড করে রুল পরীক্ষা করা যায়।"
                            )
                        }
                    }
                }
            }
        }

        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Replies Sent
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sent Replies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = totalRepliesCount.toString(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Active Rules
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTab(ScreenTab.RULES) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Active Rules",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Rule,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = enabledRulesCount.toString(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Live Simulator Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab(ScreenTab.SIMULATOR) }
                    .testTag("simulator_banner_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test Simulator",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Test in Interactive Simulator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Send test WhatsApp messages to verify your rules instantly without another device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Preset Rule Actions
        item {
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Popular Rule Presets",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        FilledTonalButton(
                            onClick = onResetPresets,
                            modifier = Modifier.testTag("import_presets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Presets", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Includes: Greetings (hi/hello/hey), Salam, Pricing, Urgent keyword, and Default away auto-reply.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Recent Activity Preview
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Recent Activity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (recentLogs.isNotEmpty()) {
                    Text(
                        text = "View all",
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onNavigateTab(ScreenTab.HISTORY) }
                            .padding(4.dp)
                    )
                }
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No replies sent yet",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "When incoming WhatsApp messages match your rules, they will appear here.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(recentLogs.take(4)) { log ->
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(log.timestamp))

                val (statusBg, statusFg, statusLabel) = when (log.status) {
                    "SENT" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Live Sent")
                    "SIMULATED" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Simulated")
                    "SKIPPED_COOLDOWN" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Cooldown")
                    "NO_RULE_MATCH" -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), "No Match")
                    "NO_REPLY_ACTION" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "No Quick Reply")
                    "GROUP_IGNORED" -> Triple(Color(0xFFFFEDD5), Color(0xFFC2410C), "Group Ignored")
                    "BLACKLISTED" -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "Blacklisted")
                    "SEND_FAILED" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Send Failed")
                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), log.status)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        SenderAvatar(name = log.sender, isGroup = log.isGroup, platform = log.platform)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = log.sender,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    PlatformBadge(platform = log.platform)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(text = statusLabel, backgroundColor = statusBg, textColor = statusFg)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timeStr,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💬 \"${log.incomingMessage}\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "↳ ${log.repliedText.replace("\n", " ")}",
                                fontSize = 12.sp,
                                color = if (log.status == "SENT") PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TroubleItem(
    number: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Surface(
            color = PrimaryGreen.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = PrimaryGreen
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
