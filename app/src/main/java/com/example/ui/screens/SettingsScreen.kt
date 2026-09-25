package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.PreferenceManager
import com.example.service.WhatsAppNotificationListener
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryGreen

@Composable
fun SettingsScreen(
    preferenceManager: PreferenceManager,
    isPermissionGranted: Boolean,
    onCheckPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isMasterEnabled by preferenceManager.isMasterEnabled.collectAsState()
    val replyToGroups by preferenceManager.replyToGroups.collectAsState()
    val prependTag by preferenceManager.prependTag.collectAsState()
    val supportWABusiness by preferenceManager.supportWhatsAppBusiness.collectAsState()
    val blacklistedContacts by preferenceManager.blacklistedContacts.collectAsState()

    var blacklistInput by remember(blacklistedContacts) { mutableStateOf(blacklistedContacts) }
    var isGuideExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // App Preferences Header
        item {
            Text(
                text = "General Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // WhatsApp App Integrations
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "WhatsApp Messenger",
                        description = "Enable auto-replies for standard WhatsApp (com.whatsapp)",
                        icon = Icons.Default.Chat,
                        checked = true, // Always active for regular WhatsApp
                        onCheckedChange = {}
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    SettingToggleRow(
                        title = "WhatsApp Business",
                        description = "Enable auto-replies for WhatsApp Business (com.whatsapp.w4b)",
                        icon = Icons.Default.Business,
                        checked = supportWABusiness,
                        onCheckedChange = { preferenceManager.setSupportWhatsAppBusiness(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    SettingToggleRow(
                        title = "Reply to WhatsApp Groups",
                        description = "When disabled, only individual 1-on-1 private messages receive replies",
                        icon = Icons.Default.Group,
                        checked = replyToGroups,
                        onCheckedChange = { preferenceManager.setReplyToGroups(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    SettingToggleRow(
                        title = "Prefix \"[Auto-Reply]\" Tag",
                        description = "Adds [Auto-Reply] to messages so the recipient knows it is automated",
                        icon = Icons.Default.Label,
                        checked = prependTag,
                        onCheckedChange = { preferenceManager.setPrependTag(it) }
                    )
                }
            }
        }

        // Blacklist Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ignored Contacts / Blacklist",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Contacts or numbers listed here will never receive an automated reply. Separate multiple names or numbers with commas.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = blacklistInput,
                        onValueChange = {
                            blacklistInput = it
                            preferenceManager.setBlacklistedContacts(it)
                        },
                        placeholder = { Text("e.g. Boss, Mom, +8801700000000", fontSize = 13.sp) },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blacklist_input")
                    )
                }
            }
        }

        // Notification Permission Control
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isPermissionGranted) AccentGreen else Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Android Notification Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isPermissionGranted) "Granted & Active" else "Not Granted",
                                    fontSize = 12.sp,
                                    color = if (isPermissionGranted) Color(0xFF15803D) else Color(0xFFB45309)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isPermissionGranted) {
                                    WhatsAppNotificationListener.openPermissionSettings(context)
                                } else {
                                    WhatsAppNotificationListener.openAppDetailsSettings(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier.testTag("open_system_settings_btn")
                        ) {
                            Text(if (isPermissionGranted) "Settings" else "Fix Access", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Step by Step Setup Guide (Bangla & English)
        item {
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isGuideExpanded = !isGuideExpanded }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "কীভাবে কাজ করে এবং সেটআপ গাইড",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = if (isGuideExpanded) "সংক্ষেপ" else "বিস্তারিত",
                            fontSize = 12.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "How WhatsApp Auto Reply works on Android and troubleshooting tips.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(visible = isGuideExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            GuideStep(
                                stepNumber = "১",
                                title = "নোটিফিকেশন পারমিশন দিন (Notification Access)",
                                description = "অ্যাপটি যদি APK দিয়ে ইনস্টল করা হয়ে থাকে, আগে App Info থেকে ⋮ -> Allow restricted settings চালু করুন। তারপর Notification Access-এ গিয়ে 'WA AutoReply' অন করুন।"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            GuideStep(
                                stepNumber = "২",
                                title = "হোয়াটসঅ্যাপে প্রিভিউ চালু রাখুন",
                                description = "হোয়াটসঅ্যাপ সেটিংস -> Notifications -> 'Show preview' বা 'High priority notifications' চালু রাখতে হবে যাতে নোটিফিকেশনে মেসেজ পড়া যায়।"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            GuideStep(
                                stepNumber = "৩",
                                title = "ব্যাটারি অপটিমাইজেশন অফ করুন",
                                description = "অ্যান্ড্রয়েড যাতে ব্যাকগ্রাউন্ডে অ্যাপ বন্ধ না করে দেয়, তাই ফোন সেটিংসে গিয়ে এই অ্যাপের জন্য Battery Optimization 'Unrestricted' বা 'Don't optimize' করে দিন।"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            GuideStep(
                                stepNumber = "৪",
                                title = "লাইভ টেস্ট করতে সিমুলেটর ব্যবহার করুন",
                                description = "অন্য ফোন ছাড়া এখনই নিয়মগুলো সঠিকভাবে কাজ করছে কি না দেখতে 'Simulator' ট্যাবে গিয়ে যেকোনো মেসেজ পাঠিয়ে সাথে সাথে স্বয়ংক্রিয় উত্তর দেখুন।"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentGreen
            )
        )
    }
}

@Composable
fun GuideStep(
    stepNumber: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = PrimaryGreen.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
