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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.photobooth.model.LayoutCardCustomization
import com.example.photobooth.ui.screens.LayoutOptionItem
import com.example.photobooth.utils.LayoutCardCustomizationStorage
import com.example.ui.theme.*

@Composable
fun LayoutIconEditorDialog(
    layout: LayoutOptionItem,
    initialCustomization: LayoutCardCustomization,
    onDismiss: () -> Unit,
    onSave: (LayoutCardCustomization) -> Unit
) {
    val context = LocalContext.current

    var visualStyle by remember { mutableStateOf(initialCustomization.visualStyle) }
    var customImageUri by remember { mutableStateOf(initialCustomization.customImageUri) }
    var colorFilter by remember { mutableStateOf(initialCustomization.colorFilter) }
    var customTitle by remember { mutableStateOf(initialCustomization.customTitle ?: "") }
    var customBadge by remember { mutableStateOf(initialCustomization.customBadge ?: "") }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedUri = LayoutCardCustomizationStorage.saveCustomImagePermanently(context, layout.id, uri)
            if (savedUri != null) {
                customImageUri = savedUri.toString()
                visualStyle = "custom_image"
                Toast.makeText(context, "Gambar ikon berhasil diunggah!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memproses gambar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val currentCustomization = remember(visualStyle, customImageUri, colorFilter, customTitle, customBadge) {
        LayoutCardCustomization(
            layoutId = layout.id,
            customImageUri = customImageUri,
            visualStyle = visualStyle,
            colorFilter = colorFilter,
            customTitle = customTitle.ifBlank { null },
            customBadge = customBadge.ifBlank { null }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonRose, NeonCyan)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NeonRose.copy(alpha = 0.2f))
                                .border(1.dp, NeonRose, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = NeonRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SUNTING IKON GRID",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = layout.title,
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LIVE PREVIEW SECTION
                    Text(
                        text = "PRATINJAU KARTU GRID (LIVE):",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Card mockup
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(2.dp, NeonRose),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(165.dp)
                                .height(225.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                ) {
                                    LayoutCardVisualPreview(
                                        layout = layout,
                                        customization = currentCustomization
                                    )

                                    // Badge in preview
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xCC000000))
                                            .border(BorderStroke(1.dp, Color(0x44FFFFFF)), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = customBadge.ifBlank { layout.badgeLabel },
                                            color = NeonRose,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = customTitle.ifBlank { layout.title },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = layout.description,
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // OPTION 1: UPLOAD CUSTOM IMAGE
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (visualStyle == "custom_image") NeonRose else Color(0x33FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "UNGGAH GAMBAR / FOTO KUSTOM",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ganti gambar ikon dari galeri perangkat Anda",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                if (visualStyle == "custom_image" && customImageUri != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NeonRose.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "AKTIF",
                                            color = NeonRose,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (visualStyle == "custom_image") NeonRose else Color(0xFF334155)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (customImageUri != null) "Ganti Gambar" else "Pilih Foto Galeri",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (customImageUri != null) {
                                    OutlinedButton(
                                        onClick = {
                                            customImageUri = null
                                            visualStyle = "default"
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus Gambar",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // OPTION 2: VISUAL STYLE PRESETS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PILIH GAYA VISUAL / PRESET IKON:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        val presetStyles = listOf(
                            Triple("default", "Foto Model Asli", "Tampilan photobooth default bawaan"),
                            Triple("blueprint", "Blueprint Minimal", "Skema CAD grid arsitektur teknis"),
                            Triple("neon_glow", "Neon Cyber Glow", "Nuansa neon pink & cyan futuristik"),
                            Triple("polaroid", "Polaroid Retro", "Gaya cetak polaroid vintage hangat"),
                            Triple("monolith", "Dark Luxe Monolith", "Estetika titanium hitam premium")
                        )

                        presetStyles.forEach { (styleKey, title, desc) ->
                            val isSelected = visualStyle == styleKey
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonRose.copy(alpha = 0.18f) else Color(0xFF1E293B))
                                    .border(
                                        BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) NeonRose else Color(0x22FFFFFF)
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { visualStyle = styleKey }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = desc,
                                            color = TextMuted,
                                            fontSize = 10.5.sp
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(NeonRose),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // OPTION 3: COLOR TINT FILTERS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "FILTER / NUANSA WARNA OVERLAY:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf(
                                Triple("none", "Asli", Color.Gray),
                                Triple("rose", "Rose Glow", NeonRose),
                                Triple("cyan", "Cyber Cyan", NeonCyan),
                                Triple("amber", "Amber Warm", Color(0xFFF59E0B)),
                                Triple("noir", "Noir Contrast", Color(0xFF1E293B))
                            )

                            filters.forEach { (filterKey, label, color) ->
                                val isSelected = colorFilter == filterKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.25f) else Color(0xFF1E293B))
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (isSelected) color else Color(0x22FFFFFF)
                                            ),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { colorFilter = filterKey }
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else TextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // OPTION 4: CUSTOM LABELS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "KUSTOMISASI LABEL TEKS (OPSIONAL):",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Nama Judul Layout (Default: ${layout.title})") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x44FFFFFF)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customBadge,
                            onValueChange = { customBadge = it },
                            label = { Text("Label Badge Sudut (Default: ${layout.badgeLabel})") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonRose,
                                unfocusedBorderColor = Color(0x44FFFFFF)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            LayoutCardCustomizationStorage.resetCustomization(context, layout.id)
                            onSave(LayoutCardCustomization(layoutId = layout.id))
                            Toast.makeText(context, "Ikon layout dikembalikan ke default", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Default", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            LayoutCardCustomizationStorage.saveCustomization(context, currentCustomization)
                            onSave(currentCustomization)
                            Toast.makeText(context, "Tampilan ikon grid berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan Tampilan",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
