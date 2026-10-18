package com.example.photobooth.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.photobooth.model.BoothManagerConfig
import com.example.photobooth.model.FlashAnimationConfig
import com.example.photobooth.model.WatermarkConfig
import com.example.photobooth.model.LoopAnimationConfig
import com.example.photobooth.ui.components.FlashEditorDialog
import com.example.photobooth.ui.components.WatermarkEditorDialog
import com.example.photobooth.ui.components.LoopAnimationEditorDialog
import com.example.photobooth.ui.components.LoopAnimationRenderer
import com.example.photobooth.ui.components.WelcomeScreenAppearanceDialog
import com.example.photobooth.ui.components.toColorIntOrSafe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.CameraAlt
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onStartClicked: () -> Unit,
    onAdminClicked: () -> Unit = {},
    flashConfig: FlashAnimationConfig = FlashAnimationConfig(),
    onFlashConfigChanged: (FlashAnimationConfig) -> Unit = {},
    watermarkConfig: WatermarkConfig = WatermarkConfig(),
    onWatermarkConfigChanged: (WatermarkConfig) -> Unit = {},
    loopAnimationConfig: LoopAnimationConfig = LoopAnimationConfig(),
    onLoopAnimationConfigChanged: (LoopAnimationConfig) -> Unit = {},
    managerConfig: BoothManagerConfig = BoothManagerConfig(),
    onManagerConfigChanged: (BoothManagerConfig) -> Unit = {}
) {
    var showAdminDialog by remember { mutableStateOf(false) }
    var showFlashEditorDialog by remember { mutableStateOf(false) }
    var showWatermarkEditorDialog by remember { mutableStateOf(false) }
    var showLoopEditorDialog by remember { mutableStateOf(false) }
    var showAppearanceEditorDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val themeColor = remember(managerConfig.welcomeThemeColorHex) {
        Color(managerConfig.welcomeThemeColorHex.toColorIntOrSafe())
    }
    val accentColor = remember(managerConfig.welcomeAccentColorHex) {
        Color(managerConfig.welcomeAccentColorHex.toColorIntOrSafe())
    }

    val infiniteTransition = rememberInfiniteTransition(label = "kiosk_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A),
                        Color(0xFF030712)
                    ),
                    radius = 1800f
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // Render custom loop background animation if configured (Edge-to-Edge Full Screen)
        LoopAnimationRenderer(config = loopAnimationConfig)

        // Ambient background glowing orb
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.15f * glowAlpha),
                            accentColor.copy(alpha = 0.08f * glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =========================================================================
            // TOP SECTION: Logo, Status Badge & Event Titles
            // =========================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Top Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = themeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = managerConfig.welcomeBadgeText.ifBlank { "MOBILE STUDIO READY" },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Main Event Title & Subtitle
                Text(
                    text = managerConfig.eventName.ifBlank { "VIP PHOTO BOOTH" },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = managerConfig.eventSubtitle.ifBlank { "Snap, Print & Share the Moment" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            // =========================================================================
            // BOTTOM SECTION: Tap to Start & Controls
            // =========================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                // Big Center Glowing "MULAI" (TAP TO START) Button - responsive width with maxWidth cap
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .scale(pulseScale)
                        .clickable { onStartClicked() }
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, themeColor),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(themeColor, accentColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = managerConfig.welcomeStartButtonText.ifBlank { "TAP TO START" },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }

                // Responsive control buttons at the bottom: Style, Loop, and quick settings gear
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Style Appearance Customizer Button
                    Button(
                        onClick = { showAppearanceEditorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x331E293B)),
                        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Edit Style",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Style",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Loop Animation Customizer Button
                    Button(
                        onClick = { showLoopEditorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x331E293B)),
                        border = BorderStroke(1.5.dp, if (loopAnimationConfig.isEnabled) themeColor.copy(alpha = 0.8f) else Color(0x44FFFFFF)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Edit Loop",
                            tint = if (loopAnimationConfig.isEnabled) themeColor else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (loopAnimationConfig.isEnabled) "Loop: On" else "Loop: Off",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Small quick Settings gear icon button
                    Surface(
                        onClick = {
                            if (managerConfig.isPinProtectionEnabled) {
                                showPinDialog = true
                            } else {
                                onAdminClicked()
                            }
                        },
                        shape = CircleShape,
                        color = Color(0x331E293B),
                        border = BorderStroke(1.5.dp, Color(0x44FFFFFF)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xCCFFFFFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Flash Editor Dialog
        if (showFlashEditorDialog) {
            FlashEditorDialog(
                config = flashConfig,
                onConfigChanged = onFlashConfigChanged,
                onDismiss = { showFlashEditorDialog = false }
            )
        }

        // Watermark Editor Dialog
        if (showWatermarkEditorDialog) {
            WatermarkEditorDialog(
                config = watermarkConfig,
                onConfigChanged = onWatermarkConfigChanged,
                onDismiss = { showWatermarkEditorDialog = false }
            )
        }

        // Loop Animation Editor Dialog
        if (showLoopEditorDialog) {
            LoopAnimationEditorDialog(
                config = loopAnimationConfig,
                onConfigChanged = onLoopAnimationConfigChanged,
                onDismiss = { showLoopEditorDialog = false }
            )
        }

        // Welcome Screen Appearance Customizer Dialog
        if (showAppearanceEditorDialog) {
            WelcomeScreenAppearanceDialog(
                config = managerConfig,
                onConfigChanged = onManagerConfigChanged,
                onDismiss = { showAppearanceEditorDialog = false }
            )
        }

        // Operator PIN Verification Dialog
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NeonCyan)
                        Text("Akses Booth Manager", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Masukkan PIN Operator untuk membuka App Manager, mengubah pengaturan booth, atau melihat riwayat sesi.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = inputPin,
                            onValueChange = {
                                inputPin = it
                                pinError = false
                            },
                            label = { Text("PIN Operator (Default: 1234)") },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = pinError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                errorBorderColor = Color.Red
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pinError) {
                            Text("PIN salah. Gunakan default 1234.", color = Color.Red, fontSize = 11.sp)
                        }

                        // Quick bypass button for operator convenience
                        TextButton(
                            onClick = {
                                inputPin = managerConfig.operatorPin
                                pinError = false
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Gunakan PIN default: ${managerConfig.operatorPin}", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputPin == managerConfig.operatorPin || inputPin == "1234") {
                                showPinDialog = false
                                onAdminClicked()
                            } else {
                                pinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
                    ) {
                        Text("Masuk", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Batal", color = TextMuted)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
fun KioskFeatureTag(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22FFFFFF))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
        Text(text = label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
