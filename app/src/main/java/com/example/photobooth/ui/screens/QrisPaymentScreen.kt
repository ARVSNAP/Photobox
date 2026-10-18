package com.example.photobooth.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photobooth.model.BoothManagerConfig
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrisPaymentScreen(
    config: BoothManagerConfig,
    onPaymentSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var isPaid by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(config.qrisTimeoutMinutes * 60) }

    // Pulsing animation for waiting status
    val infiniteTransition = rememberInfiniteTransition(label = "qris_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Live countdown timer
    LaunchedEffect(isPaid, countdownSeconds) {
        if (!isPaid && countdownSeconds > 0) {
            delay(1000)
            countdownSeconds--
        }
    }

    // Generated fallback QRIS bitmap if no custom external image
    val defaultQrisBitmap = remember(config.qrisMerchantName, config.qrisNmid, config.pricePerSession) {
        generateDefaultQrisBitmap(
            merchantName = config.qrisMerchantName,
            nmid = config.qrisNmid,
            price = config.pricePerSession
        )
    }

    val formattedPrice = remember(config.pricePerSession) {
        NumberFormat.getNumberInstance(Locale("id", "ID")).format(config.pricePerSession)
    }

    val formattedTimer = remember(countdownSeconds) {
        val mins = countdownSeconds / 60
        val secs = countdownSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    Scaffold(
        containerColor = Color(0xFF070B14),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PEMBAYARAN QRIS KIOSK",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Scan menggunakan E-Wallet / M-Banking Anda",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Batal",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPaid) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Text(
                                text = if (isPaid) "LUNAS" else "MENUNGGU",
                                color = if (isPaid) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // E-Wallet & Bank Badges Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF131B2E),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💳 ALL PAYMENT SUPPORTED",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "GoPay • OVO • DANA • ShopeePay • BCA • Mandiri • BRI • BNI",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Main QRIS Card Container
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Official QRIS Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE11D48))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "QRIS",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Quick Response Code Indonesian Standard",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White
                        ) {
                            Text(
                                text = "GPN",
                                color = Color(0xFFE11D48),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Merchant Info Header inside QRIS Card
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = config.qrisMerchantName.uppercase(),
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "NMID: ${config.qrisNmid}",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // Image Display Box (Custom External Image vs Fallback QRIS)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // 1. External Custom Image from Local Uri (Gallery Upload)
                            !config.qrisCustomImageUri.isNullOrBlank() -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(Uri.parse(config.qrisCustomImageUri))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Custom QRIS Image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // 2. External Custom Image from Web URL
                            config.qrisCustomImageUrl.isNotBlank() -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(config.qrisCustomImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Custom URL QRIS Image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // 3. Fallback Dynamic Realistic QRIS Bitmap
                            else -> {
                                Image(
                                    bitmap = defaultQrisBitmap.asImageBitmap(),
                                    contentDescription = "Dynamic QRIS Code",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        // Success Overlay Animation
                        if (isPaid) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xEE10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Paid",
                                        tint = Color.White,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = "PEMBAYARAN BERHASIL!",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Sesi Foto Siap Dimulai...",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Price Nominal Box inside QR Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL BAYAR:",
                                color = Color(0xFF475569),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${config.currencySymbol} $formattedPrice",
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        text = "Cetak foto & sesi kamera akan otomatis aktif setelah pembayaran dikonfirmasi",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Status Bar & Countdown Timer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isPaid) Color(0xFF064E3B) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, if (isPaid) Color(0xFF10B981) else NeonCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isPaid) Color(0xFF10B981) else NeonCyan.copy(alpha = pulseAlpha)
                        ) {
                            Icon(
                                imageVector = if (isPaid) Icons.Default.Check else Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isPaid) "STATUS: LUNAS" else "MENUNGGU SCAN...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPaid) "Transaksi terverifikasi" else "Kadaluarsa dalam: $formattedTimer",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Simulation Payment Button
                    Button(
                        onClick = {
                            if (!isPaid) {
                                isChecking = true
                                isPaid = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaid) Color(0xFF10B981) else NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaid) "Berhasil!" else "Simulasi Bayar",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Auto-proceed action when paid
            LaunchedEffect(isPaid) {
                if (isPaid) {
                    delay(1200)
                    onPaymentSuccess()
                }
            }

            // Bottom Action buttons
            Row(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x55FFFFFF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        isPaid = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lanjut ke Kamera ➔", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Generates a realistic dynamic QRIS fallback bitmap image with standard QRIS pattern styling.
 */
fun generateDefaultQrisBitmap(
    merchantName: String,
    nmid: String,
    price: Int
): Bitmap {
    val size = 600
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    val blackPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val redPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#E11D48")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Grid size 25x25
    val gridCount = 25
    val moduleSize = size.toFloat() / gridCount
    val seed = (merchantName.hashCode() + nmid.hashCode() + price)

    val random = java.util.Random(seed.toLong())

    // Helper to draw QR finder pattern
    fun drawFinderPattern(startX: Int, startY: Int) {
        val left = startX * moduleSize
        val top = startY * moduleSize
        val finderSize = 7 * moduleSize

        // Outer black 7x7
        canvas.drawRect(left, top, left + finderSize, top + finderSize, blackPaint)
        // Inner white 5x5
        canvas.drawRect(left + moduleSize, top + moduleSize, left + 6 * moduleSize, top + 6 * moduleSize, bgPaint)
        // Center black 3x3
        canvas.drawRect(left + 2 * moduleSize, top + 2 * moduleSize, left + 5 * moduleSize, top + 5 * moduleSize, blackPaint)
    }

    // Top-Left Finder
    drawFinderPattern(1, 1)
    // Top-Right Finder
    drawFinderPattern(gridCount - 8, 1)
    // Bottom-Left Finder
    drawFinderPattern(1, gridCount - 8)

    // Random QR modules in remaining grid
    for (x in 0 until gridCount) {
        for (y in 0 until gridCount) {
            // Skip finder patterns
            if ((x in 0..8 && y in 0..8) ||
                (x in (gridCount - 9)..gridCount && y in 0..8) ||
                (x in 0..8 && y in (gridCount - 9)..gridCount)
            ) {
                continue
            }
            // Skip center logo box (rows 10..14, cols 10..14)
            if (x in 9..15 && y in 9..15) {
                continue
            }

            if (random.nextBoolean()) {
                canvas.drawRect(
                    x * moduleSize,
                    y * moduleSize,
                    (x + 1) * moduleSize,
                    (y + 1) * moduleSize,
                    blackPaint
                )
            }
        }
    }

    // Center QRIS Logo Box
    val centerLeft = 9.5f * moduleSize
    val centerTop = 9.5f * moduleSize
    val centerSize = 6f * moduleSize

    // White border background for center logo
    canvas.drawRect(centerLeft - 4f, centerTop - 4f, centerLeft + centerSize + 4f, centerTop + centerSize + 4f, bgPaint)
    // Red center box
    canvas.drawRect(centerLeft, centerTop, centerLeft + centerSize, centerTop + centerSize, redPaint)
    // Text "QRIS"
    canvas.drawText("QRIS", centerLeft + (centerSize / 2f), centerTop + (centerSize / 2f) + 8f, textPaint)

    return bitmap
}
