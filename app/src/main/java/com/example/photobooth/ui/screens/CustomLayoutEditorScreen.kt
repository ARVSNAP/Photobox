package com.example.photobooth.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.photobooth.utils.CustomLayoutStorage
import com.example.photobooth.utils.TransparentSlot
import java.io.InputStream
import java.util.UUID
import kotlin.math.abs

class EditorSlot(
    val id: Int,
    initialLeft: Float,
    initialTop: Float,
    initialRight: Float,
    initialBottom: Float,
    val color: Color
) {
    var left by mutableFloatStateOf(initialLeft)
    var top by mutableFloatStateOf(initialTop)
    var right by mutableFloatStateOf(initialRight)
    var bottom by mutableFloatStateOf(initialBottom)
}

class OverlayImageLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Image",
    val uri: Uri,
    initialLeft: Float = 0.05f,
    initialTop: Float = 0.05f,
    initialRight: Float = 0.95f,
    initialBottom: Float = 0.95f,
    initialRotation: Float = 0f
) {
    var left by mutableFloatStateOf(initialLeft)
    var top by mutableFloatStateOf(initialTop)
    var right by mutableFloatStateOf(initialRight)
    var bottom by mutableFloatStateOf(initialBottom)
    var rotation by mutableFloatStateOf(initialRotation)
}

class TextLayer(
    val id: String = UUID.randomUUID().toString(),
    initialText: String,
    initialFontPresetIndex: Int = 0,
    initialColor: Color = Color.White,
    initialLeft: Float = 0.2f,
    initialTop: Float = 0.82f,
    initialRight: Float = 0.8f,
    initialBottom: Float = 0.90f
) {
    var text by mutableStateOf(initialText)
    var fontPresetIndex by mutableIntStateOf(initialFontPresetIndex)
    var color by mutableStateOf(initialColor)
    var left by mutableFloatStateOf(initialLeft)
    var top by mutableFloatStateOf(initialTop)
    var right by mutableFloatStateOf(initialRight)
    var bottom by mutableFloatStateOf(initialBottom)
}

class StickerLayer(
    val id: String = UUID.randomUUID().toString(),
    val emojiOrIcon: String,
    initialLeft: Float = 0.4f,
    initialTop: Float = 0.4f,
    initialRight: Float = 0.6f,
    initialBottom: Float = 0.6f
) {
    var left by mutableFloatStateOf(initialLeft)
    var top by mutableFloatStateOf(initialTop)
    var right by mutableFloatStateOf(initialRight)
    var bottom by mutableFloatStateOf(initialBottom)
}

sealed class UnifiedLayer(open val layerId: String, open val displayName: String) {
    data class FrameLayer(val slot: EditorSlot) : UnifiedLayer("frame_${slot.id}", "Frame - ${slot.id}")
    data class ImageLayer(val image: OverlayImageLayer, val indexName: String) : UnifiedLayer("image_${image.id}", indexName)
    data class StickerLayerItem(val sticker: StickerLayer, val indexName: String) : UnifiedLayer("sticker_${sticker.id}", indexName)
    data class TextLayerItem(val text: TextLayer, val indexName: String) : UnifiedLayer("text_${text.id}", indexName)
}

enum class EditorTab {
    BINGKAI, LATAR, GAMBAR, STIKER, TEKS, KODE_QR
}

data class FontPreset(
    val id: Int,
    val name: String,
    val fontFamily: FontFamily,
    val fontWeight: FontWeight,
    val fontStyle: FontStyle = FontStyle.Normal
)

val fontPresets = listOf(
    FontPreset(0, "Sans", FontFamily.Default, FontWeight.Normal),
    FontPreset(1, "Sans Bold", FontFamily.SansSerif, FontWeight.Black),
    FontPreset(2, "Serif", FontFamily.Serif, FontWeight.Normal),
    FontPreset(3, "Serif Bold", FontFamily.Serif, FontWeight.Bold),
    FontPreset(4, "Cursive", FontFamily.Cursive, FontWeight.Normal),
    FontPreset(5, "Monospace", FontFamily.Monospace, FontWeight.Normal),
    FontPreset(6, "Mono Bold", FontFamily.Monospace, FontWeight.Bold),
    FontPreset(7, "Light Sans", FontFamily.SansSerif, FontWeight.Light),
    FontPreset(8, "Medium Sans", FontFamily.SansSerif, FontWeight.Medium),
    FontPreset(9, "Italic Sans", FontFamily.SansSerif, FontWeight.Bold, FontStyle.Italic),
    FontPreset(10, "Cursive Bold", FontFamily.Cursive, FontWeight.Bold),
    FontPreset(11, "Serif Italic", FontFamily.Serif, FontWeight.Normal, FontStyle.Italic),
    FontPreset(12, "Mono Italic", FontFamily.Monospace, FontWeight.Normal, FontStyle.Italic),
    FontPreset(13, "Spaced Caps", FontFamily.SansSerif, FontWeight.ExtraBold),
    FontPreset(14, "Rounded", FontFamily.Default, FontWeight.Bold)
)

data class StickerItem(
    val id: String,
    val name: String,
    val category: String,
    val emojiOrIcon: String
)

val stickerCategories = listOf("Birthday", "Christmas", "Loves", "Wedding", "Numbers", "Party", "Cute")

val sampleStickers = listOf(
    StickerItem("b1", "Happy Birthday", "Birthday", "🎉"),
    StickerItem("b2", "Balloons", "Birthday", "🎈"),
    StickerItem("b3", "Birthday Cake", "Birthday", "🎂"),
    StickerItem("b4", "Party Hat", "Birthday", "🥳"),
    StickerItem("b5", "Gift Box", "Birthday", "🎁"),
    StickerItem("b6", "Confetti", "Birthday", "🎊"),
    StickerItem("b7", "Fireworks", "Birthday", "🎆"),
    StickerItem("b8", "Party Banner", "Birthday", "🎏"),
    StickerItem("c1", "Tree", "Christmas", "🎄"),
    StickerItem("c2", "Santa", "Christmas", "🎅"),
    StickerItem("c3", "Snowflake", "Christmas", "❄️"),
    StickerItem("l1", "Heart", "Loves", "💖"),
    StickerItem("l2", "Love Letter", "Loves", "💌"),
    StickerItem("w1", "Crown", "Wedding", "👑"),
    StickerItem("w2", "Champagne", "Wedding", "🥂"),
    StickerItem("n1", "1", "Numbers", "1️⃣"),
    StickerItem("n2", "2", "Numbers", "2️⃣"),
    StickerItem("p1", "Popper", "Party", "🎉"),
    StickerItem("u1", "Cat", "Cute", "🐱")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLayoutEditorScreen(
    paperWidthPx: Int = 2480,
    paperHeightPx: Int = 3508,
    presetName: String = "A4",
    onBackClicked: () -> Unit,
    onLayoutSaved: (layoutId: String) -> Unit
) {
    val context = LocalContext.current

    var layoutTitle by remember { mutableStateOf("Template Custom $presetName") }

    // Active bottom sheet tab & layer sheet
    var activeSheetTab by remember { mutableStateOf<EditorTab?>(EditorTab.BINGKAI) }
    var showLayersSheet by remember { mutableStateOf(false) }

    // Sub-tabs
    var activeLatarSubTab by remember { mutableStateOf("Warna") }
    var activeStickerCategory by remember { mutableStateOf("Birthday") }

    // Text sheet input
    var newTextInput by remember { mutableStateOf("") }

    // Background color & image
    var backgroundColor by remember { mutableStateOf(Color.White) }
    var backgroundImageUri by remember { mutableStateOf<Uri?>(null) }

    // Zoom & Pan Canvas States (PERBESAR / PERKECIL)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Save success alert dialog state
    var showSaveSuccessDialog by remember { mutableStateOf(false) }
    var savedLayoutId by remember { mutableStateOf<String?>(null) }

    // Slot Colors
    val slotColors = remember {
        listOf(
            Color(0xFF537895), // 1 Blue Slate
            Color(0xFF6E6390), // 2 Purple Slate
            Color(0xFF906575), // 3 Pink Slate
            Color(0xFF4A7C59), // 4 Green Slate
            Color(0xFF9E8357), // 5 Sand Slate
            Color(0xFF707070)  // 6 Gray Slate
        )
    }

    // UNIFIED LAYERS STACK (Rendered bottom to top, i.e., index 0 is at back)
    val layersList = remember {
        mutableStateListOf<UnifiedLayer>(
            UnifiedLayer.FrameLayer(EditorSlot(1, 0.06f, 0.04f, 0.94f, 0.32f, slotColors[0])),
            UnifiedLayer.FrameLayer(EditorSlot(2, 0.06f, 0.35f, 0.94f, 0.63f, slotColors[1])),
            UnifiedLayer.FrameLayer(EditorSlot(3, 0.06f, 0.66f, 0.94f, 0.94f, slotColors[2]))
        )
    }

    // Selected Layer ID
    var selectedLayerId by remember { mutableStateOf<String?>(layersList.firstOrNull()?.layerId) }

    var cornerRadiusDp by remember { mutableFloatStateOf(8f) }
    var outerMarginPct by remember { mutableFloatStateOf(0.06f) }

    val paperAspect = paperWidthPx.toFloat() / paperHeightPx.toFloat()

    // Activity result launchers for image selection
    val bgImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            backgroundImageUri = uri
        }
    }

    // Overlay image picker (adds an image layer on canvas)
    val overlayImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val imgCount = layersList.count { it is UnifiedLayer.ImageLayer } + 1
            val newImg = OverlayImageLayer(
                uri = uri,
                initialLeft = 0.05f,
                initialTop = 0.05f,
                initialRight = 0.95f,
                initialBottom = 0.95f
            )
            val newLayer = UnifiedLayer.ImageLayer(newImg, "Image $imgCount")
            layersList.add(newLayer)
            selectedLayerId = newLayer.layerId
            activeSheetTab = null
        }
    }

    // Add new frame slot
    fun addNewFrameSlot() {
        val existingFrames = layersList.filterIsInstance<UnifiedLayer.FrameLayer>()
        val nextId = existingFrames.size + 1
        val margin = outerMarginPct
        val newSlot = EditorSlot(
            id = nextId,
            initialLeft = margin,
            initialTop = 0.70f,
            initialRight = 1.0f - margin,
            initialBottom = 0.94f,
            color = slotColors[(nextId - 1) % slotColors.size]
        )
        val newLayer = UnifiedLayer.FrameLayer(newSlot)
        layersList.add(newLayer)
        selectedLayerId = newLayer.layerId
    }

    // Save full bitmap layout to storage
    fun saveLayoutToStorage() {
        val targetW = paperWidthPx
        val targetH = paperHeightPx

        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Render Background: Uri image or solid color
        if (backgroundImageUri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(backgroundImageUri!!)
                val bgBitmap = BitmapFactory.decodeStream(inputStream)
                if (bgBitmap != null) {
                    val srcRect = android.graphics.Rect(0, 0, bgBitmap.width, bgBitmap.height)
                    val dstRect = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
                    canvas.drawBitmap(bgBitmap, srcRect, dstRect, null)
                    bgBitmap.recycle()
                } else {
                    canvas.drawColor(backgroundColor.toArgb())
                }
            } catch (e: Exception) {
                canvas.drawColor(backgroundColor.toArgb())
            }
        } else {
            canvas.drawColor(backgroundColor.toArgb())
        }

        val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        val transparentSlotsList = mutableListOf<TransparentSlot>()

        // Draw layers in z-index order
        for (layer in layersList) {
            when (layer) {
                is UnifiedLayer.FrameLayer -> {
                    val s = layer.slot
                    val leftPx = s.left * targetW
                    val topPx = s.top * targetH
                    val rightPx = s.right * targetW
                    val bottomPx = s.bottom * targetH
                    val cornerPx = cornerRadiusDp * (targetW / 400f)

                    canvas.drawRoundRect(
                        RectF(leftPx, topPx, rightPx, bottomPx),
                        cornerPx,
                        cornerPx,
                        clearPaint
                    )

                    transparentSlotsList.add(
                        TransparentSlot(
                            left = s.left,
                            top = s.top,
                            right = s.right,
                            bottom = s.bottom
                        )
                    )
                }
                is UnifiedLayer.ImageLayer -> {
                    val img = layer.image
                    try {
                        val stream: InputStream? = context.contentResolver.openInputStream(img.uri)
                        val imgBmp = BitmapFactory.decodeStream(stream)
                        if (imgBmp != null) {
                            val dstRect = RectF(
                                img.left * targetW,
                                img.top * targetH,
                                img.right * targetW,
                                img.bottom * targetH
                            )
                            if (img.rotation != 0f) {
                                canvas.save()
                                canvas.rotate(img.rotation, dstRect.centerX(), dstRect.centerY())
                                canvas.drawBitmap(imgBmp, null, dstRect, null)
                                canvas.restore()
                            } else {
                                canvas.drawBitmap(imgBmp, null, dstRect, null)
                            }
                            imgBmp.recycle()
                        }
                    } catch (e: Exception) {
                        // ignore error
                    }
                }
                is UnifiedLayer.StickerLayerItem -> {
                    val st = layer.sticker
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = targetW * 0.08f
                        textAlign = Paint.Align.CENTER
                    }
                    val xPx = ((st.left + st.right) / 2f) * targetW
                    val yPx = ((st.top + st.bottom) / 2f) * targetH
                    canvas.drawText(st.emojiOrIcon, xPx, yPx, textPaint)
                }
                is UnifiedLayer.TextLayerItem -> {
                    val tLayer = layer.text
                    val preset = fontPresets.getOrElse(tLayer.fontPresetIndex) { fontPresets[0] }
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = tLayer.color.toArgb()
                        textSize = targetW * 0.05f
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = preset.fontWeight == FontWeight.Bold || preset.fontWeight == FontWeight.Black
                    }
                    val xPx = ((tLayer.left + tLayer.right) / 2f) * targetW
                    val yPx = ((tLayer.top + tLayer.bottom) / 2f) * targetH
                    canvas.drawText(tLayer.text, xPx, yPx, textPaint)
                }
            }
        }

        val saved = CustomLayoutStorage.saveCustomLayoutBitmap(
            context = context,
            title = layoutTitle,
            bitmap = bitmap,
            category = "Layout Kustom",
            badgeLabel = "👑 Custom",
            explicitSlots = transparentSlotsList
        )

        bitmap.recycle()

        if (saved != null) {
            savedLayoutId = saved.id
            showSaveSuccessDialog = true
        } else {
            onBackClicked()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Editor Layout",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "$presetName (${paperWidthPx}x${paperHeightPx}px)",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { saveLayoutToStorage() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Simpan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121118))
            )
        },
        containerColor = Color(0xFF0C0B0E)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // CENTRAL CANVAS WORKSPACE WITH PINCH-TO-ZOOM & PAN SUPPORT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0C0B0E))
                        .clip(RoundedCornerShape(0.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newZoom = (zoomScale * zoom).coerceIn(0.6f, 3.5f)
                                zoomScale = newZoom
                                if (newZoom > 1.0f) {
                                    panOffsetX += pan.x
                                    panOffsetY += pan.y
                                } else {
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxHeight(0.94f)
                            .aspectRatio(paperAspect.coerceIn(0.25f, 3.0f))
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = panOffsetX,
                                translationY = panOffsetY
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(backgroundColor)
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        val canvasW = maxWidth
                        val canvasH = maxHeight

                        // Render Background Image if set
                        if (backgroundImageUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(backgroundImageUri)
                                    .crossfade(false)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "Latar Gambar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Determine selected layer bounds & center for snap lines
                        var isXCentered by remember { mutableStateOf(false) }
                        var isYCentered by remember { mutableStateOf(false) }

                        // RENDER ALL LAYERS IN STACK ORDER (index 0 at bottom)
                        for (layer in layersList) {
                            val isSelected = layer.layerId == selectedLayerId

                            when (layer) {
                                is UnifiedLayer.FrameLayer -> {
                                    val slot = layer.slot
                                    val slotLeftDp = canvasW * slot.left
                                    val slotTopDp = canvasH * slot.top
                                    val slotWidthDp = canvasW * (slot.right - slot.left)
                                    val slotHeightDp = canvasH * (slot.bottom - slot.top)

                                    val centerX = (slot.left + slot.right) / 2f
                                    val centerY = (slot.top + slot.bottom) / 2f

                                    if (isSelected) {
                                        isXCentered = abs(centerX - 0.5f) < 0.02f
                                        isYCentered = abs(centerY - 0.5f) < 0.02f
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset(x = slotLeftDp, y = slotTopDp)
                                            .size(width = slotWidthDp, height = slotHeightDp)
                                            .clip(RoundedCornerShape(cornerRadiusDp.dp))
                                            .background(slot.color)
                                            .clickable { selectedLayerId = layer.layerId }
                                            .pointerInput(layer.layerId) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val deltaX = (dragAmount.x / zoomScale) / canvasW.toPx()
                                                    val deltaY = (dragAmount.y / zoomScale) / canvasH.toPx()
                                                    val w = slot.right - slot.left
                                                    val h = slot.bottom - slot.top

                                                    var newLeft = (slot.left + deltaX).coerceIn(0f, 1f - w)
                                                    var newTop = (slot.top + deltaY).coerceIn(0f, 1f - h)

                                                    if (abs((newLeft + w / 2f) - 0.5f) < 0.02f) {
                                                        newLeft = 0.5f - w / 2f
                                                    }
                                                    if (abs((newTop + h / 2f) - 0.5f) < 0.02f) {
                                                        newTop = 0.5f - h / 2f
                                                    }

                                                    slot.left = newLeft
                                                    slot.top = newTop
                                                    slot.right = newLeft + w
                                                    slot.bottom = newTop + h
                                                    selectedLayerId = layer.layerId
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${slot.id}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (slotWidthDp.value * 0.35f).coerceIn(16f, 48f).sp
                                        )
                                    }
                                }

                                is UnifiedLayer.ImageLayer -> {
                                    val img = layer.image
                                    val imgLeftDp = canvasW * img.left
                                    val imgTopDp = canvasH * img.top
                                    val imgWidthDp = canvasW * (img.right - img.left)
                                    val imgHeightDp = canvasH * (img.bottom - img.top)

                                    val centerX = (img.left + img.right) / 2f
                                    val centerY = (img.top + img.bottom) / 2f

                                    if (isSelected) {
                                        isXCentered = abs(centerX - 0.5f) < 0.02f
                                        isYCentered = abs(centerY - 0.5f) < 0.02f
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset(x = imgLeftDp, y = imgTopDp)
                                            .size(width = imgWidthDp, height = imgHeightDp)
                                            .rotate(img.rotation)
                                            .clickable { selectedLayerId = layer.layerId }
                                            .pointerInput(layer.layerId) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val deltaX = (dragAmount.x / zoomScale) / canvasW.toPx()
                                                    val deltaY = (dragAmount.y / zoomScale) / canvasH.toPx()
                                                    val w = img.right - img.left
                                                    val h = img.bottom - img.top

                                                    var newLeft = (img.left + deltaX).coerceIn(-0.3f, 1f)
                                                    var newTop = (img.top + deltaY).coerceIn(-0.3f, 1f)

                                                    if (abs((newLeft + w / 2f) - 0.5f) < 0.02f) {
                                                        newLeft = 0.5f - w / 2f
                                                    }
                                                    if (abs((newTop + h / 2f) - 0.5f) < 0.02f) {
                                                        newTop = 0.5f - h / 2f
                                                    }

                                                    img.left = newLeft
                                                    img.top = newTop
                                                    img.right = newLeft + w
                                                    img.bottom = newTop + h
                                                    selectedLayerId = layer.layerId
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(img.uri)
                                                .crossfade(false)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = layer.displayName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.FillBounds
                                        )
                                    }
                                }

                                is UnifiedLayer.StickerLayerItem -> {
                                    val st = layer.sticker
                                    val stLeftDp = canvasW * st.left
                                    val stTopDp = canvasH * st.top
                                    val stWidthDp = canvasW * (st.right - st.left)
                                    val stHeightDp = canvasH * (st.bottom - st.top)

                                    Box(
                                        modifier = Modifier
                                            .offset(x = stLeftDp, y = stTopDp)
                                            .size(width = stWidthDp, height = stHeightDp)
                                            .clickable { selectedLayerId = layer.layerId }
                                            .pointerInput(layer.layerId) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val deltaX = (dragAmount.x / zoomScale) / canvasW.toPx()
                                                    val deltaY = (dragAmount.y / zoomScale) / canvasH.toPx()
                                                    val w = st.right - st.left
                                                    val h = st.bottom - st.top
                                                    val newLeft = (st.left + deltaX).coerceIn(0f, 1f - w)
                                                    val newTop = (st.top + deltaY).coerceIn(0f, 1f - h)
                                                    st.left = newLeft
                                                    st.top = newTop
                                                    st.right = newLeft + w
                                                    st.bottom = newTop + h
                                                    selectedLayerId = layer.layerId
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = st.emojiOrIcon, fontSize = 32.sp)
                                    }
                                }

                                is UnifiedLayer.TextLayerItem -> {
                                    val tLayer = layer.text
                                    val preset = fontPresets.getOrElse(tLayer.fontPresetIndex) { fontPresets[0] }
                                    val tLeftDp = canvasW * tLayer.left
                                    val tTopDp = canvasH * tLayer.top
                                    val tWidthDp = canvasW * (tLayer.right - tLayer.left)
                                    val tHeightDp = canvasH * (tLayer.bottom - tLayer.top)

                                    Box(
                                        modifier = Modifier
                                            .offset(x = tLeftDp, y = tTopDp)
                                            .size(width = tWidthDp, height = tHeightDp)
                                            .clickable { selectedLayerId = layer.layerId }
                                            .pointerInput(layer.layerId) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val deltaX = (dragAmount.x / zoomScale) / canvasW.toPx()
                                                    val deltaY = (dragAmount.y / zoomScale) / canvasH.toPx()
                                                    val w = tLayer.right - tLayer.left
                                                    val h = tLayer.bottom - tLayer.top
                                                    val newLeft = (tLayer.left + deltaX).coerceIn(0f, 1f - w)
                                                    val newTop = (tLayer.top + deltaY).coerceIn(0f, 1f - h)
                                                    tLayer.left = newLeft
                                                    tLayer.top = newTop
                                                    tLayer.right = newLeft + w
                                                    tLayer.bottom = newTop + h
                                                    selectedLayerId = layer.layerId
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tLayer.text,
                                            color = tLayer.color,
                                            fontFamily = preset.fontFamily,
                                            fontWeight = preset.fontWeight,
                                            fontStyle = preset.fontStyle,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // CYAN ALIGNMENT SNAP LINES
                        if (isXCentered) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(2.dp)
                                    .align(Alignment.Center)
                                    .background(Color(0xFF00E5FF))
                            )
                        }

                        if (isYCentered) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .align(Alignment.Center)
                                    .background(Color(0xFF00E5FF))
                            )
                        }

                        // RENDER INTERACTIVE SELECTION BOUNDING BOX & CORNER HANDLES
                        val selectedLayer = layersList.find { it.layerId == selectedLayerId }
                        if (selectedLayer != null) {
                            val bounds = when (selectedLayer) {
                                is UnifiedLayer.FrameLayer -> Triple(selectedLayer.slot.left, selectedLayer.slot.top, selectedLayer.slot.right to selectedLayer.slot.bottom)
                                is UnifiedLayer.ImageLayer -> Triple(selectedLayer.image.left, selectedLayer.image.top, selectedLayer.image.right to selectedLayer.image.bottom)
                                is UnifiedLayer.StickerLayerItem -> Triple(selectedLayer.sticker.left, selectedLayer.sticker.top, selectedLayer.sticker.right to selectedLayer.sticker.bottom)
                                is UnifiedLayer.TextLayerItem -> Triple(selectedLayer.text.left, selectedLayer.text.top, selectedLayer.text.right to selectedLayer.text.bottom)
                            }

                            val leftDp = canvasW * bounds.first
                            val topDp = canvasH * bounds.second
                            val widthDp = canvasW * (bounds.third.first - bounds.first)
                            val heightDp = canvasH * (bounds.third.second - bounds.second)

                            Box(
                                modifier = Modifier
                                    .offset(x = leftDp, y = topDp)
                                    .size(width = widthDp, height = heightDp)
                                    .border(2.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                            ) {
                                // Corner 1 (Top-Left): Edit / Pen icon (Blue)
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-12).dp, y = (-12).dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6))
                                        .clickable {
                                            when (selectedLayer) {
                                                is UnifiedLayer.FrameLayer -> activeSheetTab = EditorTab.BINGKAI
                                                is UnifiedLayer.TextLayerItem -> activeSheetTab = EditorTab.TEKS
                                                else -> {}
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Corner 2 (Top-Right): Delete / Close icon (Red)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 12.dp, y = (-12).dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .clickable {
                                            layersList.remove(selectedLayer)
                                            selectedLayerId = layersList.lastOrNull()?.layerId
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Corner 3 (Bottom-Left): Resize / Expand handle (Blue)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .offset(x = (-12).dp, y = 12.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6))
                                        .pointerInput(selectedLayer.layerId) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaX = (dragAmount.x / zoomScale) / canvasW.toPx()
                                                val deltaY = (dragAmount.y / zoomScale) / canvasH.toPx()
                                                when (selectedLayer) {
                                                    is UnifiedLayer.FrameLayer -> {
                                                        val s = selectedLayer.slot
                                                        s.left = (s.left + deltaX).coerceIn(0f, s.right - 0.05f)
                                                        s.bottom = (s.bottom + deltaY).coerceIn(s.top + 0.05f, 1f)
                                                    }
                                                    is UnifiedLayer.ImageLayer -> {
                                                        val img = selectedLayer.image
                                                        img.left = (img.left + deltaX).coerceIn(-0.3f, img.right - 0.05f)
                                                        img.bottom = (img.bottom + deltaY).coerceIn(img.top + 0.05f, 1.3f)
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInFull,
                                        contentDescription = "Ubah Ukuran",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Corner 4 (Bottom-Right): Rotate / Refresh handle (Yellow/Orange)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 12.dp, y = 12.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                        .clickable {
                                            if (selectedLayer is UnifiedLayer.ImageLayer) {
                                                selectedLayer.image.rotation = (selectedLayer.image.rotation + 90f) % 360f
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Putar",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // FLOATING ZOOM & PAN CONTROLS (PERBESAR / PERKECIL)
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Zoom Out (-)
                            IconButton(
                                onClick = {
                                    val next = (zoomScale - 0.25f).coerceAtLeast(0.6f)
                                    zoomScale = next
                                    if (next <= 1.0f) {
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Perkecil",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .clickable {
                                        // Reset zoom to 100%
                                        zoomScale = 1.0f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                            )

                            // Zoom In (+)
                            IconButton(
                                onClick = {
                                    zoomScale = (zoomScale + 0.25f).coerceAtMost(3.5f)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Perbesar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // FLOATING ACTION OVERLAY (Flag Left, Layers Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { /* Toggle options */ },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showLayersSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Lapisan",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // BOTTOM FIXED TOOLBAR NAVIGATION
                Surface(
                    color = Color(0xFF16151B),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val navTabs = listOf(
                            Pair(EditorTab.BINGKAI, "Bingkai" to Icons.Default.GridOn),
                            Pair(EditorTab.LATAR, "Latar" to Icons.Default.Palette),
                            Pair(EditorTab.GAMBAR, "Gambar" to Icons.Default.Image),
                            Pair(EditorTab.STIKER, "Stiker" to Icons.Default.SentimentSatisfiedAlt),
                            Pair(EditorTab.TEKS, "Teks" to Icons.Default.Title),
                            Pair(EditorTab.KODE_QR, "Kode QR" to Icons.Default.QrCode2)
                        )

                        for ((tab, data) in navTabs) {
                            val isSelected = activeSheetTab == tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        activeSheetTab = if (isSelected) null else tab
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = data.second,
                                    contentDescription = data.first,
                                    tint = if (isSelected) Color(0xFFA855F7) else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = data.first,
                                    color = if (isSelected) Color(0xFFA855F7) else Color(0xFF9CA3AF),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // MODAL BOTTOM SHEET OVERLAY FOR ACTIVE EDITOR TAB
            if (activeSheetTab != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color(0xFF1D1B22),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        when (activeSheetTab) {
                            EditorTab.BINGKAI -> {
                                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Select Frame",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        IconButton(
                                            onClick = { activeSheetTab = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Tutup",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Surface(
                                        color = Color(0xFF26242E),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(110.dp)
                                            .clickable { addNewFrameSlot() }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.size(42.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Rectangle",
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            EditorTab.LATAR -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Latar",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    IconButton(
                                        onClick = { activeSheetTab = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Tutup",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .clickable { activeLatarSubTab = "Warna" }
                                            .padding(end = 24.dp, bottom = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = null,
                                                tint = if (activeLatarSubTab == "Warna") Color.White else Color(0xFF9CA3AF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Warna",
                                                color = if (activeLatarSubTab == "Warna") Color.White else Color(0xFF9CA3AF),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (activeLatarSubTab == "Warna") {
                                            Box(
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(2.5.dp)
                                                    .background(Color(0xFF8B5CF6))
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .clickable { activeLatarSubTab = "Gambar" }
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (activeLatarSubTab == "Gambar") Color.White else Color(0xFF9CA3AF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Gambar",
                                                color = if (activeLatarSubTab == "Gambar") Color.White else Color(0xFF9CA3AF),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (activeLatarSubTab == "Gambar") {
                                            Box(
                                                modifier = Modifier
                                                    .width(65.dp)
                                                    .height(2.5.dp)
                                                    .background(Color(0xFF8B5CF6))
                                            )
                                        }
                                    }
                                }

                                Divider(color = Color(0x22FFFFFF), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(14.dp))

                                if (activeLatarSubTab == "Warna") {
                                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                        Text(
                                            text = "Preset Colors",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        val presetColors = listOf(
                                            Color.White, Color(0xFF121212), Color(0xFF1E293B),
                                            Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B),
                                            Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
                                            Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF8B5CF6)
                                        )

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(presetColors) { color ->
                                                val isSelected = backgroundColor == color && backgroundImageUri == null
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(color)
                                                        .border(
                                                            width = if (isSelected) 2.5.dp else 1.dp,
                                                            color = if (isSelected) Color(0xFF3B82F6) else Color(0x44FFFFFF),
                                                            shape = RoundedCornerShape(10.dp)
                                                        )
                                                        .clickable {
                                                            backgroundColor = color
                                                            backgroundImageUri = null
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = if (color == Color.White) Color.Black else Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                        Surface(
                                            color = Color(0xFF26242E),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.5.dp, Color(0x33FFFFFF)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clickable { bgImagePickerLauncher.launch("image/*") }
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddPhotoAlternate,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA855F7),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Pilih dari Galeri",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            EditorTab.GAMBAR -> {
                                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Pilih sumber foto",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        IconButton(
                                            onClick = { activeSheetTab = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Tutup",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Pilih foto dari galeri atau telusuri penyimpanan perangkat Anda.",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Surface(
                                        color = Color(0xFF26242E),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { overlayImagePickerLauncher.launch("image/*") }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Color(0xFF3F3A52),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.AddPhotoAlternate,
                                                        contentDescription = null,
                                                        tint = Color(0xFFA855F7),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Pilih dari galeri",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "Pilih foto atau bingkai PNG dari galeri Anda.",
                                                    color = Color(0xFF9CA3AF),
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = Color(0xFF9CA3AF)
                                            )
                                        }
                                    }
                                }
                            }

                            EditorTab.STIKER -> {
                                Column(modifier = Modifier.padding(vertical = 14.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Stiker",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        IconButton(
                                            onClick = { activeSheetTab = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Tutup",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(stickerCategories) { cat ->
                                            val isSelected = activeStickerCategory == cat
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { activeStickerCategory = cat },
                                                label = { Text(cat) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF3B82F6),
                                                    selectedLabelColor = Color.White,
                                                    containerColor = Color(0xFF26242E),
                                                    labelColor = Color(0xFF9CA3AF)
                                                ),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    val filteredStickers = sampleStickers.filter { it.category == activeStickerCategory }
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5),
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.height(180.dp)
                                    ) {
                                        items(filteredStickers) { sticker ->
                                            Surface(
                                                color = Color(0xFF26242E),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clickable {
                                                        val stCount = layersList.count { it is UnifiedLayer.StickerLayerItem } + 1
                                                        val newSticker = StickerLayer(emojiOrIcon = sticker.emojiOrIcon)
                                                        val newLayer = UnifiedLayer.StickerLayerItem(newSticker, "Sticker $stCount")
                                                        layersList.add(newLayer)
                                                        selectedLayerId = newLayer.layerId
                                                        activeSheetTab = null
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(text = sticker.emojiOrIcon, fontSize = 28.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            EditorTab.TEKS -> {
                                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tambah Teks",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    if (newTextInput.isNotBlank()) {
                                                        val txtCount = layersList.count { it is UnifiedLayer.TextLayerItem } + 1
                                                        val newText = TextLayer(initialText = newTextInput)
                                                        val newLayer = UnifiedLayer.TextLayerItem(newText, "Text: ${newTextInput.take(8)}")
                                                        layersList.add(newLayer)
                                                        selectedLayerId = newLayer.layerId
                                                        newTextInput = ""
                                                        activeSheetTab = null
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Tambah Teks",
                                                    tint = Color.White
                                                )
                                            }
                                            IconButton(
                                                onClick = { activeSheetTab = null },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Tutup",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = newTextInput,
                                        onValueChange = { newTextInput = it },
                                        placeholder = { Text("Masukkan teks kamu...", color = Color(0xFF6B7280)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF26242E),
                                            unfocusedContainerColor = Color(0xFF26242E),
                                            focusedBorderColor = Color(0xFFA855F7),
                                            unfocusedBorderColor = Color(0x33FFFFFF),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }

            // MODAL BOTTOM SHEET FOR LAPISAN (LAYERS MANAGER)
            if (showLayersSheet) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color(0xFF1D1B22),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lapisan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            IconButton(onClick = { showLayersSheet = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF26242E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Top = Front • Gunakan panah untuk mengatur urutan lapisan",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val reversedLayers = layersList.asReversed()
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            itemsIndexed(reversedLayers) { indexInReversed, layer ->
                                val realIndex = layersList.size - 1 - indexInReversed
                                val isSelected = layer.layerId == selectedLayerId

                                Surface(
                                    color = if (isSelected) Color(0xFF2A2438) else Color(0xFF26242E),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF10B981) else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLayerId = layer.layerId
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = null,
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))

                                        Surface(
                                            color = when (layer) {
                                                is UnifiedLayer.FrameLayer -> Color(0xFF8B5CF6)
                                                is UnifiedLayer.ImageLayer -> Color(0xFF10B981)
                                                else -> Color(0xFF3B82F6)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when (layer) {
                                                        is UnifiedLayer.FrameLayer -> Icons.Default.GridView
                                                        is UnifiedLayer.ImageLayer -> Icons.Default.Image
                                                        is UnifiedLayer.StickerLayerItem -> Icons.Default.SentimentSatisfiedAlt
                                                        is UnifiedLayer.TextLayerItem -> Icons.Default.Title
                                                    },
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = layer.displayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                if (realIndex < layersList.size - 1) {
                                                    val temp = layersList[realIndex]
                                                    layersList[realIndex] = layersList[realIndex + 1]
                                                    layersList[realIndex + 1] = temp
                                                }
                                            },
                                            enabled = realIndex < layersList.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Naikkan Lapisan",
                                                tint = if (realIndex < layersList.size - 1) Color.White else Color(0x33FFFFFF)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (realIndex > 0) {
                                                    val temp = layersList[realIndex]
                                                    layersList[realIndex] = layersList[realIndex - 1]
                                                    layersList[realIndex - 1] = temp
                                                }
                                            },
                                            enabled = realIndex > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Turunkan Lapisan",
                                                tint = if (realIndex > 0) Color.White else Color(0x33FFFFFF)
                                            )
                                        }

                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SAVE SUCCESS ALERT DIALOG
            if (showSaveSuccessDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showSaveSuccessDialog = false
                        savedLayoutId?.let { onLayoutSaved(it) } ?: onBackClicked()
                    },
                    title = {
                        Text(
                            text = "Template Disimpan",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Templatemu sudah tersimpan di lokal.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSaveSuccessDialog = false
                                savedLayoutId?.let { onLayoutSaved(it) } ?: onBackClicked()
                            }
                        ) {
                            Text(
                                text = "OK",
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    },
                    containerColor = Color(0xFF1F1D2B),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
