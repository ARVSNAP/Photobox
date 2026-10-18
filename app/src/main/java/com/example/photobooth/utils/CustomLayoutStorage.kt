package com.example.photobooth.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class CustomLayout(
    val id: String,
    val title: String,
    val photoCount: Int,
    val imagePath: String,
    val category: String = "Layout PNG Kustom",
    val badgeLabel: String = "✓ Free",
    val transparentSlots: List<TransparentSlot> = emptyList()
)

object CustomLayoutStorage {
    private const val PREFS_NAME = "custom_layouts_prefs"
    private const val KEY_LAYOUTS = "layouts_json"
    private const val TAG = "CustomLayoutStorage"

    fun saveCustomLayout(
        context: Context,
        title: String,
        photoCount: Int,
        sourceUri: Uri,
        category: String = "Layout PNG Kustom",
        badgeLabel: String = "✓ Free",
        explicitSlots: List<TransparentSlot>? = null,
        autoPopulateAllSizes: Boolean = true
    ): CustomLayout? {
        return try {
            val resolver = context.contentResolver
            val timestamp = System.currentTimeMillis()
            val fileName = "custom_layout_$timestamp.png"
            val destFile = File(context.filesDir, fileName)

            resolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!destFile.exists()) {
                Log.e(TAG, "Failed to copy custom layout PNG.")
                return null
            }

            // Decode source bitmap for adaptation
            val sourceBitmap = android.graphics.BitmapFactory.decodeFile(destFile.absolutePath)

            // Auto-detect transparent slots if none provided explicitly
            val detectedSlots = explicitSlots ?: PngTransparentDetector.detectTransparentSlots(destFile.absolutePath)
            val finalPhotoCount = if (detectedSlots.isNotEmpty()) detectedSlots.size else photoCount

            val primaryLayout = CustomLayout(
                id = "custom_layout_$timestamp",
                title = title,
                photoCount = finalPhotoCount,
                imagePath = destFile.absolutePath,
                category = category,
                badgeLabel = badgeLabel,
                transparentSlots = detectedSlots
            )

            val newLayoutsList = mutableListOf(primaryLayout)

            // Automatically generate adapted layout items for Square, 2x6, and 4x6 if requested
            if (autoPopulateAllSizes && sourceBitmap != null) {
                // 1. Square Layout (1:1 aspect ratio)
                adaptAndCreateLayout(
                    context = context,
                    sourceBitmap = sourceBitmap,
                    targetW = 1200,
                    targetH = 1200,
                    prefix = "custom_layout_square",
                    title = if (title.contains("Square", ignoreCase = true)) title else "$title (Square)",
                    category = "Square",
                    badgeLabel = badgeLabel,
                    fallbackSlots = listOf(
                        TransparentSlot(0.08f, 0.08f, 0.48f, 0.48f),
                        TransparentSlot(0.52f, 0.08f, 0.92f, 0.48f),
                        TransparentSlot(0.08f, 0.52f, 0.48f, 0.92f),
                        TransparentSlot(0.52f, 0.52f, 0.92f, 0.92f)
                    )
                )?.let { newLayoutsList.add(it) }

                // 2. Layout 2x6 (1:3 photostrip ratio)
                adaptAndCreateLayout(
                    context = context,
                    sourceBitmap = sourceBitmap,
                    targetW = 600,
                    targetH = 1800,
                    prefix = "custom_layout_2x6",
                    title = if (title.contains("2x6", ignoreCase = true)) title else "$title (2x6 Strip)",
                    category = "Layout 2x6",
                    badgeLabel = badgeLabel,
                    fallbackSlots = listOf(
                        TransparentSlot(0.08f, 0.08f, 0.92f, 0.35f),
                        TransparentSlot(0.08f, 0.38f, 0.92f, 0.65f),
                        TransparentSlot(0.08f, 0.68f, 0.92f, 0.95f)
                    )
                )?.let { newLayoutsList.add(it) }

                // 3. Layout 4x6 (2:3 postcard ratio)
                adaptAndCreateLayout(
                    context = context,
                    sourceBitmap = sourceBitmap,
                    targetW = 1200,
                    targetH = 1800,
                    prefix = "custom_layout_4x6",
                    title = if (title.contains("4x6", ignoreCase = true)) title else "$title (4x6 Grid)",
                    category = "Layout 4x6",
                    badgeLabel = badgeLabel,
                    fallbackSlots = listOf(
                        TransparentSlot(0.06f, 0.08f, 0.47f, 0.46f),
                        TransparentSlot(0.53f, 0.08f, 0.94f, 0.46f),
                        TransparentSlot(0.06f, 0.52f, 0.47f, 0.90f),
                        TransparentSlot(0.53f, 0.52f, 0.94f, 0.90f)
                    )
                )?.let { newLayoutsList.add(it) }
            }

            sourceBitmap?.recycle()

            val currentLayouts = getCustomLayouts(context).toMutableList()
            currentLayouts.addAll(newLayoutsList)
            saveLayoutsList(context, currentLayouts)

            primaryLayout
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom layout", e)
            null
        }
    }

    fun saveCustomLayoutBitmap(
        context: Context,
        title: String,
        bitmap: android.graphics.Bitmap,
        category: String = "Layout PNG Kustom",
        badgeLabel: String = "👑 Kustom",
        explicitSlots: List<TransparentSlot>
    ): CustomLayout? {
        return try {
            val timestamp = System.currentTimeMillis()
            val fileName = "custom_edited_layout_$timestamp.png"
            val destFile = File(context.filesDir, fileName)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }

            if (!destFile.exists()) return null

            val newLayout = CustomLayout(
                id = "custom_edited_$timestamp",
                title = title,
                photoCount = explicitSlots.size.coerceAtLeast(1),
                imagePath = destFile.absolutePath,
                category = category,
                badgeLabel = badgeLabel,
                transparentSlots = explicitSlots
            )

            val currentList = getCustomLayouts(context).toMutableList()
            currentList.add(0, newLayout)
            saveLayoutsList(context, currentList)

            newLayout
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom layout bitmap", e)
            null
        }
    }

    private fun adaptAndCreateLayout(
        context: Context,
        sourceBitmap: android.graphics.Bitmap,
        targetW: Int,
        targetH: Int,
        prefix: String,
        title: String,
        category: String,
        badgeLabel: String,
        fallbackSlots: List<TransparentSlot>
    ): CustomLayout? {
        return try {
            val result = android.graphics.Bitmap.createBitmap(targetW, targetH, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(result)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)

            val srcW = sourceBitmap.width.toFloat()
            val srcH = sourceBitmap.height.toFloat()
            val srcAspect = srcW / srcH
            val targetAspect = targetW.toFloat() / targetH.toFloat()

            val destRect = if (srcAspect > targetAspect) {
                val scaledH = targetW / srcAspect
                val top = (targetH - scaledH) / 2f
                android.graphics.RectF(0f, top, targetW.toFloat(), top + scaledH)
            } else {
                val scaledW = targetH * srcAspect
                val left = (targetW - scaledW) / 2f
                android.graphics.RectF(left, 0f, left + scaledW, targetH.toFloat())
            }

            canvas.drawBitmap(sourceBitmap, null, destRect, paint)

            val timestamp = System.currentTimeMillis()
            val fileName = "${prefix}_${timestamp}_${(100..999).random()}.png"
            val destFile = File(context.filesDir, fileName)

            FileOutputStream(destFile).use { out ->
                result.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            result.recycle()

            if (!destFile.exists()) return null

            val detectedSlots = PngTransparentDetector.detectTransparentSlots(destFile.absolutePath)
            val finalSlots = if (detectedSlots.isNotEmpty()) detectedSlots else fallbackSlots

            CustomLayout(
                id = "${prefix}_$timestamp",
                title = title,
                photoCount = finalSlots.size,
                imagePath = destFile.absolutePath,
                category = category,
                badgeLabel = badgeLabel,
                transparentSlots = finalSlots
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adapt layout for category $category", e)
            null
        }
    }

    fun getCustomLayouts(context: Context): List<CustomLayout> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_LAYOUTS, null) ?: return emptyList()
        val list = mutableListOf<CustomLayout>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val photoCount = obj.getInt("photoCount")
                val imagePath = obj.getString("imagePath")
                val category = obj.optString("category", "Layout PNG Kustom")
                val badgeLabel = obj.optString("badgeLabel", "✓ Free")

                val slotsList = mutableListOf<TransparentSlot>()
                val slotsArray = obj.optJSONArray("transparentSlots")
                if (slotsArray != null) {
                    for (j in 0 until slotsArray.length()) {
                        val sObj = slotsArray.getJSONObject(j)
                        slotsList.add(
                            TransparentSlot(
                                left = sObj.getDouble("left").toFloat(),
                                top = sObj.getDouble("top").toFloat(),
                                right = sObj.getDouble("right").toFloat(),
                                bottom = sObj.getDouble("bottom").toFloat()
                            )
                        )
                    }
                }

                // Check if the file actually exists
                if (File(imagePath).exists()) {
                    var finalSlots = slotsList.toList()
                    // Lazy auto-detect if layout existed previously without slots
                    if (finalSlots.isEmpty()) {
                        finalSlots = PngTransparentDetector.detectTransparentSlots(imagePath)
                    }

                    list.add(
                        CustomLayout(
                            id = id,
                            title = title,
                            photoCount = if (finalSlots.isNotEmpty()) finalSlots.size else photoCount,
                            imagePath = imagePath,
                            category = category,
                            badgeLabel = badgeLabel,
                            transparentSlots = finalSlots
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom layouts", e)
        }
        return list
    }

    fun deleteCustomLayout(context: Context, layoutId: String) {
        val current = getCustomLayouts(context).toMutableList()
        val iterator = current.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.id == layoutId) {
                try {
                    File(item.imagePath).delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete file: ${item.imagePath}", e)
                }
                iterator.remove()
            }
        }
        saveLayoutsList(context, current)
    }

    private fun saveLayoutsList(context: Context, list: List<CustomLayout>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("photoCount", item.photoCount)
                obj.put("imagePath", item.imagePath)
                obj.put("category", item.category)
                obj.put("badgeLabel", item.badgeLabel)

                val slotsArray = JSONArray()
                for (s in item.transparentSlots) {
                    val sObj = JSONObject()
                    sObj.put("left", s.left.toDouble())
                    sObj.put("top", s.top.toDouble())
                    sObj.put("right", s.right.toDouble())
                    sObj.put("bottom", s.bottom.toDouble())
                    slotsArray.put(sObj)
                }
                obj.put("transparentSlots", slotsArray)

                array.put(obj)
            }
            prefs.edit().putString(KEY_LAYOUTS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving layouts list", e)
        }
    }
}
