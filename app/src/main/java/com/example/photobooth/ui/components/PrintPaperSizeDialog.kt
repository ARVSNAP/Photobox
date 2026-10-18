package com.example.photobooth.ui.components

import android.graphics.Bitmap
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.print.PrintHelper

data class PaperSizeOption(
    val id: String,
    val name: String,
    val dimensions: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isRecommendedForStrip: Boolean
)

@Composable
fun PrintPaperSizeDialog(
    composedBitmap: Bitmap,
    layoutType: String,
    initialCopies: Int = 1,
    onDismiss: () -> Unit,
    onPrintConfirmed: (copies: Int, scaleMode: Int, colorMode: Int, orientation: Int, paperSizeName: String) -> Unit
) {
    var copies by remember { mutableIntStateOf(initialCopies) }
    var selectedPaperId by remember { 
        mutableStateOf(
            if (layoutType.contains("Strip", ignoreCase = true)) "strip" else "4r"
        ) 
    }
    var scaleMode by remember { mutableIntStateOf(PrintHelper.SCALE_MODE_FIT) }
    var colorMode by remember { mutableIntStateOf(PrintHelper.COLOR_MODE_COLOR) }
    var orientation by remember { mutableIntStateOf(PrintHelper.ORIENTATION_PORTRAIT) }

    val paperOptions = listOf(
        PaperSizeOption(
            id = "strip",
            name = "Strip Photobooth (2 x 6 in)",
            dimensions = "5 x 15 cm • Kolase Vertikal",
            description = "Ukuran khas strip photo booth kustom, direkomendasikan untuk strip vertikal.",
            icon = Icons.Default.Layers,
            isRecommendedForStrip = true
        ),
        PaperSizeOption(
            id = "4r",
            name = "Cetak Foto 4R (4 x 6 in)",
            dimensions = "10 x 15 cm • Album Standar",
            description = "Ukuran cetak foto album standar terpopuler untuk dipasang di bingkai atau diserahkan.",
            icon = Icons.Default.CropOriginal,
            isRecommendedForStrip = false
        ),
        PaperSizeOption(
            id = "a6",
            name = "Postcard A6 (4.1 x 5.8 in)",
            dimensions = "10.5 x 14.8 cm • Kartu Pos",
            description = "Ukuran ringkas yang pas untuk kartu ucapan, tanda terima, atau kolase meja.",
            icon = Icons.Default.FeaturedPlayList,
            isRecommendedForStrip = false
        ),
        PaperSizeOption(
            id = "a4",
            name = "Dokumen A4 (8.3 x 11.7 in)",
            dimensions = "21 x 29.7 cm • Cetak Penuh",
            description = "Format cetak penuh yang besar, terbaik untuk poster, kolase dinding, atau bingkai besar.",
            icon = Icons.Default.Description,
            isRecommendedForStrip = false
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF06B6D4), // Cyan
                                    Color(0xFF8B5CF6), // Purple
                                    Color(0xFFEC4899)  // Pink
                                )
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF16161E) // Elegant dark background matching Studio Design
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- HEADER ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengaturan Ukuran Kertas Cetak",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Pilih ukuran kertas sebelum mengirim ke printer",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A2A38))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // --- BANNER REKOMENDASI ---
                    val isStripLayout = layoutType.contains("Strip", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isStripLayout) {
                                    "Layout Anda terdeteksi bertipe Strip. Kami merekomendasikan ukuran kertas 'Strip Photobooth' untuk hasil potong yang pas!"
                                } else {
                                    "Untuk layout kolase grid standar, kami menyarankan ukuran cetak kertas '4R'."
                                },
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // --- PILIHAN UKURAN KERTAS (PAPER SIZE SELECTOR LIST) ---
                    Text(
                        text = "PILIH UKURAN KERTAS:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        paperOptions.forEach { option ->
                            val isSelected = option.id == selectedPaperId
                            val borderBrush = if (isSelected) {
                                Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF2E2E3E), Color(0xFF2E2E3E)))
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF1E2436) else Color(0xFF181824),
                                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderBrush),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPaperId = option.id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFF06B6D4).copy(alpha = 0.2f) else Color(0xFF2D2B52)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF06B6D4) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = option.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (isStripLayout && option.isRecommendedForStrip) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                    border = BorderStroke(1.dp, Color(0xFF10B981))
                                                ) {
                                                    Text(
                                                        text = "Rekomendasi",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = option.dimensions,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = option.description,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPaperId = option.id },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF06B6D4),
                                            unselectedColor = Color(0xFF475569)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // --- ADVANCED SETTINGS (COPIES, SCALE MODE, COLOR MODE) ---
                    Divider(color = Color(0xFF2E2E3E), thickness = 1.dp)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PENGATURAN TAMBAHAN:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        // 1. Copies adjustment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jumlah Rangkap Salinan (Copies)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E1E2A),
                                border = BorderStroke(1.dp, Color(0xFF2E2E3E))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (copies > 1) copies-- },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Kurang",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "$copies Rangkap",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    IconButton(
                                        onClick = { if (copies < 10) copies++ },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Tambah",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Scale mode (Fit vs Fill)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Skala Tata Letak",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                              ) {
                                listOf(
                                    PrintHelper.SCALE_MODE_FIT to "Pas Halaman (Fit)",
                                    PrintHelper.SCALE_MODE_FILL to "Penuhi (Fill)"
                                ).forEach { (mode, label) ->
                                    val isSel = scaleMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFF06B6D4) else Color(0xFF1E1E2A),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFF06B6D4) else Color(0x22FFFFFF)),
                                        modifier = Modifier.clickable { scaleMode = mode }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSel) Color.Black else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Color Mode (Color vs Grayscale)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mode Warna",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    PrintHelper.COLOR_MODE_COLOR to "Cetak Warna",
                                    PrintHelper.COLOR_MODE_MONOCHROME to "Grayscale"
                                ).forEach { (mode, label) ->
                                    val isSel = colorMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFF8B5CF6) else Color(0xFF1E1E2A),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFF8B5CF6) else Color(0x22FFFFFF)),
                                        modifier = Modifier.clickable { colorMode = mode }
                                    ) {
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Orientation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Orientasi Kertas",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    PrintHelper.ORIENTATION_PORTRAIT to "Potret (Vertikal)",
                                    PrintHelper.ORIENTATION_LANDSCAPE to "Lanskap (Horisontal)"
                                ).forEach { (mode, label) ->
                                    val isSel = orientation == mode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFFEC4899) else Color(0xFF1E1E2A),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFFEC4899) else Color(0x22FFFFFF)),
                                        modifier = Modifier.clickable { orientation = mode }
                                    ) {
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- ACTION BUTTONS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E2E3E)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E1E2A)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(text = "Batal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val selectedPaperName = paperOptions.find { it.id == selectedPaperId }?.name ?: "4R"
                                onPrintConfirmed(copies, scaleMode, colorMode, orientation, selectedPaperName)
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6)))),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "KIRIM KE PRINTER",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
