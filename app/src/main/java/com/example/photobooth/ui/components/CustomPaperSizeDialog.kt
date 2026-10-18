package com.example.photobooth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class PaperPreset(
    val id: String,
    val name: String,
    val description: String,
    val widthPx: Int,
    val heightPx: Int,
    val isLandscape: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPaperSizeDialog(
    onDismiss: () -> Unit,
    onConfirm: (widthPx: Int, heightPx: Int, presetName: String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val presets = remember {
        listOf(
            PaperPreset("a4", "A4", "210 × 297 mm", 2480, 3508),
            PaperPreset("a4_land", "A4 Landscape", "297 × 210 mm", 3508, 2480, isLandscape = true),
            PaperPreset("a5", "A5", "148 × 210 mm", 1748, 2480),
            PaperPreset("a5_land", "A5 Landscape", "210 × 148 mm", 2480, 1748, isLandscape = true),
            PaperPreset("photo_4x6", "4x6 Photo", "2400 × 3600 px", 2400, 3600),
            PaperPreset("strip_2x6", "2x6 Strip", "1200 × 3600 px", 1200, 3600)
        )
    }

    fun dismissWithKeyboardHide(action: () -> Unit) {
        focusManager.clearFocus()
        keyboardController?.hide()
        action()
    }

    var selectedPresetId by remember { mutableStateOf("a4") }
    var widthInput by remember { mutableStateOf("2480") }
    var heightInput by remember { mutableStateOf("3508") }

    fun selectPreset(preset: PaperPreset) {
        selectedPresetId = preset.id
        widthInput = preset.widthPx.toString()
        heightInput = preset.heightPx.toString()
    }

    val currentW = widthInput.toIntOrNull() ?: 2480
    val currentH = heightInput.toIntOrNull() ?: 3508
    val aspectRatio = if (currentH > 0) (currentW.toFloat() / currentH.toFloat()) else 0.71f

    Dialog(
        onDismissRequest = { dismissWithKeyboardHide(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF1C1B20),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2D2B36),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CropPortrait,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Custom Paper Size",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Choose a preset or enter dimensions in px.",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2B2933))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section: PRESET SIZES
                Text(
                    text = "PRESET SIZES",
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rows = presets.chunked(2)
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (preset in row) {
                                val isSelected = preset.id == selectedPresetId
                                Surface(
                                    onClick = { selectPreset(preset) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) Color(0xFF282338) else Color(0xFF24222C),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) Color(0xFFA855F7) else Color(0x22FFFFFF)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = if (preset.isLandscape) 26.dp else 18.dp, height = if (preset.isLandscape) 18.dp else 26.dp)
                                                .border(1.5.dp, if (isSelected) Color(0xFFA855F7) else Color(0xFF6B7280), RoundedCornerShape(3.dp))
                                                .background(if (isSelected) Color(0xFFA855F7).copy(alpha = 0.2f) else Color.Transparent)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = preset.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = preset.description,
                                                color = Color(0xFF9CA3AF),
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section: MANUAL INPUT
                Text(
                    text = "MANUAL INPUT",
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ratio Preview Thumbnail
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF24222C),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier.size(width = 64.dp, height = 112.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = (30 * aspectRatio.coerceIn(0.4f, 2.0f)).dp,
                                        height = (42 / aspectRatio.coerceIn(0.4f, 2.0f)).dp
                                    )
                                    .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(3.dp))
                                    .background(Color(0xFFA855F7).copy(alpha = 0.15f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%.2f", aspectRatio),
                                color = Color(0xFFA855F7),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Width Input
                        OutlinedTextField(
                            value = widthInput,
                            onValueChange = {
                                widthInput = it.filter { char -> char.isDigit() }
                                selectedPresetId = "custom"
                            },
                            prefix = {
                                Text(
                                    text = "W ",
                                    color = Color(0xFFA855F7),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            },
                            suffix = {
                                Text(
                                    text = "px",
                                    color = Color(0xFF6B7280),
                                    fontSize = 11.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141318),
                                unfocusedContainerColor = Color(0xFF141318),
                                focusedBorderColor = Color(0xFFA855F7),
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Swap Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val temp = widthInput
                                    widthInput = heightInput
                                    heightInput = temp
                                    selectedPresetId = "custom"
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF2D2B36))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap Orientation",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Height Input
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = {
                                heightInput = it.filter { char -> char.isDigit() }
                                selectedPresetId = "custom"
                            },
                            prefix = {
                                Text(
                                    text = "H ",
                                    color = Color(0xFFA855F7),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            },
                            suffix = {
                                Text(
                                    text = "px",
                                    color = Color(0xFF6B7280),
                                    fontSize = 11.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141318),
                                unfocusedContainerColor = Color(0xFF141318),
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

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { dismissWithKeyboardHide(onDismiss) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24222C)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Batal",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            dismissWithKeyboardHide {
                                val presetName = presets.find { it.id == selectedPresetId }?.name ?: "Custom"
                                onConfirm(currentW.coerceAtLeast(300), currentH.coerceAtLeast(300), presetName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OK",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
