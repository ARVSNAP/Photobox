package com.example.photobooth.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.photobooth.utils.PngTransparentDetector
import com.example.photobooth.utils.TransparentSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PngLayoutConfigDialog(
    imageUri: Uri? = null,
    existingImagePath: String? = null,
    initialTitle: String = "Frame PNG 4",
    initialCategory: String = "Layout PNG Kustom",
    initialBadge: String = "👑 Pro",
    initialSlots: List<TransparentSlot> = emptyList(),
    onDismiss: () -> Unit,
    onSaveLayout: (title: String, category: String, badgeLabel: String, slots: List<TransparentSlot>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentUri by remember { mutableStateOf(imageUri) }
    var currentImagePath by remember { mutableStateOf(existingImagePath) }

    var title by remember { mutableStateOf(if (initialTitle.isNotBlank()) initialTitle else "Frame PNG 4") }
    var category by remember { mutableStateOf(initialCategory) }
    var badgeLabel by remember { mutableStateOf(initialBadge) }

    var simulatePhotos by remember { mutableStateOf(true) }
    var cornerRadiusDp by remember { mutableFloatStateOf(16f) }
    var selectedSlotIndex by remember { mutableIntStateOf(0) }
    var selectedPresetMode by remember { mutableStateOf("Auto") }

    var isDetecting by remember { mutableStateOf(false) }
    var detectedSlots by remember { mutableStateOf<List<TransparentSlot>>(initialSlots) }

    // Image dimension state
    var imageDimensions by remember { mutableStateOf(Pair(1023, 3072)) }

    // Resolve file from current URI or existing path
    val resolvedFile = remember(currentUri, currentImagePath) {
        if (currentImagePath != null) {
            val f = File(currentImagePath!!)
            if (f.exists()) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.absolutePath, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    imageDimensions = Pair(opts.outWidth, opts.outHeight)
                }
            }
            f
        } else if (currentUri != null) {
            try {
                val f = File(context.cacheDir, "temp_cfg_${System.currentTimeMillis()}.png")
                context.contentResolver.openInputStream(currentUri!!)?.use { input ->
                    f.outputStream().use { output -> input.copyTo(output) }
                }
                if (f.exists()) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.absolutePath, opts)
                    if (opts.outWidth > 0 && opts.outHeight > 0) {
                        imageDimensions = Pair(opts.outWidth, opts.outHeight)
                    }
                }
                f
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Change photo picker launcher
    val changePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { newUri ->
        if (newUri != null) {
            currentUri = newUri
            currentImagePath = null
            detectedSlots = emptyList()
        }
    }

    // Auto-detect transparent slots when image loads
    LaunchedEffect(resolvedFile) {
        if (resolvedFile != null && resolvedFile.exists() && detectedSlots.isEmpty()) {
            isDetecting = true
            coroutineScope.launch(Dispatchers.IO) {
                val slots = PngTransparentDetector.detectTransparentSlots(
                    imagePath = resolvedFile.absolutePath,
                    alphaThreshold = 40
                )
                withContext(Dispatchers.Main) {
                    detectedSlots = if (slots.isNotEmpty()) {
                        slots
                    } else {
                        listOf(
                            TransparentSlot(0.08f, 0.08f, 0.92f, 0.35f),
                            TransparentSlot(0.08f, 0.38f, 0.92f, 0.65f),
                            TransparentSlot(0.08f, 0.68f, 0.92f, 0.95f)
                        )
                    }
                    selectedPresetMode = "Auto"
                    isDetecting = false
                }
            }
        }
    }

    fun runAutoDetect() {
        if (resolvedFile == null || !resolvedFile.exists()) return
        isDetecting = true
        coroutineScope.launch(Dispatchers.IO) {
            val slots = PngTransparentDetector.detectTransparentSlots(
                imagePath = resolvedFile.absolutePath,
                alphaThreshold = 40
            )
            withContext(Dispatchers.Main) {
                if (slots.isNotEmpty()) {
                    detectedSlots = slots
                    selectedSlotIndex = 0
                }
                selectedPresetMode = "Auto"
                isDetecting = false
            }
        }
    }

    fun applyPresetLayout(mode: String) {
        selectedPresetMode = mode
        when (mode) {
            "Auto" -> runAutoDetect()
            "1 Foto" -> {
                detectedSlots = listOf(
                    TransparentSlot(0.08f, 0.12f, 0.92f, 0.88f)
                )
                selectedSlotIndex = 0
            }
            "2 Foto" -> {
                detectedSlots = listOf(
                    TransparentSlot(0.08f, 0.08f, 0.92f, 0.48f),
                    TransparentSlot(0.08f, 0.52f, 0.92f, 0.92f)
                )
                selectedSlotIndex = 0
            }
            "4 Grid" -> {
                detectedSlots = listOf(
                    TransparentSlot(0.06f, 0.08f, 0.48f, 0.48f),
                    TransparentSlot(0.52f, 0.08f, 0.94f, 0.48f),
                    TransparentSlot(0.06f, 0.52f, 0.48f, 0.92f),
                    TransparentSlot(0.52f, 0.52f, 0.94f, 0.92f)
                )
                selectedSlotIndex = 0
            }
            "6 Grid" -> {
                detectedSlots = listOf(
                    TransparentSlot(0.06f, 0.06f, 0.48f, 0.34f),
                    TransparentSlot(0.52f, 0.06f, 0.94f, 0.34f),
                    TransparentSlot(0.06f, 0.36f, 0.48f, 0.64f),
                    TransparentSlot(0.52f, 0.36f, 0.94f, 0.64f),
                    TransparentSlot(0.06f, 0.66f, 0.48f, 0.94f),
                    TransparentSlot(0.52f, 0.66f, 0.94f, 0.94f)
                )
                selectedSlotIndex = 0
            }
        }
    }

    // Update slot helper functions
    fun updateSelectedSlot(newSlot: TransparentSlot) {
        if (selectedSlotIndex in detectedSlots.indices) {
            val list = detectedSlots.toMutableList()
            list[selectedSlotIndex] = TransparentSlot(
                left = newSlot.left.coerceIn(0f, 0.95f),
                top = newSlot.top.coerceIn(0f, 0.95f),
                right = newSlot.right.coerceIn(0.05f, 1f),
                bottom = newSlot.bottom.coerceIn(0.05f, 1f)
            )
            detectedSlots = list
        }
    }

    fun nudgeSelectedSlot(dx: Float, dy: Float) {
        if (selectedSlotIndex in detectedSlots.indices) {
            val slot = detectedSlots[selectedSlotIndex]
            val w = slot.right - slot.left
            val h = slot.bottom - slot.top
            var newLeft = slot.left + dx
            var newTop = slot.top + dy

            newLeft = newLeft.coerceIn(0f, 1f - w)
            newTop = newTop.coerceIn(0f, 1f - h)

            updateSelectedSlot(
                TransparentSlot(
                    left = newLeft,
                    top = newTop,
                    right = (newLeft + w).coerceAtMost(1f),
                    bottom = (newTop + h).coerceAtMost(1f)
                )
            )
        }
    }

    fun resizeSelectedSlot(dw: Float, dh: Float) {
        if (selectedSlotIndex in detectedSlots.indices) {
            val slot = detectedSlots[selectedSlotIndex]
            val centerX = (slot.left + slot.right) / 2f
            val centerY = (slot.top + slot.bottom) / 2f
            val currentW = slot.right - slot.left
            val currentH = slot.bottom - slot.top

            val newW = (currentW + dw).coerceIn(0.05f, 1f)
            val newH = (currentH + dh).coerceIn(0.05f, 1f)

            val newLeft = (centerX - newW / 2f).coerceIn(0f, 1f - newW)
            val newTop = (centerY - newH / 2f).coerceIn(0f, 1f - newH)

            updateSelectedSlot(
                TransparentSlot(
                    left = newLeft,
                    top = newTop,
                    right = (newLeft + newW).coerceAtMost(1f),
                    bottom = (newTop + newH).coerceAtMost(1f)
                )
            )
        }
    }

    val slotColors = listOf(
        Pair(Color(0xFF8B5CF6), Color(0xFF6366F1)), // Purple / Indigo
        Pair(Color(0xFFEC4899), Color(0xFFF43F5E)), // Pink / Rose
        Pair(Color(0xFF3B82F6), Color(0xFF0EA5E9)), // Blue / Sky
        Pair(Color(0xFF10B981), Color(0xFF059669)), // Emerald
        Pair(Color(0xFFF59E0B), Color(0xFFD97706)), // Amber
        Pair(Color(0xFF8B5CF6), Color(0xFFA855F7)), // Violet
        Pair(Color(0xFF06B6D4), Color(0xFF0284C7)), // Cyan
        Pair(Color(0xFFE11D48), Color(0xFFBE123C))  // Rose
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main Modal Card Container with Neon Gradient Border
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFEC4899), // Pink
                                    Color(0xFF8B5CF6), // Purple
                                    Color(0xFF06B6D4)  // Cyan
                                )
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF16161E) // Deep elegant dark container
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- TOP HEADER ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gradient Icon (Layers)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & Subtitle Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Konfigurasi Layout PNG",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Resolusi: ${imageDimensions.first} x ${imageDimensions.second} px • ${detectedSlots.size} Slot Foto",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        // Change Image Button
                        IconButton(
                            onClick = { changePhotoLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D2B52))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Ganti Foto",
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A2A38))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // --- BANNER ALERT NOTIFICATION ---
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF262444),
                        border = BorderStroke(1.dp, Color(0xFF3F3B6E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Terdeteksi ${detectedSlots.size} kotak transparan! Foto akan otomatis mengisi kotak ini.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, Color(0xFF8B5CF6))
                                ) {
                                    Text(
                                        text = "⚡ AUTO ADAPTIF",
                                        color = Color(0xFFA78BFA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Otomatis mengisi opsi layout Square (1:1), Layout 2x6, dan Layout 4x6",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // --- INTERACTIVE VISUAL PNG PREVIEW CANVAS (WITH DRAG & TOUCH SUPPORT) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0D0D12))
                            .border(BorderStroke(1.dp, Color(0xFF2E2E3E)), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resolvedFile != null && resolvedFile.exists()) {
                            val imgAspect = imageDimensions.first.toFloat() / imageDimensions.second.toFloat()

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(imgAspect)
                                    .pointerInput(detectedSlots, selectedSlotIndex) {
                                        detectTapGestures { tapOffset ->
                                            val cW = size.width
                                            val cH = size.height
                                            // Check which slot was tapped
                                            detectedSlots.forEachIndexed { index, slot ->
                                                val left = slot.left * cW
                                                val top = slot.top * cH
                                                val right = slot.right * cW
                                                val bottom = slot.bottom * cH
                                                if (tapOffset.x in left..right && tapOffset.y in top..bottom) {
                                                    selectedSlotIndex = index
                                                    return@detectTapGestures
                                                }
                                            }
                                        }
                                    }
                                    .pointerInput(detectedSlots, selectedSlotIndex) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val cW = size.width
                                            val cH = size.height
                                            if (cW > 0 && cH > 0) {
                                                val dx = dragAmount.x / cW
                                                val dy = dragAmount.y / cH
                                                nudgeSelectedSlot(dx, dy)
                                            }
                                        }
                                    }
                            ) {
                                // Layer 1: Simulated Photos (Behind PNG Frame)
                                if (simulatePhotos) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cW = size.width
                                        val cH = size.height
                                        val cornerPx = cornerRadiusDp * (cW / 300f)

                                        detectedSlots.forEachIndexed { index, slot ->
                                            val left = slot.left * cW
                                            val top = slot.top * cH
                                            val right = slot.right * cW
                                            val bottom = slot.bottom * cH
                                            val width = right - left
                                            val height = bottom - top

                                            val (color1, color2) = slotColors[index % slotColors.size]

                                            // Draw Simulated Photo Card
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(listOf(color1, color2)),
                                                topLeft = Offset(left, top),
                                                size = Size(width, height),
                                                cornerRadius = CornerRadius(cornerPx, cornerPx)
                                            )
                                        }
                                    }

                                    // Overlay Text for simulated photos
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        detectedSlots.forEachIndexed { index, slot ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth(slot.right - slot.left)
                                                    .fillMaxHeight(slot.bottom - slot.top)
                                                    .align(
                                                        androidx.compose.ui.BiasAlignment(
                                                            horizontalBias = (slot.left + slot.right) - 1f,
                                                            verticalBias = (slot.top + slot.bottom) - 1f
                                                        )
                                                    ),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                // Slot badge
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color.White.copy(alpha = 0.25f),
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "#${index + 1}",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "FOTO #${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "Pose Kamera",
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                // Layer 2: PNG Frame (Overlay)
                                AsyncImage(
                                    model = resolvedFile,
                                    contentDescription = "Frame PNG Overlay",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Layer 3: Interactive Dashed Slot Outline & Resize Handles
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cW = size.width
                                    val cH = size.height

                                    detectedSlots.forEachIndexed { index, slot ->
                                        val isSelected = (index == selectedSlotIndex)
                                        val left = slot.left * cW
                                        val top = slot.top * cH
                                        val right = slot.right * cW
                                        val bottom = slot.bottom * cH
                                        val width = right - left
                                        val height = bottom - top

                                        // Dashed Bounding Box
                                        drawRect(
                                            color = if (isSelected) Color(0xFFEC4899) else Color(0x6606B6D4),
                                            topLeft = Offset(left, top),
                                            size = Size(width, height),
                                            style = Stroke(
                                                width = if (isSelected) 2.5.dp.toPx() else 1.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                            )
                                        )

                                        // Corner Handles for Selected Slot
                                        if (isSelected) {
                                            val handleSize = 7.dp.toPx()
                                            val handleColor = Color(0xFFEC4899)

                                            drawCircle(handleColor, handleSize, Offset(left, top))
                                            drawCircle(handleColor, handleSize, Offset(right, top))
                                            drawCircle(handleColor, handleSize, Offset(left, bottom))
                                            drawCircle(handleColor, handleSize, Offset(right, bottom))
                                            drawCircle(Color.White, handleSize * 0.5f, Offset((left + right) / 2f, (top + bottom) / 2f))
                                        }
                                    }
                                }
                            }
                        }

                        if (isDetecting) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFFEC4899), strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Mendeteksi Kotak Transparan...",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // --- SLOT SELECTOR ROW ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Slot:",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            detectedSlots.forEachIndexed { index, _ ->
                                val isChosen = (index == selectedSlotIndex)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChosen) Color(0xFF2C2A4A) else Color(0xFF1E1E2A),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isChosen) Color(0xFF8B5CF6) else Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.clickable { selectedSlotIndex = index }
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = if (isChosen) Color.White else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Add Slot Button
                        Button(
                            onClick = {
                                val current = detectedSlots.toMutableList()
                                val newIndex = current.size
                                val topOffset = (newIndex * 0.22f).coerceAtMost(0.7f)
                                current.add(
                                    TransparentSlot(0.1f, topOffset, 0.9f, (topOffset + 0.22f).coerceAtMost(0.95f))
                                )
                                detectedSlots = current
                                selectedSlotIndex = newIndex
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242240)),
                            border = BorderStroke(1.dp, Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ + Slot",
                                color = Color(0xFFA5B4FC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // --- ATUR POSISI & UKURAN LUBANG (SLOT CONTROLLER) ---
                    if (selectedSlotIndex in detectedSlots.indices) {
                        val currentSlot = detectedSlots[selectedSlotIndex]
                        val slotWidthPercent = ((currentSlot.right - currentSlot.left) * 100).toInt()
                        val slotHeightPercent = ((currentSlot.bottom - currentSlot.top) * 100).toInt()
                        val posXPercent = (((currentSlot.left + currentSlot.right) / 2f) * 100).toInt()
                        val posYPercent = (((currentSlot.top + currentSlot.bottom) / 2f) * 100).toInt()

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E1E2C),
                            border = BorderStroke(1.dp, Color(0xFF3F3B6E))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Section Header & Action Buttons
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
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color(0xFFEC4899),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Atur Posisi & Ukuran Slot #${selectedSlotIndex + 1}",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Delete slot button if more than 1 slot
                                    if (detectedSlots.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val list = detectedSlots.toMutableList()
                                                list.removeAt(selectedSlotIndex)
                                                detectedSlots = list
                                                selectedSlotIndex = 0.coerceAtMost(list.size - 1)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Slot",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Quick Nudge D-Pad & Size Adjuster Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Directional Position Nudge Buttons
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Up button
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF2A284A),
                                            border = BorderStroke(1.dp, Color(0xFF4C4782)),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { nudgeSelectedSlot(0f, -0.02f) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Geser Atas",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Left button
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF2A284A),
                                                border = BorderStroke(1.dp, Color(0xFF4C4782)),
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable { nudgeSelectedSlot(-0.02f, 0f) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowLeft,
                                                        contentDescription = "Geser Kiri",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            // Center indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(Color(0xFF1E1E2A), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("POS", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Right button
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF2A284A),
                                                border = BorderStroke(1.dp, Color(0xFF4C4782)),
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable { nudgeSelectedSlot(0.02f, 0f) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowRight,
                                                        contentDescription = "Geser Kanan",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Down button
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF2A284A),
                                            border = BorderStroke(1.dp, Color(0xFF4C4782)),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { nudgeSelectedSlot(0f, 0.02f) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Geser Bawah",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Quick Size Adjust Buttons
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        // Width Adjust Buttons
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Lebar:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF242240),
                                                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                                modifier = Modifier.clickable { resizeSelectedSlot(-0.03f, 0f) }
                                            ) {
                                                Text("-", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                            }
                                            Text("$slotWidthPercent%", color = Color(0xFF06B6D4), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF242240),
                                                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                                modifier = Modifier.clickable { resizeSelectedSlot(0.03f, 0f) }
                                            ) {
                                                Text("+", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                            }
                                        }

                                        // Height Adjust Buttons
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Tinggi:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF242240),
                                                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                                modifier = Modifier.clickable { resizeSelectedSlot(0f, -0.03f) }
                                            ) {
                                                Text("-", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                            }
                                            Text("$slotHeightPercent%", color = Color(0xFFEC4899), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF242240),
                                                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                                modifier = Modifier.clickable { resizeSelectedSlot(0f, 0.03f) }
                                            ) {
                                                Text("+", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                            }
                                        }
                                    }
                                }

                                // Sliders: Posisi X / Y & Lebar / Tinggi
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Posisi Horizontal X Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Posisi Horizontal (X)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text("$posXPercent%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = currentSlot.left,
                                        onValueChange = { newLeft ->
                                            val w = currentSlot.right - currentSlot.left
                                            updateSelectedSlot(
                                                currentSlot.copy(
                                                    left = newLeft,
                                                    right = (newLeft + w).coerceAtMost(1f)
                                                )
                                            )
                                        },
                                        valueRange = 0f..((1f - (currentSlot.right - currentSlot.left)).coerceAtLeast(0.01f)),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF06B6D4),
                                            activeTrackColor = Color(0xFF06B6D4),
                                            inactiveTrackColor = Color(0xFF2A2A3A)
                                        )
                                    )

                                    // Posisi Vertikal Y Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Posisi Vertikal (Y)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text("$posYPercent%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = currentSlot.top,
                                        onValueChange = { newTop ->
                                            val h = currentSlot.bottom - currentSlot.top
                                            updateSelectedSlot(
                                                currentSlot.copy(
                                                    top = newTop,
                                                    bottom = (newTop + h).coerceAtMost(1f)
                                                )
                                            )
                                        },
                                        valueRange = 0f..((1f - (currentSlot.bottom - currentSlot.top)).coerceAtLeast(0.01f)),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFEC4899),
                                            activeTrackColor = Color(0xFFEC4899),
                                            inactiveTrackColor = Color(0xFF2A2A3A)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // --- TOGGLE SWITCH: SIMULASIKAN FOTO DI KOTAK TRANSPARAN ---
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E1E2A),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Simulasikan Foto Di Kotak Transparan",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Switch(
                                checked = simulatePhotos,
                                onCheckedChange = { simulatePhotos = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFEC4899),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF333346)
                                )
                            )
                        }
                    }

                    // --- PILIHAN JUMLAH SLOT FOTO ---
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PILIHAN JUMLAH SLOT FOTO:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            // Deteksi Ulang Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E1E2A),
                                border = BorderStroke(1.dp, Color(0xFF06B6D4)),
                                modifier = Modifier.clickable { runAutoDetect() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Deteksi Ulang",
                                        color = Color(0xFF06B6D4),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Presets Row (Auto (3), 1 Foto, 2 Foto, 4 Grid, 6 Grid)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presetList = listOf(
                                "Auto" to "Auto (${detectedSlots.size})",
                                "1 Foto" to "1 Foto",
                                "2 Foto" to "2 Foto",
                                "4 Grid" to "4 Grid",
                                "6 Grid" to "6 Grid"
                            )

                            presetList.forEach { (modeKey, label) ->
                                val isSelected = (selectedPresetMode == modeKey)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF1E1E2A),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFC084FC) else Color(0x22FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { applyPresetLayout(modeKey) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- KELENGKUNGAN SUDUT FOTO (CORNER RADIUS SLIDER) ---
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kelengkungan Sudut Foto",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${cornerRadiusDp.toInt()} dp",
                                color = Color(0xFFEC4899),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = cornerRadiusDp,
                            onValueChange = { cornerRadiusDp = it },
                            valueRange = 0f..32f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEC4899),
                                activeTrackColor = Color(0xFF8B5CF6),
                                inactiveTrackColor = Color(0xFF2A2A3A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // --- NAMA TEMPLATE FRAME PNG ---
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Template Frame PNG") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color(0xFF2E2E3E),
                            focusedLabelColor = Color(0xFFEC4899),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            cursorColor = Color(0xFFEC4899)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // --- BOTTOM ACTION BUTTONS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Batal Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1E1E2A)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF333346))
                        ) {
                            Text(
                                text = "Batal",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Simpan & Gunakan Button (Gradient Pink/Purple)
                        Button(
                            onClick = {
                                onSaveLayout(title, category, badgeLabel, detectedSlots)
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                    )
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Simpan & Gunakan",
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
