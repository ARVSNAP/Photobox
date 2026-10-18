package com.example.photobooth.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photobooth.hardware.PrintController
import com.example.photobooth.utils.QrCodeGenerator
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SharingHubScreen(
    finalBitmap: Bitmap,
    gifFrames: List<Bitmap> = emptyList(),
    gifBytes: ByteArray? = null,
    isGifMode: Boolean = false,
    onSessionFinished: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var printCount by remember { mutableStateOf(1) }
    var isPrintSuccess by remember { mutableStateOf(false) }
    var isPlayingGif by remember { mutableStateOf(true) }
    var activeFrameIdx by remember { mutableStateOf(0) }
    var showStripPreview by remember { mutableStateOf(!isGifMode) }

    // Animated GIF frame playback
    val loopFrames = remember(gifFrames) {
        if (gifFrames.size > 2) {
            val reversed = gifFrames.subList(1, gifFrames.size - 1).reversed()
            gifFrames + reversed
        } else {
            gifFrames
        }
    }

    LaunchedEffect(isPlayingGif, loopFrames) {
        if (isPlayingGif && loopFrames.isNotEmpty()) {
            while (true) {
                delay(140)
                activeFrameIdx = (activeFrameIdx + 1) % loopFrames.size
            }
        }
    }

    // Cloudinary States
    var isUploading by remember { mutableStateOf(false) }
    var uploadUrl by remember { mutableStateOf<String?>(null) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(finalBitmap, gifBytes) {
        if (com.example.photobooth.utils.CloudinaryUploader.isConfigured()) {
            isUploading = true
            uploadError = null
            
            val bytes = if (isGifMode) {
                gifBytes
            } else {
                val stream = java.io.ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.toByteArray()
            }

            if (bytes != null && bytes.isNotEmpty()) {
                val url = com.example.photobooth.utils.CloudinaryUploader.uploadToCloudinary(bytes, isGifMode)
                if (url != null) {
                    uploadUrl = url
                } else {
                    uploadError = "Upload gagal, menggunakan QR Code lokal"
                }
            } else {
                uploadError = "Data media kosong"
            }
            isUploading = false
        }
    }

    val qrCodeBitmap = remember(uploadUrl) {
        val url = uploadUrl ?: "https://photobooth.kiosk/download/${if (isGifMode) "animated-gif" else "photo-strip"}/${System.currentTimeMillis()}"
        QrCodeGenerator.generateQrCode(url)
    }

    val printController = remember { PrintController(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF030712))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = if (isGifMode) "GIF & SHARING HUB" else "PRINT & DIGITAL SHARING",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Scan QR Code for instant download or print copies",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        // Main Stacked Content for Mobile Portrait
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x221E293B)),
                border = BorderStroke(1.dp, if (isGifMode) NeonRose.copy(alpha = 0.6f) else Color(0x33FFFFFF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isGifMode && !showStripPreview) Icons.Default.MovieFilter else Icons.Default.Print,
                                contentDescription = null,
                                tint = if (isGifMode) NeonRose else NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isGifMode && !showStripPreview) "Live GIF Preview" else "Print Layout",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isGifMode) {
                            FilterChip(
                                selected = showStripPreview,
                                onClick = { showStripPreview = !showStripPreview },
                                label = { Text(if (showStripPreview) "GIF Loop" else "Print Strip", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGifMode && !showStripPreview && loopFrames.isNotEmpty()) {
                            val frame = loopFrames.getOrNull(activeFrameIdx) ?: loopFrames[0]
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(frame.width.toFloat() / frame.height.toFloat())
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(2.dp, NeonRose), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = frame.asImageBitmap(),
                                    contentDescription = "Animated GIF Frame",
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Frame tag
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xCC000000))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${activeFrameIdx + 1}/${loopFrames.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Image(
                                bitmap = finalBitmap.asImageBitmap(),
                                contentDescription = "Final Photo Strip",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0x44FFFFFF)), RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }

            // Digital QR & Sharing Hub Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x221E293B)),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = NeonRose)
                        Text(
                            text = if (isGifMode) "Instant GIF Mobile Download" else "Instant Mobile Download",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                color = NeonRose,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Image(
                                bitmap = qrCodeBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        text = if (isUploading) {
                            "Mengunggah ke Cloudinary..."
                        } else if (uploadUrl != null) {
                            "Scan QR Code untuk download langsung dari Cloud!"
                        } else if (!uploadError.isNullOrBlank()) {
                            "Gagal upload, menggunakan link lokal"
                        } else if (isGifMode) {
                            "Scan with phone camera to save high-speed animated GIF"
                        } else {
                            "Scan with phone camera to save high-resolution digital photo"
                        },
                        color = if (isUploading) NeonCyan else if (uploadUrl != null) NeonCyan else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isUploading || uploadUrl != null) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // Social / Email Share Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (uploadUrl != null) {
                                    val annotated = androidx.compose.ui.text.buildAnnotatedString { append(uploadUrl ?: "") }
                                    clipboardManager.setText(annotated)
                                    Toast.makeText(context, "Link Cloudinary berhasil disalin!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (isGifMode) "Exporting GIF file..." else "Saving photo...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uploadUrl != null) "Salin Link" else if (isGifMode) "Save GIF" else "Save", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (uploadUrl != null) {
                                    val annotated = androidx.compose.ui.text.buildAnnotatedString { append(uploadUrl ?: "") }
                                    clipboardManager.setText(annotated)
                                    Toast.makeText(context, "Link disalin! Bagikan ke Email.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Sending via Email...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (uploadUrl != null) {
                                    val annotated = androidx.compose.ui.text.buildAnnotatedString { append(uploadUrl ?: "") }
                                    clipboardManager.setText(annotated)
                                    Toast.makeText(context, "Link disalin! Bagikan ke WhatsApp.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Generating WhatsApp link...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Print Quantity & Execution Control Bar for Mobile
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x331E293B))
                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Print Copies:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { if (printCount > 1) printCount-- },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "$printCount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { if (printCount < 10) printCount++ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        printController.printWithAndroidPrintHelper(finalBitmap)
                        isPrintSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPrintSuccess) Icons.Default.CheckCircle else Icons.Default.Print,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPrintSuccess) "PRINT JOB SENT ✅" else "PRINT PHOTO ($printCount COPIES)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onSessionFinished,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(text = "FINISH / NEW SESSION", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
