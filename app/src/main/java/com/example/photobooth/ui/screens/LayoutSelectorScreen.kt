package com.example.photobooth.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.example.R
import com.example.photobooth.model.LayoutCardCustomization
import com.example.photobooth.ui.components.LayoutCardVisualPreview
import com.example.photobooth.ui.components.LayoutIconEditorDialog
import com.example.photobooth.ui.components.PngLayoutConfigDialog
import com.example.photobooth.utils.CustomLayoutStorage
import com.example.photobooth.utils.LayoutCardCustomizationStorage
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class LayoutOptionItem(
    val id: String,
    val title: String,
    val description: String,
    val photoCount: Int,
    val imageRes: Int,
    val badgeLabel: String = "✓ Free",
    val category: String = "Layout 4x6",
    val customImagePath: String? = null,
    val isPro: Boolean = false
)

@Composable
fun LayoutSelectorScreen(
    onLayoutSelected: (String) -> Unit,
    onBackClicked: () -> Unit = {},
    onMyTemplatesClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    var customLayoutsList by remember {
        mutableStateOf(CustomLayoutStorage.getCustomLayouts(context))
    }

    val defaultLayouts = remember {
        listOf(
            // --- SQUARE CATEGORY ---
            LayoutOptionItem(
                id = "Single Shot",
                title = "SUNDAY NIGHT PARTY",
                description = "1 Full-Res Studio Shot",
                photoCount = 1,
                imageRes = R.drawable.img_layout_single,
                badgeLabel = "✓ Free",
                category = "Square",
                isPro = false
            ),
            LayoutOptionItem(
                id = "Square 1x1",
                title = "Happy Birthday",
                description = "1x1 Square Frame",
                photoCount = 1,
                imageRes = R.drawable.img_layout_grid2x2,
                badgeLabel = "👑 Pro",
                category = "Square",
                isPro = true
            ),
            LayoutOptionItem(
                id = "Square Party 2x2",
                title = "Party Weekend",
                description = "2x2 Square Grid",
                photoCount = 4,
                imageRes = R.drawable.img_layout_grid2x2,
                badgeLabel = "👑 Pro",
                category = "Square",
                isPro = true
            ),

            // --- LAYOUT 2x6 CATEGORY ---
            LayoutOptionItem(
                id = "3-Photo Strip",
                title = "Promise & Love",
                description = "Classic Retro Photostrip",
                photoCount = 3,
                imageRes = R.drawable.img_layout_strip3,
                badgeLabel = "👑 Pro",
                category = "Layout 2x6",
                isPro = true
            ),
            LayoutOptionItem(
                id = "4-Photo Strip",
                title = "SILVER HARMONY",
                description = "4 Story Sequence Strip",
                photoCount = 4,
                imageRes = R.drawable.img_layout_grid4,
                badgeLabel = "👑 Pro",
                category = "Layout 2x6",
                isPro = true
            ),
            LayoutOptionItem(
                id = "Pastel Strip 2x6",
                title = "Forever Yours",
                description = "Soft Pastel Frame",
                photoCount = 3,
                imageRes = R.drawable.img_layout_strip3,
                badgeLabel = "👑 Pro",
                category = "Layout 2x6",
                isPro = true
            ),
            LayoutOptionItem(
                id = "Comic Strip 2x6",
                title = "LOVE STORIES",
                description = "Pop Art Comic Style",
                photoCount = 3,
                imageRes = R.drawable.img_layout_grid4,
                badgeLabel = "👑 Pro",
                category = "Layout 2x6",
                isPro = true
            ),

            // --- LAYOUT 4x6 CATEGORY ---
            LayoutOptionItem(
                id = "2x2 Grid",
                title = "Jose Andrian",
                description = "4 Photos Portrait Collage",
                photoCount = 4,
                imageRes = R.drawable.img_layout_grid2x2,
                badgeLabel = "👑 Pro",
                category = "Layout 4x6",
                isPro = true
            ),
            LayoutOptionItem(
                id = "4x6 Jessica",
                title = "Jessica Johnson",
                description = "4-Photo Balanced Grid",
                photoCount = 4,
                imageRes = R.drawable.img_layout_grid4,
                badgeLabel = "👑 Pro",
                category = "Layout 4x6",
                isPro = true
            ),
            LayoutOptionItem(
                id = "Boothlab 8-Frame",
                title = "Boothlab 8 Box",
                description = "Template Kotak Transparan",
                photoCount = 8,
                imageRes = R.drawable.img_frame_boothlab,
                badgeLabel = "👑 Pro",
                category = "Layout 4x6",
                isPro = true
            ),
            LayoutOptionItem(
                id = "GIF / Boomerang",
                title = "Motion Loop GIF",
                description = "8-Frame Motion Loop",
                photoCount = 8,
                imageRes = R.drawable.img_layout_gif,
                badgeLabel = "👑 Pro",
                category = "Layout 4x6",
                isPro = true
            )
        )
    }

    // Convert saved Custom PNG Layouts into LayoutOptionItem list
    val customPngOptionItems = remember(customLayoutsList) {
        customLayoutsList.map { cl ->
            LayoutOptionItem(
                id = cl.id,
                title = cl.title,
                description = "Layout PNG Kustom",
                photoCount = cl.photoCount,
                imageRes = 0,
                badgeLabel = cl.badgeLabel,
                category = cl.category,
                customImagePath = cl.imagePath,
                isPro = cl.badgeLabel.contains("Pro")
            )
        }
    }

    val allLayouts = remember(defaultLayouts, customPngOptionItems) {
        defaultLayouts + customPngOptionItems
    }

    val coroutineScope = rememberCoroutineScope()

    // Category Filter State
    var selectedCategoryTab by remember { mutableStateOf("Semua") }
    val categoryTabs = listOf("Semua", "Square", "Layout 2x6", "Layout 4x6")

    val filteredLayouts = remember(allLayouts, selectedCategoryTab) {
        if (selectedCategoryTab == "Semua") {
            allLayouts
        } else {
            allLayouts.filter { it.category == selectedCategoryTab }
        }
    }

    val pagerState = rememberPagerState(pageCount = { filteredLayouts.size })

    var editingLayout by remember { mutableStateOf<LayoutOptionItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var customLayoutTitle by remember { mutableStateOf("") }
    var selectedPhotoCount by remember { mutableIntStateOf(4) }
    var selectedCategory by remember { mutableStateOf("Layout PNG Kustom") }
    var selectedBadge by remember { mutableStateOf("👑 Pro") }
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            customLayoutTitle = "Layout PNG ${customLayoutsList.size + 1}"
            selectedPhotoCount = 4
            selectedCategory = "Layout PNG Kustom"
            selectedBadge = "👑 Pro"
            showAddDialog = true
        }
    }

    // Map of persistent customizations
    var customizations by remember {
        mutableStateOf(LayoutCardCustomizationStorage.getAllCustomizations(context, allLayouts.map { it.id }))
    }

    // Currently centered/selected layout item
    val activeLayoutIndex = pagerState.currentPage.coerceIn(0, (filteredLayouts.size - 1).coerceAtLeast(0))
    val currentSelectedLayout = filteredLayouts.getOrNull(activeLayoutIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0A09)) // Deep Studio Dark
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- TOP HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(BorderStroke(1.dp, Color(0x22FFFFFF)), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BINGKAI FOTO",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Pilih template atau bingkai terbaik Anda",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Dual Custom Action Capsule
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF27272A).copy(alpha = 0.5f))
                    .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onMyTemplatesClicked,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD97706).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Template Saya",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color(0x22FFFFFF))
                )

                IconButton(
                    onClick = { imagePickerLauncher.launch("image/png") },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Tambah PNG Kustom",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // --- MODERN CATEGORY SELECTOR TABS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111015))
                .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categoryTabs.forEach { tab ->
                val isTabSelected = selectedCategoryTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isTabSelected) {
                                Modifier.background(
                                    brush = Brush.horizontalGradient(listOf(ElectricViolet, NeonRose)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable {
                            selectedCategoryTab = tab
                            coroutineScope.launch {
                                pagerState.scrollToPage(0)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isTabSelected) Color.White else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 3D COVER FLOW CAROUSEL SLIDER ---
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val availableH = maxHeight
            val availableW = maxWidth
            val sidePadding = (availableW * 0.28f).coerceIn(40.dp, 120.dp)

            if (filteredLayouts.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = sidePadding),
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val layoutItem = filteredLayouts[page]
                    val isCurrentPage = page == pagerState.currentPage

                    // Dynamic responsive card dimensions based on container height
                    val targetCardH = (availableH * 0.72f).coerceIn(160.dp, 330.dp)
                    val (cardW, cardH) = when (layoutItem.category) {
                        "Layout 2x6" -> Pair(targetCardH * 0.52f, targetCardH)
                        "Square" -> Pair(targetCardH * 0.85f, targetCardH * 0.85f)
                        else -> Pair(targetCardH * 0.68f, targetCardH)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Defer pageOffset calculation lazily inside graphicsLayer to avoid recomposition stalls
                                val pageOffset = (
                                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                )

                                // 3D Y-Axis Rotation
                                rotationY = pageOffset * -28f

                                // Scale down side cards smoothly
                                val scale = lerp(
                                    start = 0.72f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                                )
                                scaleX = scale
                                scaleY = scale

                                // Alpha fade for distant cards
                                alpha = lerp(
                                    start = 0.35f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                                )

                                shadowElevation = (1f - pageOffset.absoluteValue.coerceIn(0f, 1f)) * 10f
                            }
                            .clickable {
                                if (!isCurrentPage) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(page)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CoverFlowCardItem(
                            layout = layoutItem,
                            isSelected = isCurrentPage,
                            customization = customizations[layoutItem.id],
                            cardWidth = cardW,
                            cardHeight = cardH,
                            onSelect = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(page)
                                }
                            },
                            onDelete = { showDeleteConfirmDialog = layoutItem.id },
                            onEdit = { editingLayout = layoutItem }
                        )
                    }
                }
            }
        }

        // --- FUTURISTIC FLOATING ACTION BAR ---
        if (currentSelectedLayout != null) {
            Surface(
                color = Color(0xEE12111A),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(ElectricViolet.copy(alpha = 0.5f), NeonRose.copy(alpha = 0.5f))
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Elegant futuristic status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E1035).copy(alpha = 0.4f))
                                .border(BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "READY",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Right: Actions with premium design
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Custom styled Tune/Edit button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF27272A).copy(alpha = 0.8f))
                                    .border(BorderStroke(1.dp, Color(0x44FFFFFF)), CircleShape)
                                    .clickable { editingLayout = currentSelectedLayout }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Edit Layout",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Glowing gradient USE button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(NeonRose, ElectricViolet)
                                        )
                                    )
                                    .clickable { onLayoutSelected(currentSelectedLayout.id) }
                                    .padding(horizontal = 18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GUNAKAN",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MODAL DIALOGS ---

    // Dialog for adding / configuring custom PNG layout with automatic transparent box detection
    if (showAddDialog && selectedImageUri != null) {
        PngLayoutConfigDialog(
            imageUri = selectedImageUri,
            initialTitle = customLayoutTitle,
            initialCategory = selectedCategory,
            initialBadge = selectedBadge,
            onDismiss = { showAddDialog = false },
            onSaveLayout = { title, category, badge, slots ->
                val saved = CustomLayoutStorage.saveCustomLayout(
                    context = context,
                    title = title,
                    photoCount = if (slots.isNotEmpty()) slots.size else selectedPhotoCount,
                    sourceUri = selectedImageUri!!,
                    category = category,
                    badgeLabel = badge,
                    explicitSlots = slots
                )
                if (saved != null) {
                    customLayoutsList = CustomLayoutStorage.getCustomLayouts(context)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
                showAddDialog = false
            }
        )
    }

    // Delete confirmation modal
    showDeleteConfirmDialog?.let { idToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = "Hapus Layout PNG Kustom?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus template PNG kustom ini dari koleksi aplikasi?",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        CustomLayoutStorage.deleteCustomLayout(context, idToDelete)
                        customLayoutsList = CustomLayoutStorage.getCustomLayouts(context)
                        coroutineScope.launch {
                            pagerState.scrollToPage(0)
                        }
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Hapus Permanen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Batal", color = Color.White)
                }
            },
            containerColor = Color(0xFF18181B),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Editing layout dialog (opens PngLayoutConfigDialog for PNG layouts, or LayoutIconEditorDialog for standard presets)
    editingLayout?.let { targetLayout ->
        if (targetLayout.customImagePath != null) {
            val cl = customLayoutsList.find { it.id == targetLayout.id }
            PngLayoutConfigDialog(
                existingImagePath = targetLayout.customImagePath,
                initialTitle = targetLayout.title,
                initialCategory = targetLayout.category,
                initialBadge = targetLayout.badgeLabel,
                initialSlots = cl?.transparentSlots ?: emptyList(),
                onDismiss = { editingLayout = null },
                onSaveLayout = { title, category, badge, slots ->
                    // Update existing custom layout in storage
                    val currentList = CustomLayoutStorage.getCustomLayouts(context).toMutableList()
                    val idx = currentList.indexOfFirst { it.id == targetLayout.id }
                    if (idx != -1) {
                        currentList[idx] = currentList[idx].copy(
                            title = title,
                            category = category,
                            badgeLabel = badge,
                            photoCount = if (slots.isNotEmpty()) slots.size else currentList[idx].photoCount,
                            transparentSlots = slots
                        )
                        val prefs = context.getSharedPreferences("custom_layouts_prefs", android.content.Context.MODE_PRIVATE)
                        val array = org.json.JSONArray()
                        for (item in currentList) {
                            val obj = org.json.JSONObject().apply {
                                put("id", item.id)
                                put("title", item.title)
                                put("photoCount", item.photoCount)
                                put("imagePath", item.imagePath)
                                put("category", item.category)
                                put("badgeLabel", item.badgeLabel)
                                val slotsArr = org.json.JSONArray()
                                for (s in item.transparentSlots) {
                                    val sObj = org.json.JSONObject().apply {
                                        put("left", s.left.toDouble())
                                        put("top", s.top.toDouble())
                                        put("right", s.right.toDouble())
                                        put("bottom", s.bottom.toDouble())
                                    }
                                    slotsArr.put(sObj)
                                }
                                put("transparentSlots", slotsArr)
                            }
                            array.put(obj)
                        }
                        prefs.edit().putString("layouts_json", array.toString()).apply()
                        customLayoutsList = CustomLayoutStorage.getCustomLayouts(context)
                    }
                    editingLayout = null
                }
            )
        } else {
            val currentCust = customizations[targetLayout.id] ?: LayoutCardCustomization(layoutId = targetLayout.id)
            LayoutIconEditorDialog(
                layout = targetLayout,
                initialCustomization = currentCust,
                onDismiss = { editingLayout = null },
                onSave = { updatedCust ->
                    customizations = customizations.toMutableMap().apply {
                        put(targetLayout.id, updatedCust)
                    }
                    editingLayout = null
                }
            )
        }
    }
}

// --- COVER FLOW SINGLE CARD ITEM ---
@Composable
private fun CoverFlowCardItem(
    layout: LayoutOptionItem,
    isSelected: Boolean,
    customization: LayoutCardCustomization?,
    cardWidth: Dp,
    cardHeight: Dp,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val isCustomPng = layout.customImagePath != null

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) NeonRose else Color(0x33FFFFFF)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clickable { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCustomPng) {
                AsyncImage(
                    model = java.io.File(layout.customImagePath!!),
                    contentDescription = layout.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LayoutCardVisualPreview(
                    layout = layout,
                    customization = customization,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- SELECTION CHECKMARK FLOATING BADGE (TOP RIGHT) ---
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(NeonRose)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Terpilih",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Floating Delete Trash Button for Custom PNGs
            if (isCustomPng) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xEE18181B))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


