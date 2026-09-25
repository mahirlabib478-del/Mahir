package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.AutoReplyForegroundService
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppTopBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RuleDialog
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.theme.WAAutoReplyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            if (AutoReplyApp.instance.preferenceManager.isMasterEnabled.value) {
                AutoReplyForegroundService.startService(this)
            }
        } catch (_: Exception) {}

        setContent {
            WAAutoReplyTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val isMasterEnabled by viewModel.isMasterEnabled.collectAsState()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()

    val totalRepliesCount by viewModel.totalRepliesCount.collectAsState()
    val enabledRulesCount by viewModel.enabledRulesCount.collectAsState()
    val allRules by viewModel.allRules.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()

    val simChatHistory by viewModel.simChatHistory.collectAsState()
    val isSimulatingReply by viewModel.isSimulatingReply.collectAsState()

    val showRuleDialog by viewModel.showRuleDialog.collectAsState()
    val editingRule by viewModel.editingRule.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Request notification permission for Foreground Service on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted && isMasterEnabled) {
                AutoReplyForegroundService.startService(context)
            }
        }
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Re-check notification listener permission when user returns to app from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show snackbar events
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Back handling: return to Dashboard if on any other tab
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        viewModel.setTab(ScreenTab.DASHBOARD)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                isMasterEnabled = isMasterEnabled,
                onTabSelect = { viewModel.setTab(it) }
            )
        },
        bottomBar = {
            AppBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_transition",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                ScreenTab.DASHBOARD -> DashboardScreen(
                    isMasterEnabled = isMasterEnabled,
                    isPermissionGranted = isPermissionGranted,
                    totalRepliesCount = totalRepliesCount,
                    enabledRulesCount = enabledRulesCount,
                    recentLogs = allLogs,
                    onToggleMaster = { viewModel.toggleMasterSwitch() },
                    onNavigateTab = { viewModel.setTab(it) },
                    onResetPresets = { viewModel.resetToStarterPresets() },
                    onCheckPermission = { viewModel.checkPermission() },
                    onReconnectService = { viewModel.reconnectService() }
                )
                ScreenTab.RULES -> RulesScreen(
                    rules = allRules,
                    onToggleRule = { viewModel.toggleRule(it) },
                    onEditRule = { viewModel.openEditRuleDialog(it) },
                    onDeleteRule = { viewModel.deleteRule(it) },
                    onAddNewRule = { viewModel.openAddRuleDialog() },
                    onNavigateToSimulator = { viewModel.setTab(ScreenTab.SIMULATOR) },
                    onResetPresets = { viewModel.resetToStarterPresets() }
                )
                ScreenTab.SIMULATOR -> SimulatorScreen(
                    chatHistory = simChatHistory,
                    isSimulating = isSimulatingReply,
                    onSendMessage = { sender, text, isGroup, platform ->
                        viewModel.simulateIncomingMessage(sender, text, isGroup, platform)
                    },
                    onClearChat = { viewModel.clearSimChat() }
                )
                ScreenTab.HISTORY -> HistoryScreen(
                    logs = allLogs,
                    onClearLogs = { viewModel.clearAllLogs() }
                )
                ScreenTab.SETTINGS -> SettingsScreen(
                    preferenceManager = viewModel.preferenceManager,
                    isPermissionGranted = isPermissionGranted,
                    onCheckPermission = { viewModel.checkPermission() }
                )
            }
        }
    }

    // Modal dialog to Add / Edit Rule
    if (showRuleDialog) {
        RuleDialog(
            initialRule = editingRule,
            onDismiss = { viewModel.closeRuleDialog() },
            onSave = { name, pattern, matchType, replyText, isGroupAllowed, cooldown, priority ->
                viewModel.saveRule(
                    name = name,
                    pattern = pattern,
                    matchType = matchType,
                    replyText = replyText,
                    isGroupAllowed = isGroupAllowed,
                    cooldownSeconds = cooldown,
                    priority = priority
                )
            }
        )
    }
}
