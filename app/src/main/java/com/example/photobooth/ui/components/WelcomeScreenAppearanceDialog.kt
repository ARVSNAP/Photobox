package com.example.photobooth.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photobooth.model.BoothManagerConfig
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreenAppearanceDialog(
    config: BoothManagerConfig,
    onConfigChanged: (BoothManagerConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var eventName by remember(config) { mutableStateOf(config.eventName) }
    var eventSubtitle by remember(config) { mutableStateOf(config.eventSubtitle) }
    var welcomeBadgeText by remember(config) { mutableStateOf(config.welcomeBadgeText) }
    var welcomeStartButtonText by remember(config) { mutableStateOf(config.welcomeStartButtonText) }
    var welcomeThemeColorHex by remember(config) { mutableStateOf(config.welcomeThemeColorHex) }
    var welcomeAccentColorHex by remember(config) { mutableStateOf(config.welcomeAccentColorHex) }
    var welcomeLogoPreset by remember(config) { mutableStateOf(config.welcomeLogoPreset) }
    var welcomeCustomLogoUri by remember(config) { mutableStateOf(config.welcomeCustomLogoUri) }

    // Launcher for custom welcome logo from gallery
    val pickLogoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            welcomeCustomLogoUri = uri.toString()
            welcomeLogoPreset = "Custom Logo"
            Toast.makeText(context, "Logo kustom berhasil diunggah!", Toast.LENGTH_SHORT).show()
        }
    }

    val colorSchemes = listOf(
        ThemeColorScheme("Cyberpunk", "#FF007F", "#00F5FF"),
        ThemeColorScheme("Neon Sunset", "#FF007F", "#FFAB00"),
        ThemeColorScheme("Emerald Mint", "#10B981", "#00F5FF"),
        ThemeColorScheme("Electric Violet", "#8B5CF6", "#FF007F"),
        ThemeColorScheme("Classic Slate", "#FFFFFF", "#64748B")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(welcomeThemeColorHex.toColorIntOrSafe()).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EDIT TAMPILAN WELCOME SCREEN",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kustomisasi penuh teks, logo, dan tema warna utama halaman depan",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Form Fields Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Teks Kustomisasi
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "INFORMASI TEKS",
                                    color = Color(welcomeThemeColorHex.toColorIntOrSafe()),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                OutlinedTextField(
                                    value = eventName,
                                    onValueChange = { eventName = it },
                                    label = { Text("Judul Booth Utama", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(welcomeThemeColorHex.toColorIntOrSafe()),
                                        unfocusedBorderColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = eventSubtitle,
                                    onValueChange = { eventSubtitle = it },
                                    label = { Text("Sub-judul Booth", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(welcomeThemeColorHex.toColorIntOrSafe()),
                                        unfocusedBorderColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = welcomeBadgeText,
                                        onValueChange = { welcomeBadgeText = it },
                                        label = { Text("Status Badge", color = TextMuted) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(welcomeThemeColorHex.toColorIntOrSafe()),
                                            unfocusedBorderColor = Color(0x33FFFFFF)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = welcomeStartButtonText,
                                        onValueChange = { welcomeStartButtonText = it },
                                        label = { Text("Teks Tombol Mulai", color = TextMuted) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(welcomeThemeColorHex.toColorIntOrSafe()),
                                            unfocusedBorderColor = Color(0x33FFFFFF)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // 2. Skema Warna (Tema)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "PILIH TEMA WARNA",
                                    color = Color(welcomeThemeColorHex.toColorIntOrSafe()),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    colorSchemes.forEach { scheme ->
                                        val isSel = welcomeThemeColorHex.equals(scheme.themeHex, ignoreCase = true)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) Color(scheme.themeHex.toColorIntOrSafe()).copy(alpha = 0.15f) else Color.Transparent)
                                                .border(
                                                    BorderStroke(
                                                        1.5.dp,
                                                        if (isSel) Color(scheme.themeHex.toColorIntOrSafe()) else Color(0x22FFFFFF)
                                                    ),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    welcomeThemeColorHex = scheme.themeHex
                                                    welcomeAccentColorHex = scheme.accentHex
                                                }
                                                .padding(vertical = 10.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(scheme.themeHex.toColorIntOrSafe()))
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(scheme.accentHex.toColorIntOrSafe()))
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = scheme.name,
                                                color = if (isSel) Color.White else TextMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Logo Kustomisasi
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "PRESET ATAU LOGO KUSTOM",
                                    color = Color(welcomeThemeColorHex.toColorIntOrSafe()),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                val logoPresets = listOf("Default Logo", "Sparkles", "Camera", "Smiley")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    logoPresets.forEach { preset ->
                                        val isSel = welcomeLogoPreset == preset
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSel) Color(welcomeThemeColorHex.toColorIntOrSafe()).copy(alpha = 0.15f) else Color(0x11FFFFFF))
                                                .border(
                                                    BorderStroke(
                                                        1.dp,
                                                        if (isSel) Color(welcomeThemeColorHex.toColorIntOrSafe()) else Color(0x22FFFFFF)
                                                    ),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { welcomeLogoPreset = preset }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = preset,
                                                color = if (isSel) Color.White else TextMuted,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { pickLogoLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x331E293B)),
                                    border = BorderStroke(1.dp, Color(welcomeAccentColorHex.toColorIntOrSafe()).copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = Color(welcomeAccentColorHex.toColorIntOrSafe()), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pilih Gambar Logo dari Galeri", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (welcomeCustomLogoUri != null || welcomeLogoPreset != "Default Logo") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x22FF007F))
                                            .border(BorderStroke(1.dp, Color(0x66FF007F)), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (welcomeCustomLogoUri != null) "Logo Kustom Terpasang" else "Preset Logo: $welcomeLogoPreset",
                                            color = NeonRose,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Button(
                                            onClick = {
                                                welcomeCustomLogoUri = null
                                                welcomeLogoPreset = "Default Logo"
                                                Toast.makeText(context, "Logo kustom/preset berhasil dihapus & direset ke Default!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF007F)),
                                            border = BorderStroke(1.dp, NeonRose),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus", tint = NeonRose, modifier = Modifier.size(12.dp))
                                                Text("Hapus / Reset 🗑️", fontSize = 10.sp, color = NeonRose, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                onConfigChanged(
                                    config.copy(
                                        eventName = eventName,
                                        eventSubtitle = eventSubtitle,
                                        welcomeBadgeText = welcomeBadgeText,
                                        welcomeStartButtonText = welcomeStartButtonText,
                                        welcomeThemeColorHex = welcomeThemeColorHex,
                                        welcomeAccentColorHex = welcomeAccentColorHex,
                                        welcomeLogoPreset = welcomeLogoPreset,
                                        welcomeCustomLogoUri = welcomeCustomLogoUri
                                    )
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(welcomeThemeColorHex.toColorIntOrSafe())),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Tampilan")
                        }
                    }
                }
            }
        }
    }
}

data class ThemeColorScheme(
    val name: String,
    val themeHex: String,
    val accentHex: String
)

// Helper function to safely parse Hex String to Color Int
fun String.toColorIntOrSafe(): Int {
    return try {
        android.graphics.Color.parseColor(this)
    } catch (e: Exception) {
        android.graphics.Color.parseColor("#FF007F") // default fallback
    }
}
