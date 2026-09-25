package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenTab
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    isMasterEnabled: Boolean,
    onTabSelect: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // WhatsApp-like logo dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (isMasterEnabled) AccentGreen else Color.Gray, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WA AutoReply",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            Surface(
                shape = CircleShape,
                color = if (isMasterEnabled) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .testTag("status_indicator_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isMasterEnabled) Color(0xFF16A34A) else Color(0xFFDC2626),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMasterEnabled) "ACTIVE" else "PAUSED",
                        color = if (isMasterEnabled) Color(0xFF166534) else Color(0xFF991B1B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Composable
fun AppBottomNav(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        NavigationBarItem(
            selected = currentTab == ScreenTab.DASHBOARD,
            onClick = { onTabSelected(ScreenTab.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = if (currentTab == ScreenTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "Dashboard"
                )
            },
            label = { Text("Home", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_dashboard")
        )

        NavigationBarItem(
            selected = currentTab == ScreenTab.RULES,
            onClick = { onTabSelected(ScreenTab.RULES) },
            icon = {
                Icon(
                    imageVector = if (currentTab == ScreenTab.RULES) Icons.Filled.Rule else Icons.Outlined.Rule,
                    contentDescription = "Rules"
                )
            },
            label = { Text("Rules", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_rules")
        )

        NavigationBarItem(
            selected = currentTab == ScreenTab.SIMULATOR,
            onClick = { onTabSelected(ScreenTab.SIMULATOR) },
            icon = {
                Icon(
                    imageVector = if (currentTab == ScreenTab.SIMULATOR) Icons.Filled.PlayArrow else Icons.Filled.PlayArrow,
                    contentDescription = "Test Simulator"
                )
            },
            label = { Text("Simulator", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_simulator")
        )

        NavigationBarItem(
            selected = currentTab == ScreenTab.HISTORY,
            onClick = { onTabSelected(ScreenTab.HISTORY) },
            icon = {
                Icon(
                    imageVector = if (currentTab == ScreenTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = "Reply Logs"
                )
            },
            label = { Text("Logs", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_history")
        )

        NavigationBarItem(
            selected = currentTab == ScreenTab.SETTINGS,
            onClick = { onTabSelected(ScreenTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == ScreenTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}
