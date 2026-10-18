package com.example.photobooth.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photobooth.ui.components.CustomPaperSizeDialog
import com.example.photobooth.utils.CustomLayout
import com.example.photobooth.utils.CustomLayoutStorage
import coil.compose.rememberAsyncImagePainter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTemplatesScreen(
    onBackClicked: () -> Unit,
    onCreateNewLayout: (widthPx: Int, heightPx: Int, presetName: String) -> Unit,
    onUseLayout: (layoutId: String) -> Unit
) {
    val context = LocalContext.current
    var customLayouts by remember { mutableStateOf<List<CustomLayout>>(emptyList()) }
    var showPaperSizeDialog by remember { mutableStateOf(false) }
    var showCanvaGuideDialog by remember { mutableStateOf(false) }
    var layoutToDelete by remember { mutableStateOf<String?>(null) }

    fun refreshLayouts() {
        customLayouts = CustomLayoutStorage.getCustomLayouts(context)
    }

    LaunchedEffect(Unit) {
        refreshLayouts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Template Saya",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121214))
            )
        },
        containerColor = Color(0xFF0F0E11)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. CARD "Buat Baru" (Design layout yourself)
            Surface(
                onClick = { showPaperSizeDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF7C3AED), Color(0xFFC084FC))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Buat Baru",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Desain layout sendiri",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. CARD "Cara Mengimpor Template dari Canva"
            Surface(
                onClick = { showCanvaGuideDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0284C7), Color(0xFF9333EA), Color(0xFFEC4899))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row: Badge & External Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "Canva",
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Video tutorial",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Bottom row: Play Icon & Description
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF9333EA),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cara Mengimpor Template dari Canva",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Siapkan desain Canva lalu masukkan ke LemonBooth sebagai template foto yang siap dipakai.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. SECTION "Template Kamu"
            Text(
                text = "Template Kamu",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (customLayouts.isEmpty()) {
                // EMPTY STATE (Matching Image 1!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1C24),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ImageNotSupported,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Belum ada template",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Buat template pertamamu di sini",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // SAVED CUSTOM TEMPLATES LIST
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(customLayouts) { layout ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1A1920),
                            border = BorderStroke(1.dp, Color(0x22FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF272530),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    if (File(layout.imagePath).exists()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(File(layout.imagePath)),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.GridOn,
                                                contentDescription = null,
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = layout.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${layout.photoCount} Slot Foto • ${layout.category}",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { layoutToDelete = layout.id },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Button(
                                        onClick = { onUseLayout(layout.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            text = "Gunakan",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Paper Size Modal
    if (showPaperSizeDialog) {
        CustomPaperSizeDialog(
            onDismiss = { showPaperSizeDialog = false },
            onConfirm = { widthPx, heightPx, presetName ->
                showPaperSizeDialog = false
                onCreateNewLayout(widthPx, heightPx, presetName)
            }
        )
    }

    // Canva Tutorial Modal
    if (showCanvaGuideDialog) {
        AlertDialog(
            onDismissRequest = { showCanvaGuideDialog = false },
            confirmButton = {
                TextButton(onClick = { showCanvaGuideDialog = false }) {
                    Text("Mengerti", color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("Panduan Impor Template Canva", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Buat desain frame photobooth Anda di Canva dengan slot transparan (PNG).", color = Color(0xFFD1D5DB), fontSize = 13.sp)
                    Text("2. Unduh hasil desain Canva Anda sebagai PNG Transparan.", color = Color(0xFFD1D5DB), fontSize = 13.sp)
                    Text("3. Masuk ke Editor Kustom LemonBooth, pilih 'Latar/Gambar' lalu unggah file PNG Canva Anda.", color = Color(0xFFD1D5DB), fontSize = 13.sp)
                    Text("4. Aplikasi akan mendeteksi slot foto secara otomatis dan siap dipakai!", color = Color(0xFFD1D5DB), fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1E1B26)
        )
    }

    // Delete confirmation dialog
    if (layoutToDelete != null) {
        AlertDialog(
            onDismissRequest = { layoutToDelete = null },
            confirmButton = {
                Button(
                    onClick = {
                        layoutToDelete?.let { CustomLayoutStorage.deleteCustomLayout(context, it) }
                        layoutToDelete = null
                        refreshLayouts()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { layoutToDelete = null }) {
                    Text("Batal", color = Color.White)
                }
            },
            title = { Text("Hapus Template?", color = Color.White) },
            text = { Text("Template ini akan dihapus permanen dari daftar.", color = Color(0xFF9CA3AF)) },
            containerColor = Color(0xFF1E1B26)
        )
    }
}
