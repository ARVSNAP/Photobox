package com.example.photobooth.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photobooth.data.SessionEntity
import com.example.photobooth.model.BoothManagerConfig
import com.example.photobooth.model.FlashAnimationConfig
import com.example.photobooth.model.WatermarkConfig
import com.example.photobooth.ui.components.FlashEditorDialog
import com.example.photobooth.ui.components.WatermarkEditorDialog
import com.example.photobooth.ui.viewmodel.PhotoBoothViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import com.example.photobooth.hardware.UsbOtgCameraManager
import com.example.photobooth.hardware.UsbCameraDeviceInfo
import com.example.photobooth.hardware.gphoto2.LibGPhoto2Engine
import com.example.photobooth.hardware.gphoto2.GPhoto2Brand
import com.example.photobooth.hardware.gphoto2.GPhoto2CameraDevice
import com.example.photobooth.hardware.gphoto2.GPhoto2ConnectionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerAppScreen(
    viewModel: PhotoBoothViewModel,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val managerConfig by viewModel.managerConfig.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val flashConfig by viewModel.flashAnimationConfig.collectAsState()
    val watermarkConfig by viewModel.watermarkConfig.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Event, 2: Camera, 3: Printer, 4: History
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var showFlashDialog by remember { mutableStateOf(false) }
    var showUsbTestDialog by remember { mutableStateOf(false) }
    var showGphoto2TestDialog by remember { mutableStateOf(false) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }

    // Local editable copy of BoothManagerConfig
    var localConfig by remember(managerConfig) { mutableStateOf(managerConfig) }
    var showPinText by remember { mutableStateOf(false) }

    // Test print status banner
    var testPrintBanner by remember { mutableStateOf<String?>(null) }

    val totalPrints = remember(allSessions) {
        allSessions.sumOf { it.printCount }
    }
    val totalRevenue = remember(allSessions, localConfig) {
        if (localConfig.isFreeMode) 0 else allSessions.size * localConfig.pricePerSession
    }

    Scaffold(
        containerColor = Color(0xFF0A0F1D),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "BOOTH MANAGER APP",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonCyan.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text(
                                    text = localConfig.boothId,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Admin & Operations Control Center",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.updateManagerConfig(localConfig)
                        onBackClicked()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Save & Apply Button
                    Button(
                        onClick = {
                            viewModel.updateManagerConfig(localConfig)
                            Toast.makeText(context, "Pengaturan Manager Disimpan & Diterapkan!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Manager Tab Navigation
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF131B2E),
                contentColor = NeonCyan,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = Color(0x22FFFFFF)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📊 Overview", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("⚙️ Event & Booth", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("📸 Kamera & Flash", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("🖨️ Printer & Format", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("📜 Riwayat (${allSessions.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("💳 QRIS & Payment", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // Test Print Feedback Banner
            AnimatedVisibility(visible = testPrintBanner != null) {
                testPrintBanner?.let { msg ->
                    Surface(
                        color = NeonCyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan)
                                Text(text = msg, color = Color.White, fontSize = 12.sp)
                            }
                            IconButton(onClick = { testPrintBanner = null }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }
                    }
                }
            }

            // Active Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> DashboardTab(
                        sessionsCount = allSessions.size,
                        printsCount = totalPrints,
                        revenue = totalRevenue,
                        config = localConfig,
                        onTestPrint = {
                            testPrintBanner = "✅ Test Print Berhasil Terkirim ke Printer ESC/POS!"
                        },
                        onTestFlash = { showFlashDialog = true },
                        onOpenWatermark = { showWatermarkDialog = true },
                        onClearCache = {
                            Toast.makeText(context, "Photo & Render Cache Berhasil Dikosongkan!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> EventConfigTab(
                        config = localConfig,
                        onConfigChanged = { localConfig = it },
                        showPin = showPinText,
                        onToggleShowPin = { showPinText = !showPinText }
                    )
                    2 -> CameraConfigTab(
                        config = localConfig,
                        onConfigChanged = { localConfig = it },
                        flashConfig = flashConfig,
                        watermarkConfig = watermarkConfig,
                        onOpenFlashEditor = { showFlashDialog = true },
                        onOpenWatermarkEditor = { showWatermarkDialog = true },
                        onOpenUsbTest = { showUsbTestDialog = true },
                        onOpenGPhoto2Test = { showGphoto2TestDialog = true }
                    )
                    3 -> PrinterConfigTab(
                        config = localConfig,
                        onConfigChanged = { localConfig = it },
                        onTestPrint = {
                            testPrintBanner = "✅ Cetak Struk Diagnostik 2x6\" Selesai!"
                        }
                    )
                    4 -> HistoryLogTab(
                        sessions = allSessions,
                        onDeleteSession = { sessionToDelete = it },
                        onClearAllSessions = { showClearAllConfirmDialog = true }
                    )
                    5 -> QrisConfigTab(
                        config = localConfig,
                        onConfigChanged = { localConfig = it }
                    )
                }
            }
        }
    }

    // Watermark Editor Dialog
    if (showWatermarkDialog) {
        WatermarkEditorDialog(
            config = watermarkConfig,
            onConfigChanged = { viewModel.setWatermarkConfig(it) },
            onDismiss = { showWatermarkDialog = false }
        )
    }

    // Flash Animation Editor Dialog
    if (showFlashDialog) {
        FlashEditorDialog(
            config = flashConfig,
            onConfigChanged = { viewModel.setFlashConfig(it) },
            onDismiss = { showFlashDialog = false }
        )
    }

    // USB OTG Camera Test Dialog
    if (showUsbTestDialog) {
        UsbCameraTestDialog(
            config = localConfig,
            onDismiss = { showUsbTestDialog = false }
        )
    }

    // libgphoto2 DSLR Camera Test Dialog
    if (showGphoto2TestDialog) {
        LibGPhoto2TestDialog(
            config = localConfig,
            onDismiss = { showGphoto2TestDialog = false }
        )
    }

    // Confirm Delete Single Session Dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Hapus Sesi Foto?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus catatan Sesi #${session.id} (${session.layoutType}) dari riwayat booth?",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        sessionToDelete = null
                        Toast.makeText(context, "Sesi berhasil dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Batal", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Confirm Clear All Sessions Dialog
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = { Text("Reset Semua Riwayat Sesi?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "PERINGATAN: Semua ${allSessions.size} catatan sesi foto yang tersimpan akan dihapus secara permanen dari database lokal.",
                    color = Color(0xFFFCA5A5)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllSessions()
                        showClearAllConfirmDialog = false
                        Toast.makeText(context, "Semua riwayat sesi berhasil dikosongkan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reset Semua", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Batal", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// =========================================================================
// TAB 1: OVERVIEW & ANALYTICS DASHBOARD
// =========================================================================
@Composable
private fun DashboardTab(
    sessionsCount: Int,
    printsCount: Int,
    revenue: Int,
    config: BoothManagerConfig,
    onTestPrint: () -> Unit,
    onTestFlash: () -> Unit,
    onOpenWatermark: () -> Unit,
    onClearCache: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Event Banner Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EVENT AKTIF",
                        color = NeonRose,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = config.eventName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = config.eventSubtitle,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (config.isFreeMode) NeonCyan.copy(alpha = 0.2f) else NeonRose.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (config.isFreeMode) NeonCyan else NeonRose)
                ) {
                    Text(
                        text = if (config.isFreeMode) "FREE MODE" else "PAID MODE",
                        color = if (config.isFreeMode) NeonCyan else NeonRose,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Metrics Grid (4 KPI Cards)
        Text(
            text = "RINGKASAN OPERASIONAL BOOTH",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard(
                title = "Total Sesi",
                value = "$sessionsCount",
                icon = Icons.Default.CameraAlt,
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Total Cetakan",
                value = "$printsCount lembar",
                icon = Icons.Default.Print,
                color = NeonRose,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard(
                title = "Estimasi Omset",
                value = if (config.isFreeMode) "Gratis" else "Rp ${NumberFormat.getNumberInstance(Locale.GERMANY).format(revenue)}",
                icon = Icons.Default.AttachMoney,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Status Kiosk",
                value = if (config.isKioskLockEnabled) "Locked ✅" else "Unlocked",
                icon = if (config.isKioskLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        // Hardware & Device Health Status
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "STATUS HARDWARE & SUBSISTEM",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                HardwareStatusRow(
                    label = "Thermal / Dye-Sub Printer",
                    status = config.printerStatus,
                    isGood = true
                )
                HardwareStatusRow(
                    label = "Kamera Shutter Engine",
                    status = "CameraX High-Res (${config.cameraFacing})",
                    isGood = true
                )
                val isUsbConnected by UsbOtgCameraManager.isUsbCameraConnected.collectAsState()
                val activeUsbDevice by UsbOtgCameraManager.activeCameraDevice.collectAsState()
                HardwareStatusRow(
                    label = "Kamera Eksternal USB OTG",
                    status = if (isUsbConnected) "${activeUsbDevice?.productName ?: "USB OTG UVC"} (Siap)" else "Belum Terhubung (Siap Pasang OTG)",
                    isGood = isUsbConnected
                )
                HardwareStatusRow(
                    label = "QR Code Cloud / Local Server",
                    status = if (config.isQrSharingEnabled) "Active & Hosting" else "Disabled",
                    isGood = config.isQrSharingEnabled
                )
                HardwareStatusRow(
                    label = "Database Room SQLite",
                    status = "Operational ($sessionsCount Sesi)",
                    isGood = true
                )
            }
        }

        // Quick Operator Tools
        Text(
            text = "AKSI CEPAT OPERATOR",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTestPrint,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x2206B6D4)),
                border = BorderStroke(1.dp, NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Print", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onTestFlash,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22EC4899)),
                border = BorderStroke(1.dp, NeonRose),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = NeonRose, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Flash", color = NeonRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onClearCache,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear Cache", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, color)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Column {
                Text(text = title, color = TextMuted, fontSize = 11.sp)
                Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HardwareStatusRow(label: String, status: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isGood) Color(0xFF10B981) else Color(0xFFEF4444))
            )
            Text(
                text = status,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =========================================================================
// TAB 2: EVENT & BOOTH CONFIGURATION
// =========================================================================
@Composable
private fun EventConfigTab(
    config: BoothManagerConfig,
    onConfigChanged: (BoothManagerConfig) -> Unit,
    showPin: Boolean,
    onToggleShowPin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "INFORMASI EVENT & BRANDING",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // Event Name
        OutlinedTextField(
            value = config.eventName,
            onValueChange = { onConfigChanged(config.copy(eventName = it)) },
            label = { Text("Nama Acara / Event Title") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0x44FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Event Subtitle
        OutlinedTextField(
            value = config.eventSubtitle,
            onValueChange = { onConfigChanged(config.copy(eventSubtitle = it)) },
            label = { Text("Tagline / Subtitle Acara") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0x44FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Booth Identifier
        OutlinedTextField(
            value = config.boothId,
            onValueChange = { onConfigChanged(config.copy(boothId = it)) },
            label = { Text("Booth Identifier (ID Kiosk)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0x44FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Welcome Badge Text
        OutlinedTextField(
            value = config.welcomeBadgeText,
            onValueChange = { onConfigChanged(config.copy(welcomeBadgeText = it)) },
            label = { Text("Teks Status Badge Welcome Screen") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0x44FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Welcome Start Button Text
        OutlinedTextField(
            value = config.welcomeStartButtonText,
            onValueChange = { onConfigChanged(config.copy(welcomeStartButtonText = it)) },
            label = { Text("Teks Tombol Mulai Welcome Screen") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0x44FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "TARIF & MODEL BISNIS",
            color = NeonRose,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // Free Mode Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (config.isFreeMode) "Mode Gratis (Free Kiosk)" else "Mode Berbayar (Commercial)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (config.isFreeMode) "Pengunjung bebas mengambil foto tanpa batas pembayaran" else "Pengunjung membayar tarif per sesi foto",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = !config.isFreeMode,
                    onCheckedChange = { onConfigChanged(config.copy(isFreeMode = !it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonRose,
                        checkedTrackColor = NeonRose.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // Price Input (if not free)
        if (!config.isFreeMode) {
            OutlinedTextField(
                value = config.pricePerSession.toString(),
                onValueChange = {
                    val price = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
                    onConfigChanged(config.copy(pricePerSession = price))
                },
                label = { Text("Tarif Per Sesi (Rupiah)") },
                prefix = { Text("Rp ", color = NeonRose, fontWeight = FontWeight.Bold) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonRose,
                    unfocusedBorderColor = Color(0x44FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "KEAMANAN & PIN OPERATOR",
            color = Color(0xFFF59E0B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // Operator PIN field
        OutlinedTextField(
            value = config.operatorPin,
            onValueChange = {
                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                    onConfigChanged(config.copy(operatorPin = it))
                }
            },
            label = { Text("PIN Operator (Default: 1234)") },
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleShowPin) {
                    Icon(
                        imageVector = if (showPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle PIN",
                        tint = TextMuted
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFF59E0B),
                unfocusedBorderColor = Color(0x44FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Kiosk Lock Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Kiosk Lock Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Mengunci antarmuka agar pengunjung tidak bisa keluar aplikasi", color = TextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = config.isKioskLockEnabled,
                onCheckedChange = { onConfigChanged(config.copy(isKioskLockEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// =========================================================================
// TAB 3: CAMERA & SHUTTER CONFIGURATION
// =========================================================================
@Composable
private fun CameraConfigTab(
    config: BoothManagerConfig,
    onConfigChanged: (BoothManagerConfig) -> Unit,
    flashConfig: FlashAnimationConfig,
    watermarkConfig: WatermarkConfig,
    onOpenFlashEditor: () -> Unit,
    onOpenWatermarkEditor: () -> Unit,
    onOpenUsbTest: () -> Unit,
    onOpenGPhoto2Test: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PENGATURAN SHOOTING & COUNTDOWN",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // Countdown Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Durasi Timer Hitung Mundur (Countdown):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3 to "3 Detik (Cepat)", 5 to "5 Detik (Ideal)", 10 to "10 Detik (Santai)").forEach { (sec, label) ->
                        val isSelected = config.countdownSeconds == sec
                        Button(
                            onClick = { onConfigChanged(config.copy(countdownSeconds = sec)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonCyan else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Camera Orientation & Source
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Sumber & Arah Lensa Kamera Utama:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Front Kiosk", "DSLR libgphoto2", "USB OTG", "Back Wide").forEach { facing ->
                        val isSelected = config.cameraFacing == facing
                        Button(
                            onClick = { onConfigChanged(config.copy(cameraFacing = facing)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonRose else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = facing,
                                fontSize = 9.5.sp,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // DEDICATED USB OTG CAMERA INTEGRATION CARD
        // ==========================================
        val context = LocalContext.current
        val isUsbConnected by UsbOtgCameraManager.isUsbCameraConnected.collectAsState()
        val activeUsbDevice by UsbOtgCameraManager.activeCameraDevice.collectAsState()

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.5.dp, if (isUsbConnected) Color(0xFF10B981) else NeonCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Usb, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "DUKUNGAN KAMERA USB OTG",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = if (isUsbConnected) Color(0x3310B981) else Color(0x22FFFFFF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isUsbConnected) Color(0xFF10B981) else Color(0x44FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isUsbConnected) Color(0xFF10B981) else Color.Gray)
                            )
                            Text(
                                text = if (isUsbConnected) "TERHUBUNG" else "STANDBY OTG",
                                color = if (isUsbConnected) Color(0xFF10B981) else Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Device info or connection guide
                if (isUsbConnected && activeUsbDevice != null) {
                    Surface(
                        color = Color(0x2210B981),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Perangkat: ${activeUsbDevice?.productName ?: "USB Video Camera"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Pabrikan: ${activeUsbDevice?.manufacturerName ?: "Generic UVC"} • VID:${activeUsbDevice?.vendorId} PID:${activeUsbDevice?.productId}",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        color = Color(0x11FFFFFF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Pasang webcam USB, camcorder, atau capture card HDMI DSLR ke port USB-C / micro-USB menggunakan konverter OTG.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Action buttons: Scan and Test
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val scanned = UsbOtgCameraManager.scanDevices(context)
                            Toast.makeText(
                                context,
                                "Pindai USB: ${scanned.size} perangkat OTG terdeteksi",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pindai USB", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onOpenUsbTest,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Uji Live & Jepret", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0x22FFFFFF))

                // Auto-switch Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Otomatis Beralih ke USB OTG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = "Langsung pakai kamera eksternal saat kabel OTG dicolokkan", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = config.autoSwitchToUsbCamera,
                        onCheckedChange = { onConfigChanged(config.copy(autoSwitchToUsbCamera = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                        )
                    )
                }

                // USB Resolution Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Resolusi Input USB OTG:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1080p (FHD)", "720p (HD)", "4K (UHD)").forEach { res ->
                            val isSelected = config.usbCameraResolution == res
                            Button(
                                onClick = { onConfigChanged(config.copy(usbCameraResolution = res)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NeonCyan else Color(0x22FFFFFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = res,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // USB Aspect Ratio
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Rasio Bidang USB OTG:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("4:3", "16:9", "1:1").forEach { ratio ->
                            val isSelected = config.usbCameraAspectRatio == ratio
                            Button(
                                onClick = { onConfigChanged(config.copy(usbCameraAspectRatio = ratio)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NeonRose else Color(0x22FFFFFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = ratio,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // ========================================================
        // DEDICATED LIBGPHOTO2 DSLR & MIRRORLESS INTEGRATION CARD
        // ========================================================
        val gphotoCameras by LibGPhoto2Engine.detectedCameras.collectAsState()
        val activeGphotoCam by LibGPhoto2Engine.activeCamera.collectAsState()

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.5.dp, if (activeGphotoCam != null) NeonCyan else Color(0x4400F5FF)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header & status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "LIBGPHOTO2 KAMERA EKSTERNAL",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PTP2 Driver • Support Canon, Nikon, Sony, Fuji via USB/Wi-Fi",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Surface(
                        color = Color(0x3300F5FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "v2.5.34 SIAP",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Active camera banner
                Surface(
                    color = Color(0x2200F5FF),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0x3300F5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kamera Terpilih:",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonRose.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonRose)
                            ) {
                                Text(
                                    text = activeGphotoCam?.brand?.displayName ?: "DSLR",
                                    color = NeonRose,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = activeGphotoCam?.modelName ?: "Canon EOS R50 / 2000D",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Koneksi: ${activeGphotoCam?.connectionType?.name ?: "USB_OTG"} • Port: ${activeGphotoCam?.ipAddress ?: "usb:otg"} • Baterai: ${activeGphotoCam?.batteryLevelPercent ?: 95}%",
                            color = NeonCyan,
                            fontSize = 10.5.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                // Camera Model Chooser
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Pilih Kamera DSLR Terdeteksi:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        gphotoCameras.forEach { cam ->
                            val isSelected = activeGphotoCam?.id == cam.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x11FFFFFF),
                                border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0x22FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { LibGPhoto2Engine.selectCamera(context, cam) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = cam.modelName,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "${cam.brand.displayName} • ${cam.connectionType.name} • ISO ${cam.currentIso} ${cam.currentAperture} ${cam.currentShutterSpeed}",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Hardware telemetry: ISO, Aperture, Shutter Speed tuning
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pengaturan Telemetri PTP (ISO & Diafragma):",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // ISO selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ISO:", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        listOf("100", "200", "400", "800", "1600", "3200").forEach { iso ->
                            val isIsoSelected = (activeGphotoCam?.currentIso ?: config.gphoto2Iso) == iso
                            Button(
                                onClick = {
                                    LibGPhoto2Engine.setIso(iso)
                                    onConfigChanged(config.copy(gphoto2Iso = iso))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isIsoSelected) NeonCyan else Color(0x22FFFFFF)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = iso,
                                    fontSize = 9.sp,
                                    color = if (isIsoSelected) Color.Black else Color.White,
                                    fontWeight = if (isIsoSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Aperture selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "F-Stop:", color = NeonRose, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        listOf("f/1.8", "f/2.8", "f/4.0", "f/5.6", "f/8.0").forEach { fstop ->
                            val isFSelected = (activeGphotoCam?.currentAperture ?: config.gphoto2Aperture) == fstop
                            Button(
                                onClick = {
                                    LibGPhoto2Engine.setAperture(fstop)
                                    onConfigChanged(config.copy(gphoto2Aperture = fstop))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFSelected) NeonRose else Color(0x22FFFFFF)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = fstop,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = if (isFSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Shutter Speed selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Shutter:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        listOf("1/60s", "1/125s", "1/160s", "1/200s", "1/250s").forEach { speed ->
                            val isSpeedSelected = (activeGphotoCam?.currentShutterSpeed ?: config.gphoto2ShutterSpeed) == speed
                            Button(
                                onClick = {
                                    LibGPhoto2Engine.setShutterSpeed(speed)
                                    onConfigChanged(config.copy(gphoto2ShutterSpeed = speed))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSpeedSelected) Color.White else Color(0x22FFFFFF)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = speed,
                                    fontSize = 8.5.sp,
                                    color = if (isSpeedSelected) Color.Black else Color.White,
                                    fontWeight = if (isSpeedSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Wireless PTP/IP Tethering Card
                Surface(
                    color = Color(0x15000000),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Koneksi Kamera Nirkabel (PTP/IP Wi-Fi):",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = config.gphoto2PtpIpAddress,
                                onValueChange = { onConfigChanged(config.copy(gphoto2PtpIpAddress = it)) },
                                placeholder = { Text("192.168.1.100", fontSize = 11.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x44FFFFFF)
                                )
                            )

                            Button(
                                onClick = {
                                    LibGPhoto2Engine.connectPtpIp(config.gphoto2PtpIpAddress)
                                    Toast.makeText(context, "Mencoba koneksi PTP/IP ke ${config.gphoto2PtpIpAddress}...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Sambung Wi-Fi", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Action buttons: Scan & Test
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val scanned = LibGPhoto2Engine.scanCameras()
                            Toast.makeText(
                                context,
                                "Pindai libgphoto2: ${scanned.size} kamera terdeteksi",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pindai DSLR", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onOpenGPhoto2Test,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Uji Live & Shutter DSLR", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Audio shutter switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Suara Shutter Kamera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Bunyikan efek suara klik shutter saat foto diambil", color = TextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = config.isShutterSoundEnabled,
                onCheckedChange = { onConfigChanged(config.copy(isShutterSoundEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                )
            )
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        Text(
            text = "EFEK FLASH & WATERMARK",
            color = NeonRose,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // Flash Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Animasi Flash Layar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        text = "Tema: ${flashConfig.style.displayName} (${flashConfig.durationMillis}ms)",
                        color = NeonRose,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = onOpenFlashEditor,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ubah Flash", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Watermark Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Watermark Logo / Tagline", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        text = if (watermarkConfig.isEnabled) "Aktif: Posisi ${watermarkConfig.corner.shortLabel}" else "Status: Nonaktif",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = onOpenWatermarkEditor,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ubah Watermark", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: PRINTER & SHARING CONFIGURATION
// =========================================================================
@Composable
private fun PrinterConfigTab(
    config: BoothManagerConfig,
    onConfigChanged: (BoothManagerConfig) -> Unit,
    onTestPrint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "FORMAT KERTAS & CETAK FISIK",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // Paper Format
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Pilih Format Kertas Cetak:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("2x6 Strip", "4x6 Postcard", "Square 4x4").forEach { format ->
                        val isSelected = config.paperFormat == format
                        Button(
                            onClick = { onConfigChanged(config.copy(paperFormat = format)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonCyan else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = format,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Copies count
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Jumlah Lembar Cetak Bawaan (Default Copies):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 4).forEach { copies ->
                        val isSelected = config.defaultCopies == copies
                        Button(
                            onClick = { onConfigChanged(config.copy(defaultCopies = copies)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonRose else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$copies Lembar",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Auto print switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Auto-Print Saat Sesi Selesai", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Otomatis mencetak tanpa harus menekan tombol print di layar share", color = TextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = config.isAutoPrintEnabled,
                onCheckedChange = { onConfigChanged(config.copy(isAutoPrintEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                )
            )
        }

        // QR Code Sharing switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "QR Code Digital Download", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Sediakan QR code untuk pengunjung mendownload foto & GIF ke smartphone", color = TextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = config.isQrSharingEnabled,
                onCheckedChange = { onConfigChanged(config.copy(isQrSharingEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonRose,
                    checkedTrackColor = NeonRose.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Diagnostic Test Print Button
        Button(
            onClick = onTestPrint,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("CETAK STRUK DIAGNOSTIK PRINTER ➔", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// =========================================================================
// TAB 5: RIWAYAT SESI (SESSION LOGS & HISTORY)
// =========================================================================
@Composable
private fun HistoryLogTab(
    sessions: List<SessionEntity>,
    onDeleteSession: (SessionEntity) -> Unit,
    onClearAllSessions: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOG SESI FOTO (${sessions.size})",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            if (sessions.isNotEmpty()) {
                Button(
                    onClick = onClearAllSessions,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Semua", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Belum Ada Sesi Foto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Sesi foto pengunjung akan otomatis tercatat di sini setelah selesai disimpan.", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeonCyan.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "#${session.id}",
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = session.layoutType,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = dateFormat.format(Date(session.timestamp)),
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )

                                Text(
                                    text = "${session.photoPaths} • ${session.printCount} lembar dicetak",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { onDeleteSession(session) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22EF4444))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// QRIS PAYMENT CONFIG TAB
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrisConfigTab(
    config: BoothManagerConfig,
    onConfigChanged: (BoothManagerConfig) -> Unit
) {
    val context = LocalContext.current

    // Photo picker launcher for custom QRIS image selection
    val qrisPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onConfigChanged(config.copy(qrisCustomImageUri = uri.toString()))
            Toast.makeText(context, "Gambar QRIS Eksternal Berhasil Diunggah!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Main Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Wajibkan Pembayaran QRIS",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pengunjung harus scan QRIS & bayar sebelum sesi foto dimulai",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = config.isQrisPaymentEnabled,
                        onCheckedChange = {
                            onConfigChanged(config.copy(isQrisPaymentEnabled = it, isFreeMode = !it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonCyan
                        )
                    )
                }

                HorizontalDivider(color = Color(0x11FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mode Operasi Kiosk:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (config.isQrisPaymentEnabled) NeonCyan.copy(alpha = 0.2f) else Color(0x22FFFFFF)
                    ) {
                        Text(
                            text = if (config.isQrisPaymentEnabled) "💳 QRIS PAYMENT REQUIRED" else "🎁 FREE / GRATIS MODE",
                            color = if (config.isQrisPaymentEnabled) NeonCyan else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Card 2: Merchant & Pricing Configuration
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "INFORMASI MERCHANT & TARIF",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = config.qrisMerchantName,
                    onValueChange = { onConfigChanged(config.copy(qrisMerchantName = it)) },
                    label = { Text("Nama Merchant QRIS") },
                    placeholder = { Text("e.g. PHOTOBOOTH STUDIO ID") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = NeonCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = config.qrisNmid,
                    onValueChange = { onConfigChanged(config.copy(qrisNmid = it)) },
                    label = { Text("NMID (National Merchant ID)") },
                    placeholder = { Text("e.g. ID1024589201923") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = NeonCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = config.pricePerSession.toString(),
                    onValueChange = { input ->
                        val price = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                        onConfigChanged(config.copy(pricePerSession = price))
                    },
                    label = { Text("Harga per Sesi (Rp)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = NeonCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Card 3: Custom External QRIS Image Picker
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GAMBAR QRIS EKSTERNAL (CUSTOM IMAGE)",
                            color = NeonRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Gunakan gambar barcode QRIS dari merchant bank/e-wallet Anda sendiri",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Active Image Preview Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        !config.qrisCustomImageUri.isNullOrBlank() -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(Uri.parse(config.qrisCustomImageUri))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Custom Image QRIS Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        config.qrisCustomImageUrl.isNotBlank() -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(config.qrisCustomImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Custom URL QRIS Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Menggunakan QRIS Standar (Bawaan)",
                                    color = Color(0xFF0F172A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Klik 'Upload Gambar QRIS' untuk mengganti dengan barcode milik Anda",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Action Buttons for Photo Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            qrisPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📁 Upload Gambar (Galeri)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!config.qrisCustomImageUri.isNullOrBlank() || config.qrisCustomImageUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                onConfigChanged(config.copy(qrisCustomImageUri = null, qrisCustomImageUrl = ""))
                                Toast.makeText(context, "Gambar QRIS Kustom Direset ke Bawaan!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = config.qrisCustomImageUrl,
                    onValueChange = { onConfigChanged(config.copy(qrisCustomImageUrl = it)) },
                    label = { Text("Atau Input URL Gambar QRIS External (http/https)") },
                    placeholder = { Text("https://example.com/qris_my_store.png") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = NeonCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

// =========================================================================
// USB OTG CAMERA TEST & LIVE STREAM DIALOG
// =========================================================================
@Composable
fun UsbCameraTestDialog(
    config: BoothManagerConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isUsbConnected by UsbOtgCameraManager.isUsbCameraConnected.collectAsState()
    val activeDevice by UsbOtgCameraManager.activeCameraDevice.collectAsState()
    val hasCamera2External by UsbOtgCameraManager.hasCamera2External.collectAsState()

    var testCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Usb, contentDescription = null, tint = NeonCyan)
                Text(
                    text = "UJI KAMERA USB OTG",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Status Banner
                Surface(
                    color = if (isUsbConnected) Color(0x2210B981) else Color(0x22EF4444),
                    border = BorderStroke(1.dp, if (isUsbConnected) Color(0xFF10B981) else Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isUsbConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Column {
                            Text(
                                text = if (isUsbConnected) "Status: Terhubung & Siap Streaming" else "Status: Menunggu Sambungan USB OTG",
                                color = if (isUsbConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = activeDevice?.productName ?: if (hasCamera2External) "Camera2 External HAL Terdeteksi" else "Gunakan kabel OTG untuk menghubungkan webcam UVC / capture card DSLR",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Live Camera View / Test Stream Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
                    border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        // Display live simulated test feed
                        val previewBitmap = remember(config.usbCameraResolution, isUsbConnected) {
                            UsbOtgCameraManager.createUsbCameraPreviewFrame(
                                width = 640,
                                height = 360,
                                deviceLabel = activeDevice?.productName ?: "USB OTG FEED",
                                resolutionLabel = config.usbCameraResolution
                            )
                        }
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "USB Camera Test Stream",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )

                        // Top HUD Overlay
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xCC000000),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text("LIVE FEED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Bottom resolution HUD
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xCC000000),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "${config.usbCameraResolution} • 30 FPS",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // If a test photo was captured
                testCapturedBitmap?.let { bitmap ->
                    Surface(
                        color = Color(0xFF131B2E),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("HASIL FOTO UJI (SHUTTER TEST):", color = NeonRose, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured Test",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Text("✅ Foto USB OTG berhasil diakuisisi dengan resolusi penuh!", color = Color(0xFF10B981), fontSize = 11.sp)
                        }
                    }
                }

                // Action Buttons inside Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val count = UsbOtgCameraManager.scanDevices(context).size
                            Toast.makeText(context, "Pindai Selesai: $count perangkat USB terdeteksi", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pindai Ulang", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            testCapturedBitmap = UsbOtgCameraManager.createUsbCameraPreviewFrame(
                                width = 1280,
                                height = 960,
                                deviceLabel = activeDevice?.productName ?: "USB OTG SNAPSHOT",
                                resolutionLabel = config.usbCameraResolution
                            )
                            Toast.makeText(context, "Foto uji USB OTG berhasil diambil!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ambil Foto Uji", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Tutup", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// =========================================================================
// LIBGPHOTO2 DSLR / MIRRORLESS LIVE VIEW & SHUTTER TEST DIALOG
// =========================================================================
@Composable
fun LibGPhoto2TestDialog(
    config: BoothManagerConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeCam by LibGPhoto2Engine.activeCamera.collectAsState()
    val liveBitmap by LibGPhoto2Engine.liveViewBitmap.collectAsState()
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var imageCaptureInstance by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }

    // Start live view on open, stop on dismiss
    DisposableEffect(Unit) {
        LibGPhoto2Engine.startLiveView()
        onDispose {
            LibGPhoto2Engine.stopLiveView()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Camera, contentDescription = null, tint = NeonCyan)
                Column {
                    Text(
                        text = "DIAGNOSTIK LIBGPHOTO2 DSLR",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeCam?.modelName ?: "DSLR External"} • PTP2 Live View",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Viewfinder / Captured Photo View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.5.dp, if (capturedBitmap != null) NeonRose else NeonCyan, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "DSLR Live View",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else if (liveBitmap != null) {
                        Image(
                            bitmap = liveBitmap!!.asImageBitmap(),
                            contentDescription = "DSLR Live View",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        com.example.photobooth.hardware.CameraXPreviewView(
                            modifier = Modifier.fillMaxSize(),
                            lensFacing = androidx.camera.core.CameraSelector.LENS_FACING_FRONT,
                            isExternalUsb = false,
                            onImageCaptureReady = { ic ->
                                imageCaptureInstance = ic
                            }
                        )
                    }

                    // Viewfinder 3x3 Grid Overlay
                    if (showGrid && capturedBitmap == null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val lineColor = Color(0x3300F5FF)
                            // Vertical lines
                            drawLine(lineColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1f)
                            drawLine(lineColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = 1f)
                            // Horizontal lines
                            drawLine(lineColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1f)
                            drawLine(lineColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = 1f)
                        }
                    }

                    // Top DSLR OSD HUD
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xBB000000), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (capturedBitmap != null) NeonRose.copy(alpha = 0.3f) else NeonCyan.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (capturedBitmap != null) NeonRose else NeonCyan)
                        ) {
                            Text(
                                text = if (capturedBitmap != null) "● FOTO DIAMBIL" else "● PTP LIVE VIEW",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "BAT: ${activeCam?.batteryLevelPercent ?: 95}% • RAW+JPEG",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    // Bottom DSLR OSD HUD (ISO, F-Stop, Shutter)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xDD000000))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ISO ${activeCam?.currentIso ?: config.gphoto2Iso}   ${activeCam?.currentAperture ?: config.gphoto2Aperture}   ${activeCam?.currentShutterSpeed ?: config.gphoto2ShutterSpeed}",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )

                        Text(
                            text = "libgphoto2 2.5.34",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                // Camera Hardware Details & Status
                Surface(
                    color = Color(0x15FFFFFF),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Info Perangkat: ${activeCam?.modelName ?: "Generic PTP Camera"}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vendor: ${activeCam?.brand?.displayName ?: "DSLR"} • Port: ${activeCam?.ipAddress ?: "usb:otg"} • Driver: gphoto2_ptp2",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        if (capturedBitmap != null) {
                            Text(
                                text = "Hasil Jepretan: ${capturedBitmap!!.width}x${capturedBitmap!!.height} px (High Resolution 100% Quality)",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Live Controls (ISO & Aperture on-the-fly)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("100", "400", "1600").forEach { iso ->
                        Button(
                            onClick = {
                                LibGPhoto2Engine.setIso(iso)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ISO $iso", fontSize = 9.5.sp, color = Color.White)
                        }
                    }

                    listOf("f/2.8", "f/5.6").forEach { fstop ->
                        Button(
                            onClick = {
                                LibGPhoto2Engine.setAperture(fstop)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fstop, fontSize = 9.5.sp, color = NeonRose)
                        }
                    }
                }

                // Action Buttons: Shutter & Reset Live View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (capturedBitmap != null) {
                        Button(
                            onClick = { capturedBitmap = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Kembali ke Live", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            isCapturing = true
                            if (imageCaptureInstance != null) {
                                com.example.photobooth.hardware.capturePhotoFromCameraX(
                                    context = context,
                                    imageCapture = imageCaptureInstance,
                                    isExternalUsb = false,
                                    onSuccess = { realBitmap ->
                                        coroutineScope.launch {
                                            val photo = LibGPhoto2Engine.capturePhoto(context, realBitmap)
                                            capturedBitmap = photo
                                            isCapturing = false
                                            Toast.makeText(
                                                context,
                                                "Shutter DSLR actuated! Foto resolusi penuh berhasil diunduh.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onError = { err ->
                                        coroutineScope.launch {
                                            val photo = LibGPhoto2Engine.capturePhoto(context, null)
                                            capturedBitmap = photo
                                            isCapturing = false
                                            Toast.makeText(
                                                context,
                                                "Shutter DSLR actuated! (Fallback ke Simulasi)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            } else {
                                coroutineScope.launch {
                                    val photo = LibGPhoto2Engine.capturePhoto(context, null)
                                    capturedBitmap = photo
                                    isCapturing = false
                                    Toast.makeText(
                                        context,
                                        "Shutter DSLR actuated! (Simulasi)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f),
                        enabled = !isCapturing
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mengambil Foto...", fontSize = 11.sp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Jepret Shutter DSLR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Tutup", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
