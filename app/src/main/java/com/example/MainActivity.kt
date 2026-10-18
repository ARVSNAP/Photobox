package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photobooth.hardware.KioskUtil
import com.example.photobooth.ui.screens.*
import com.example.photobooth.ui.viewmodel.PhotoBoothViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        KioskUtil.startKioskMode(this)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: PhotoBoothViewModel = viewModel()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "welcome",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        enterTransition = { fadeIn(animationSpec = tween(500)) },
                        exitTransition = { fadeOut(animationSpec = tween(500)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(500)) },
                        popExitTransition = { fadeOut(animationSpec = tween(500)) }
                    ) {
                        composable("welcome") {
                            val flashConfig by viewModel.flashAnimationConfig.collectAsState()
                            val watermarkConfig by viewModel.watermarkConfig.collectAsState()
                            val managerConfig by viewModel.managerConfig.collectAsState()
                            val loopAnimationConfig by viewModel.loopAnimationConfig.collectAsState()
                            WelcomeScreen(
                                onStartClicked = { navController.navigate("layout_selector") },
                                onAdminClicked = { navController.navigate("manager_app") },
                                flashConfig = flashConfig,
                                onFlashConfigChanged = { viewModel.setFlashConfig(it) },
                                watermarkConfig = watermarkConfig,
                                onWatermarkConfigChanged = { viewModel.setWatermarkConfig(it) },
                                loopAnimationConfig = loopAnimationConfig,
                                onLoopAnimationConfigChanged = { viewModel.setLoopAnimationConfig(it) },
                                managerConfig = managerConfig,
                                onManagerConfigChanged = { viewModel.updateManagerConfig(it) }
                            )
                        }
                        composable("manager_app") {
                            ManagerAppScreen(
                                viewModel = viewModel,
                                onBackClicked = { navController.popBackStack() }
                            )
                        }
                        composable("layout_selector") {
                            val managerConfig by viewModel.managerConfig.collectAsState()
                            LayoutSelectorScreen(
                                onLayoutSelected = { layout ->
                                    viewModel.setLayout(layout)
                                    viewModel.clearCapturedPhotos()
                                    if (managerConfig.isQrisPaymentEnabled) {
                                        navController.navigate("qris_payment")
                                    } else {
                                        navController.navigate("live_preview")
                                    }
                                },
                                onBackClicked = {
                                    navController.popBackStack()
                                },
                                onMyTemplatesClicked = {
                                    navController.navigate("my_templates")
                                }
                            )
                        }
                        composable("my_templates") {
                            MyTemplatesScreen(
                                onBackClicked = { navController.popBackStack() },
                                onCreateNewLayout = { widthPx, heightPx, presetName ->
                                    navController.navigate("custom_editor/$widthPx/$heightPx/$presetName")
                                },
                                onUseLayout = { layoutId ->
                                    viewModel.setLayout(layoutId)
                                    viewModel.clearCapturedPhotos()
                                    val managerConfig = viewModel.managerConfig.value
                                    if (managerConfig.isQrisPaymentEnabled) {
                                        navController.navigate("qris_payment")
                                    } else {
                                        navController.navigate("live_preview")
                                    }
                                }
                            )
                        }
                        composable("custom_editor/{widthPx}/{heightPx}/{presetName}") { backStackEntry ->
                            val widthPx = backStackEntry.arguments?.getString("widthPx")?.toIntOrNull() ?: 2480
                            val heightPx = backStackEntry.arguments?.getString("heightPx")?.toIntOrNull() ?: 3508
                            val presetName = backStackEntry.arguments?.getString("presetName") ?: "Custom"
                            CustomLayoutEditorScreen(
                                paperWidthPx = widthPx,
                                paperHeightPx = heightPx,
                                presetName = presetName,
                                onBackClicked = { navController.popBackStack() },
                                onLayoutSaved = { layoutId ->
                                    viewModel.setLayout(layoutId)
                                    viewModel.clearCapturedPhotos()
                                    navController.popBackStack("layout_selector", inclusive = false)
                                }
                            )
                        }
                        composable("qris_payment") {
                            val managerConfig by viewModel.managerConfig.collectAsState()
                            QrisPaymentScreen(
                                config = managerConfig,
                                onPaymentSuccess = {
                                    navController.navigate("live_preview") {
                                        popUpTo("layout_selector") { inclusive = false }
                                    }
                                },
                                onCancel = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("live_preview") {
                            val layoutType by viewModel.selectedLayout.collectAsState()
                            val flashConfig by viewModel.flashAnimationConfig.collectAsState()
                            val watermarkConfig by viewModel.watermarkConfig.collectAsState()
                            val managerConfig by viewModel.managerConfig.collectAsState()
                            LivePreviewScreen(
                                layoutType = layoutType,
                                flashConfig = flashConfig,
                                onFlashConfigChanged = { viewModel.setFlashConfig(it) },
                                watermarkConfig = watermarkConfig,
                                onWatermarkConfigChanged = { viewModel.setWatermarkConfig(it) },
                                managerConfig = managerConfig,
                                onPhotosComplete = { photos ->
                                    photos.forEach { viewModel.addCapturedPhoto(it) }
                                    navController.navigate("photo_editor")
                                }
                            )
                        }
                        composable("photo_editor") {
                            val photos by viewModel.capturedPhotos.collectAsState()
                            val layoutType by viewModel.selectedLayout.collectAsState()
                            val watermarkConfig by viewModel.watermarkConfig.collectAsState()
                            val brandingFrameConfig by viewModel.brandingFrameConfig.collectAsState()
                            val flashConfig by viewModel.flashAnimationConfig.collectAsState()
                            val managerConfig by viewModel.managerConfig.collectAsState()

                            PhotoEditorScreen(
                                photos = photos,
                                layoutType = layoutType,
                                watermarkConfig = watermarkConfig,
                                onWatermarkConfigChanged = { viewModel.setWatermarkConfig(it) },
                                brandingFrameConfig = brandingFrameConfig,
                                onBrandingFrameConfigChanged = { viewModel.setBrandingFrameConfig(it) },
                                flashConfig = flashConfig,
                                managerConfig = managerConfig,
                                onPhotoReplaced = { index, newBitmap ->
                                    viewModel.replaceCapturedPhoto(index, newBitmap)
                                },
                                onPhotosRearranged = { reordered ->
                                    viewModel.setCapturedPhotos(reordered)
                                },
                                onRetakeAll = {
                                    viewModel.setCapturedPhotos(emptyList())
                                    navController.popBackStack("live_preview", inclusive = false)
                                },
                                onFinishSession = {
                                    viewModel.saveSession(layoutType, photos.size, 1)
                                    viewModel.setCapturedPhotos(emptyList())
                                    navController.navigate("welcome") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onEditCompleted = { composedBitmap, gifBytes ->
                                    viewModel.setFinalComposedBitmap(composedBitmap)
                                    if (gifBytes != null) {
                                        viewModel.setGeneratedGifBytes(gifBytes)
                                    }
                                    viewModel.saveSession(layoutType, photos.size, 1)
                                    navController.navigate("sharing_hub")
                                }
                            )
                        }
                        composable("sharing_hub") {
                            val finalBitmap by viewModel.finalComposedBitmap.collectAsState()
                            val photos by viewModel.capturedPhotos.collectAsState()
                            val layoutType by viewModel.selectedLayout.collectAsState()
                            val isGifMode = (layoutType == "GIF / Boomerang")
                            val gifBytes by viewModel.generatedGifBytes.collectAsState()

                            finalBitmap?.let { bitmap ->
                                SharingHubScreen(
                                    finalBitmap = bitmap,
                                    gifFrames = photos,
                                    gifBytes = gifBytes,
                                    isGifMode = isGifMode,
                                    onSessionFinished = {
                                        navController.navigate("welcome") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        KioskUtil.hideSystemUI(this)
    }
}

