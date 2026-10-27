package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityLog
import com.example.data.AppSettings
import com.example.data.LogLevel
import com.example.data.ServerInfo
import com.example.data.ServerStatus
import com.example.data.WifiConnectionState
import com.example.ui.FtpViewModel
import com.example.ui.theme.NaturalAmber
import com.example.ui.theme.NaturalBg
import com.example.ui.theme.NaturalBorder
import com.example.ui.theme.NaturalError
import com.example.ui.theme.NaturalGreenDark
import com.example.ui.theme.NaturalGreenLight
import com.example.ui.theme.NaturalGreenPrimary
import com.example.ui.theme.NaturalSurface
import com.example.ui.theme.NaturalSurfaceLight
import com.example.ui.theme.NaturalSurfaceVariant
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.ui.theme.NaturalTextTertiary
import com.example.ui.theme.NaturalToggleInactive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FtpViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val serverInfo by viewModel.serverInfo.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val wifiState by viewModel.wifiState.collectAsState()
    val logs by viewModel.activityLogs.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddNetworkDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    // Launcher for Location Permission (needed to read Wi-Fi SSID on Android 8.1+)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshState()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NaturalBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AirDrive FTP",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = NaturalTextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = "Automated Wireless Transfer",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NaturalTextSecondary
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showGuideDialog = true },
                        modifier = Modifier.testTag("help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Connection Guide",
                            tint = NaturalGreenPrimary
                        )
                    }
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NaturalSurfaceVariant)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Server Settings",
                            tint = NaturalTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NaturalBg
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Navigation Tabs (Natural Tones pill styling)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NaturalSurface,
                contentColor = NaturalGreenPrimary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NaturalGreenPrimary,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, NaturalBorder, RoundedCornerShape(20.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Server & Auto-Start",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) NaturalGreenPrimary else NaturalTextSecondary
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (selectedTab == 0) NaturalGreenPrimary else NaturalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_server")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Activity Log (${logs.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) NaturalGreenPrimary else NaturalTextSecondary
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = if (selectedTab == 1) NaturalGreenPrimary else NaturalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_logs")
                )
            }

            // Storage Permission Warning Banner
            if (!hasStoragePermission) {
                StoragePermissionBanner(
                    onGrantClick = {
                        requestStoragePermission(context)
                    }
                )
            }

            // Location Permission Warning Banner (if SSID is unavailable)
            if (wifiState.isConnected && !hasLocationPermission && wifiState.ssid == null) {
                LocationPermissionBanner(
                    onGrantClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    ServerControlTab(
                        serverInfo = serverInfo,
                        settings = settings,
                        wifiState = wifiState,
                        hasStoragePermission = hasStoragePermission,
                        onToggleServer = { viewModel.toggleServer() },
                        onSetAutoStart = { viewModel.setAutoStartOnWifi(it) },
                        onSetOnlyTrusted = { viewModel.setOnlyTrustedNetworks(it) },
                        onAddCurrentNetwork = { viewModel.addCurrentNetworkToTrusted() },
                        onAddNetworkClick = { showAddNetworkDialog = true },
                        onRemoveTrustedNetwork = { viewModel.removeTrustedNetwork(it) },
                        onOpenConfig = { showConfigDialog = true },
                        onCopyUrl = { copyToClipboard(context, serverInfo.ftpUrl ?: "") },
                        onShareUrl = { shareUrl(context, serverInfo.ftpUrl ?: "") }
                    )
                }
                1 -> {
                    ActivityLogsTab(
                        logs = logs,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
            }
        }
    }

    // Dialog: Add Trusted Network
    if (showAddNetworkDialog) {
        AddTrustedNetworkDialog(
            currentSsid = wifiState.ssid,
            onDismiss = { showAddNetworkDialog = false },
            onAdd = { ssid ->
                viewModel.addTrustedNetwork(ssid)
                showAddNetworkDialog = false
            }
        )
    }

    // Dialog: Server Advanced Configuration
    if (showConfigDialog) {
        ServerConfigDialog(
            settings = settings,
            isRunning = serverInfo.status == ServerStatus.RUNNING,
            onDismiss = { showConfigDialog = false },
            onSave = { port, allowAnon, allowWrite, user, pass, wakeLock, mdns ->
                viewModel.setPort(port)
                viewModel.setAllowAnonymous(allowAnon)
                viewModel.setAllowWrite(allowWrite)
                viewModel.setCredentials(user, pass)
                viewModel.setEnableWakeLock(wakeLock)
                viewModel.setAdvertiseMdns(mdns)
                showConfigDialog = false
            }
        )
    }

    // Dialog: Connection Guide
    if (showGuideDialog) {
        ConnectionGuideDialog(
            ftpUrl = serverInfo.ftpUrl ?: "ftp://${wifiState.ipAddress ?: "192.168.X.X"}:${settings.port}",
            onDismiss = { showGuideDialog = false }
        )
    }
}

@Composable
private fun StoragePermissionBanner(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("storage_permission_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalAmber, NaturalAmber)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = NaturalAmber,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage Access Required",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF78350F),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "All Files Access is required to share /storage/emulated/0 over FTP.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = NaturalAmber, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("grant_storage_button")
            ) {
                Text("Grant", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocationPermissionBanner(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalSurfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NetworkCheck,
                contentDescription = null,
                tint = NaturalGreenPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wi-Fi Name Detection",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = NaturalTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "Grant location permission to identify your trusted Wi-Fi SSID.",
                    style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary)
                )
            }
            TextButton(onClick = onGrantClick) {
                Text("Enable", color = NaturalGreenPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServerControlTab(
    serverInfo: ServerInfo,
    settings: AppSettings,
    wifiState: WifiConnectionState,
    hasStoragePermission: Boolean,
    onToggleServer: () -> Unit,
    onSetAutoStart: (Boolean) -> Unit,
    onSetOnlyTrusted: (Boolean) -> Unit,
    onAddCurrentNetwork: () -> Unit,
    onAddNetworkClick: () -> Unit,
    onRemoveTrustedNetwork: (String) -> Unit,
    onOpenConfig: () -> Unit,
    onCopyUrl: () -> Unit,
    onShareUrl: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // Hero Server Status Card (Natural Tones Sage Container)
        item {
            ServerHeroCard(
                serverInfo = serverInfo,
                wifiState = wifiState,
                settings = settings,
                onCopyUrl = onCopyUrl,
                onShareUrl = onShareUrl
            )
        }

        // Manual Start / Stop Button
        item {
            ManualServerButton(
                serverInfo = serverInfo,
                wifiState = wifiState,
                hasStoragePermission = hasStoragePermission,
                onToggleServer = onToggleServer
            )
        }

        // Wi-Fi Status & Detection Card
        item {
            WifiStatusCard(
                wifiState = wifiState,
                settings = settings,
                onAddCurrentToTrusted = onAddCurrentNetwork
            )
        }

        // Auto-Start Rules Section
        item {
            AutoStartConfigCard(
                settings = settings,
                wifiState = wifiState,
                hasStoragePermission = hasStoragePermission,
                onSetAutoStart = onSetAutoStart,
                onSetOnlyTrusted = onSetOnlyTrusted,
                onAddNetworkClick = onAddNetworkClick,
                onRemoveTrustedNetwork = onRemoveTrustedNetwork
            )
        }

        // Quick Server Overview Card
        item {
            ServerDetailsCard(
                serverInfo = serverInfo,
                settings = settings,
                onOpenConfig = onOpenConfig
            )
        }
    }
}

@Composable
private fun ServerHeroCard(
    serverInfo: ServerInfo,
    wifiState: WifiConnectionState,
    settings: AppSettings,
    onCopyUrl: () -> Unit,
    onShareUrl: () -> Unit
) {
    val isRunning = serverInfo.status == ServerStatus.RUNNING
    val isStarting = serverInfo.status == ServerStatus.STARTING

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server_hero_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header Row: Status Label & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SERVER STATUS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NaturalTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isRunning -> NaturalGreenPrimary
                        isStarting -> NaturalAmber
                        serverInfo.status == ServerStatus.ERROR -> NaturalError
                        else -> NaturalTextSecondary
                    }
                ) {
                    Text(
                        text = when {
                            isRunning -> "ACTIVE"
                            isStarting -> "STARTING"
                            serverInfo.status == ServerStatus.ERROR -> "ERROR"
                            else -> "INACTIVE"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // IP & Port Large Display
            if (isRunning && serverInfo.ftpUrl != null) {
                val ipDisplay = serverInfo.ipAddress ?: wifiState.ipAddress ?: "127.0.0.1"
                Text(
                    text = ipDisplay,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextPrimary
                    )
                )
                Text(
                    text = "Port: ${serverInfo.port}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NaturalTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Full FTP URL Box with Copy/Share
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onCopyUrl() }
                        .testTag("ftp_url_display"),
                    color = NaturalBg.copy(alpha = 0.7f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONNECT VIA FTP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NaturalTextTertiary,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = serverInfo.ftpUrl!!,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = NaturalGreenPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Row {
                            IconButton(onClick = onCopyUrl, modifier = Modifier.size(36.dp).testTag("copy_url_button")) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy URL",
                                    tint = NaturalGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = onShareUrl, modifier = Modifier.size(36.dp).testTag("share_url_button")) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share URL",
                                    tint = NaturalTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = wifiState.ipAddress ?: "0.0.0.0",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextSecondary
                    )
                )
                Text(
                    text = "Port: ${settings.port}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NaturalTextTertiary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Connected Wi-Fi Pill (Natural Tones aesthetic)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NaturalBg.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (wifiState.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (wifiState.isConnected) NaturalGreenPrimary else NaturalError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (wifiState.isConnected) {
                            "Connected: "
                        } else {
                            "Status: "
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary)
                    )
                    Text(
                        text = wifiState.ssid ?: if (wifiState.isConnected) "Wi-Fi Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NaturalTextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualServerButton(
    serverInfo: ServerInfo,
    wifiState: WifiConnectionState,
    hasStoragePermission: Boolean,
    onToggleServer: () -> Unit
) {
    val isRunning = serverInfo.status == ServerStatus.RUNNING
    val isStarting = serverInfo.status == ServerStatus.STARTING

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onToggleServer,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("manual_start_stop_button"),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) NaturalTextPrimary else NaturalGreenPrimary,
                contentColor = NaturalBg
            ),
            enabled = !isStarting && (isRunning || hasStoragePermission)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = NaturalBg,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Starting FTP Engine...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                } else {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Stop Server" else "Start Server",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "mDNS: airdrive._tcp.local",
            style = MaterialTheme.typography.labelSmall.copy(
                color = NaturalTextSecondary,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun WifiStatusCard(
    wifiState: WifiConnectionState,
    settings: AppSettings,
    onAddCurrentToTrusted: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wifi_status_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
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
                            .background(NaturalSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (wifiState.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (wifiState.isConnected) NaturalGreenPrimary else NaturalError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Current Wi-Fi Network",
                            style = MaterialTheme.typography.labelSmall.copy(color = NaturalTextSecondary)
                        )
                        Text(
                            text = wifiState.ssid ?: if (wifiState.isConnected) "Connected (Wi-Fi)" else "Disconnected",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NaturalTextPrimary
                            )
                        )
                    }
                }

                // Trusted status badge
                if (wifiState.isConnected && wifiState.ssid != null) {
                    if (wifiState.isTrusted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NaturalSurfaceVariant,
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NaturalGreenPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trusted", style = MaterialTheme.typography.labelSmall.copy(color = NaturalGreenPrimary, fontWeight = FontWeight.Bold))
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onAddCurrentToTrusted,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("add_current_wifi_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NaturalGreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trust Network", color = NaturalGreenPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutoStartConfigCard(
    settings: AppSettings,
    wifiState: WifiConnectionState,
    hasStoragePermission: Boolean,
    onSetAutoStart: (Boolean) -> Unit,
    onSetOnlyTrusted: (Boolean) -> Unit,
    onAddNetworkClick: () -> Unit,
    onRemoveTrustedNetwork: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auto_start_settings_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Auto-start on Wi-Fi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSetAutoStart(!settings.autoStartOnWifi) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-start on Wi-Fi",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = NaturalTextPrimary
                        )
                    )
                    Text(
                        text = "Starts service when connected",
                        style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary)
                    )
                }
                Switch(
                    checked = settings.autoStartOnWifi,
                    onCheckedChange = onSetAutoStart,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NaturalGreenPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = NaturalToggleInactive
                    ),
                    modifier = Modifier.testTag("auto_start_toggle")
                )
            }

            HorizontalDivider(color = NaturalBorder, modifier = Modifier.padding(horizontal = 8.dp))

            // Row 2: Trusted Networks Config
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = settings.autoStartOnWifi) {
                        onSetOnlyTrusted(!settings.onlyTrustedNetworks)
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Only on Trusted Networks",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (settings.autoStartOnWifi) NaturalTextPrimary else NaturalTextTertiary
                        )
                    )
                    Text(
                        text = "${settings.trustedNetworks.size} trusted network${if (settings.trustedNetworks.size == 1) "" else "s"} configured",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (settings.autoStartOnWifi) NaturalTextSecondary else NaturalTextTertiary
                        )
                    )
                }
                Switch(
                    checked = settings.onlyTrustedNetworks,
                    onCheckedChange = onSetOnlyTrusted,
                    enabled = settings.autoStartOnWifi,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NaturalGreenPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = NaturalToggleInactive
                    ),
                    modifier = Modifier.testTag("trusted_networks_toggle")
                )
            }

            // Trusted Networks SSIDs pill area
            AnimatedVisibility(
                visible = settings.autoStartOnWifi && settings.onlyTrustedNetworks,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Configured Networks",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = NaturalGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        TextButton(
                            onClick = onAddNetworkClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("add_network_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NaturalGreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add SSID", color = NaturalGreenPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (settings.trustedNetworks.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = NaturalSurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "No trusted networks yet. Add your home/office Wi-Fi SSID.",
                                style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary),
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            settings.trustedNetworks.forEach { ssid ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NaturalSurfaceVariant,
                                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = NaturalGreenPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = ssid,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = NaturalTextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        IconButton(
                                            onClick = { onRemoveTrustedNetwork(ssid) },
                                            modifier = Modifier.size(28.dp).testTag("remove_network_$ssid")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove $ssid",
                                                tint = NaturalTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = NaturalBorder, modifier = Modifier.padding(horizontal = 8.dp))

            // Row 3: Storage Access Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Storage Access",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (hasStoragePermission) NaturalGreenPrimary else NaturalError
                        )
                    )
                    Text(
                        text = if (hasStoragePermission) "Permissions granted" else "All Files permission required",
                        style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary)
                    )
                }
                Icon(
                    imageVector = if (hasStoragePermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasStoragePermission) NaturalGreenPrimary else NaturalAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(color = NaturalBorder, modifier = Modifier.padding(horizontal = 8.dp))

            // Row 4: Anonymous Access
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Anonymous Access",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = NaturalTextPrimary
                        )
                    )
                    Text(
                        text = if (settings.allowAnonymous) "Read/Write enabled" else "Password required",
                        style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NaturalSurfaceVariant,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
                ) {
                    Text(
                        text = if (settings.allowAnonymous) "Open" else "Protected",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NaturalTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerDetailsCard(
    serverInfo: ServerInfo,
    settings: AppSettings,
    onOpenConfig: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server_details_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Server Configuration",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextPrimary
                    )
                )
                IconButton(onClick = onOpenConfig, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Edit Configuration",
                        tint = NaturalGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            DetailRow(label = "Port", value = "${settings.port}")
            DetailRow(label = "Root Path", value = settings.rootPath)
            DetailRow(
                label = "Permissions",
                value = if (settings.allowWrite) "Read / Write" else "Read Only"
            )
            DetailRow(
                label = "mDNS Broadcast",
                value = if (settings.advertiseMdns) "_airdrive._tcp (${settings.port})" else "Disabled"
            )
            DetailRow(
                label = "Background WakeLock",
                value = if (settings.enableWakeLock) "Active (High Performance)" else "Off"
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = NaturalTextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun ActivityLogsTab(
    logs: List<ActivityLog>,
    onClearLogs: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Live Activity & Connection Log",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NaturalTextPrimary
                )
            )
            TextButton(onClick = onClearLogs, modifier = Modifier.testTag("clear_logs_button")) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = NaturalTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear", color = NaturalTextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = NaturalTextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No activity recorded yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = NaturalTextSecondary)
                    )
                    Text(
                        text = "Logs will appear when Wi-Fi state changes or clients connect.",
                        style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextTertiary),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NaturalSurface)
                    .border(1.dp, NaturalBorder, RoundedCornerShape(20.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs, key = { it.id }) { logItem ->
                    LogItemRow(logItem)
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(log: ActivityLog) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = timeFormat.format(Date(log.timestamp))

    val color = when (log.level) {
        LogLevel.INFO -> NaturalTextSecondary
        LogLevel.SUCCESS -> NaturalGreenPrimary
        LogLevel.WARNING -> NaturalAmber
        LogLevel.ERROR -> NaturalError
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall.copy(
                color = NaturalTextTertiary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (log.tag != null) {
            Surface(
                color = NaturalSurfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = log.tag,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NaturalGreenPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = log.message,
            style = MaterialTheme.typography.bodySmall.copy(
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AddTrustedNetworkDialog(
    currentSsid: String?,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var ssidInput by remember { mutableStateOf(currentSsid ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NaturalSurface,
        title = {
            Text("Add Trusted Wi-Fi Network", color = NaturalTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "FTP Server will only auto-start when connected to trusted networks.",
                    style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ssidInput,
                    onValueChange = { ssidInput = it },
                    label = { Text("Wi-Fi SSID (Network Name)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NaturalGreenPrimary,
                        unfocusedBorderColor = NaturalBorder,
                        focusedTextColor = NaturalTextPrimary,
                        unfocusedTextColor = NaturalTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("ssid_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(ssidInput.trim()) },
                enabled = ssidInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NaturalGreenPrimary, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_add_network_button")
            ) {
                Text("Add Network", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NaturalTextSecondary)
            }
        }
    )
}

@Composable
private fun ServerConfigDialog(
    settings: AppSettings,
    isRunning: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, Boolean, Boolean, String, String, Boolean, Boolean) -> Unit
) {
    var portInput by remember { mutableStateOf(settings.port.toString()) }
    var allowAnon by remember { mutableStateOf(settings.allowAnonymous) }
    var allowWrite by remember { mutableStateOf(settings.allowWrite) }
    var username by remember { mutableStateOf(settings.username) }
    var password by remember { mutableStateOf(settings.password) }
    var wakeLock by remember { mutableStateOf(settings.enableWakeLock) }
    var mdns by remember { mutableStateOf(settings.advertiseMdns) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NaturalSurface,
        title = {
            Text("FTP Server Settings", color = NaturalTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    if (isRunning) {
                        Surface(
                            color = NaturalSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Note: Changes will apply next time the server starts.",
                                style = MaterialTheme.typography.bodySmall.copy(color = NaturalGreenDark),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                item {
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it.filter { char -> char.isDigit() } },
                        label = { Text("FTP Port (Default: 2121)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalGreenPrimary,
                            unfocusedBorderColor = NaturalBorder,
                            focusedTextColor = NaturalTextPrimary,
                            unfocusedTextColor = NaturalTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("port_input_field")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Anonymous Login", color = NaturalTextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("Allow login without password", color = NaturalTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = allowAnon,
                            onCheckedChange = { allowAnon = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NaturalGreenPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = NaturalToggleInactive
                            )
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Read & Write Access", color = NaturalTextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("Allow clients to upload and delete files", color = NaturalTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = allowWrite,
                            onCheckedChange = { allowWrite = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NaturalGreenPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = NaturalToggleInactive
                            )
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("mDNS Service Discovery", color = NaturalTextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("Advertise _airdrive._tcp on LAN", color = NaturalTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = mdns,
                            onCheckedChange = { mdns = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NaturalGreenPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = NaturalToggleInactive
                            )
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Keep CPU / Wi-Fi Active", color = NaturalTextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("Prevent sleep during large transfers", color = NaturalTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = wakeLock,
                            onCheckedChange = { wakeLock = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NaturalGreenPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = NaturalToggleInactive
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portInput.toIntOrNull() ?: 2121
                    onSave(port, allowAnon, allowWrite, username, password, wakeLock, mdns)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NaturalGreenPrimary, contentColor = Color.White),
                modifier = Modifier.testTag("save_config_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NaturalTextSecondary)
            }
        }
    )
}

@Composable
private fun ConnectionGuideDialog(
    ftpUrl: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NaturalSurface,
        title = {
            Text("How to Connect via FTP", color = NaturalTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    GuideItem(
                        platform = "Windows File Explorer",
                        step = "1. Open 'This PC' or any folder\n2. In address bar at top, type:\n   $ftpUrl\n3. Press Enter to view files!"
                    )
                }
                item {
                    GuideItem(
                        platform = "macOS Finder",
                        step = "1. Open Finder\n2. Press Cmd + K (or Go > Connect to Server)\n3. Enter Server Address:\n   $ftpUrl\n4. Click 'Connect' as Guest."
                    )
                }
                item {
                    GuideItem(
                        platform = "FileZilla / Cyberduck",
                        step = "Host: ${ftpUrl.removePrefix("ftp://").substringBefore(":")}\nPort: ${ftpUrl.substringAfterLast(":")}\nLogon Type: Anonymous"
                    )
                }
                item {
                    GuideItem(
                        platform = "AirDrive Discovery (mDNS)",
                        step = "AirDrive automatically advertises on local Wi-Fi via mDNS as '_airdrive._tcp.'. Compatible clients can auto-detect this device!"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NaturalGreenPrimary, contentColor = Color.White)
            ) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun GuideItem(platform: String, step: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NaturalBorder, NaturalBorder)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = platform,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = NaturalGreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = step,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NaturalTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

private fun requestStoragePermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            context.startActivity(intent)
        }
    } else {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("FTP URL", text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
}

private fun shareUrl(context: Context, url: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Connect to my device via FTP: $url")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share FTP Server URL")
    context.startActivity(shareIntent)
}
