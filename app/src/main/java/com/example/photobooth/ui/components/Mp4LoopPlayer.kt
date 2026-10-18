package com.example.photobooth.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * High-performance full-screen MP4 loop video player.
 * Uses TextureView to seamlessly integrate with Compose without surface punch-through or Z-order conflicts.
 * Supports aspect ratio fitting (tidak terpotong), full screen stretch, and zoom crop.
 */
@Composable
fun Mp4LoopPlayer(
    uriString: String,
    scaleMode: String = "fit",
    speed: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    fun applyScaleMatrix(tv: TextureView, vWidth: Int, vHeight: Int, mode: String) {
        val viewWidth = tv.width
        val viewHeight = tv.height
        if (viewWidth <= 0 || viewHeight <= 0 || vWidth <= 0 || vHeight <= 0) return

        val matrix = Matrix()
        val sx = viewWidth.toFloat() / vWidth
        val sy = viewHeight.toFloat() / vHeight

        when (mode) {
            "fill" -> {
                // Stretch to fill 100% of screen edge-to-edge without cutting off any content
                matrix.setScale(1f, 1f)
            }
            "crop" -> {
                // Center crop: fill entire screen, but may crop outer edges
                val scale = maxOf(sx, sy)
                val scaledWidth = vWidth * scale
                val scaledHeight = vHeight * scale
                matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, viewWidth / 2f, viewHeight / 2f)
            }
            else -> { // "fit" (Default: Full screen, 100% intact / tidak terpotong)
                // Center fit: fits entire video on screen without cropping
                val scale = minOf(sx, sy)
                val scaledWidth = vWidth * scale
                val scaledHeight = vHeight * scale
                matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, viewWidth / 2f, viewHeight / 2f)
            }
        }
        tv.setTransform(matrix)
    }

    fun startPlayback(tv: TextureView, surfaceTexture: SurfaceTexture, targetUri: String) {
        try {
            mediaPlayer?.let { oldMp ->
                try {
                    oldMp.stop()
                    oldMp.reset()
                    oldMp.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
            val mp = MediaPlayer().apply {
                val surface = Surface(surfaceTexture)
                setSurface(surface)
                val parsed = Uri.parse(targetUri)
                if (parsed.scheme == "file" && parsed.path != null) {
                    setDataSource(parsed.path)
                } else {
                    setDataSource(context, parsed)
                }
                isLooping = true
                setVolume(0f, 0f) // Silent loop
                setOnVideoSizeChangedListener { _, w, h ->
                    if (w > 0 && h > 0) {
                        videoWidth = w
                        videoHeight = h
                        textureViewRef?.let { ref ->
                            applyScaleMatrix(ref, w, h, scaleMode)
                        }
                    }
                }
                setOnPreparedListener { preparedMp ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && speed != 1.0f) {
                        try {
                            preparedMp.playbackParams = preparedMp.playbackParams.setSpeed(speed)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    preparedMp.start()
                }
                prepareAsync()
            }
            mediaPlayer = mp
            tv.tag = targetUri
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(uriString) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                textureViewRef = this
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        startPlayback(this@apply, surfaceTexture, uriString)
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        if (videoWidth > 0 && videoHeight > 0) {
                            applyScaleMatrix(this@apply, videoWidth, videoHeight, scaleMode)
                        }
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.reset()
                            mediaPlayer?.release()
                            mediaPlayer = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                }
            }
        },
        update = { tv ->
            // If the URI changed while the surface is alive, restart playback
            if (tv.isAvailable && tv.surfaceTexture != null && tv.tag != uriString) {
                startPlayback(tv, tv.surfaceTexture!!, uriString)
            } else if (videoWidth > 0 && videoHeight > 0) {
                applyScaleMatrix(tv, videoWidth, videoHeight, scaleMode)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
