package com.example.photobooth.utils

import android.util.Log
import com.example.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"

    // Cloudinary Credentials & Config
    const val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/jggtvymn/image/upload"
    const val UPLOAD_PRESET = "DdFr-TgDz8NTwz91psJSuvP1i5c"
    const val API_KEY = "865291324538569"
    const val API_SECRET = "DdFr-TgDz8NTwz91psJSuvP1i5c"
    const val CLOUD_NAME = "jggtvymn"

    fun isConfigured(): Boolean {
        return true
    }

    private fun getRawUrl(): String? {
        return try {
            BuildConfig.CLOUDINARY_URL
        } catch (e: Exception) {
            null
        }
    }

    fun getCloudinaryCredentials(): Triple<String, String, String> {
        val rawUrl = getRawUrl()
        if (!rawUrl.isNullOrBlank()) {
            val cleanUrl = rawUrl.replace("<", "").replace(">", "").trim()
            if (cleanUrl.startsWith("cloudinary://")) {
                try {
                    val withoutPrefix = cleanUrl.substringAfter("cloudinary://")
                    val parts = withoutPrefix.split("@")
                    if (parts.size == 2) {
                        val credentialsPart = parts[0]
                        val cloudName = parts[1]
                        val creds = credentialsPart.split(":")
                        if (creds.size == 2) {
                            return Triple(cloudName, creds[0], creds[1])
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing CLOUDINARY_URL from BuildConfig", e)
                }
            }
        }
        return Triple(CLOUD_NAME, API_KEY, API_SECRET)
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun uploadToCloudinary(
        fileBytes: ByteArray,
        isGif: Boolean = false
    ): String? = suspendCancellableCoroutine { continuation ->
        val client = OkHttpClient()
        val mimeType = if (isGif) "image/gif" else "image/jpeg"
        val fileName = if (isGif) "captured_loop.gif" else "photostrip.jpg"

        // 1. Try unsigned upload using UPLOAD_PRESET
        val unsignedBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBytes.toRequestBody(mimeType.toMediaType()))
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        val unsignedRequest = Request.Builder()
            .url(CLOUDINARY_URL)
            .post(unsignedBody)
            .build()

        client.newCall(unsignedRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Unsigned Cloudinary upload connection error: ${e.message}, trying signed fallback")
                executeSignedUpload(client, fileBytes, fileName, mimeType, continuation)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        try {
                            val json = JSONObject(bodyStr)
                            val secureUrl = json.optString("secure_url")
                            if (!secureUrl.isNullOrBlank()) {
                                Log.d(TAG, "Cloudinary unsigned upload successful: $secureUrl")
                                continuation.resume(secureUrl)
                                return
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing unsigned response", e)
                        }
                    }
                    Log.w(TAG, "Unsigned upload responded with code ${resp.code}: $bodyStr. Executing signed upload fallback...")
                    executeSignedUpload(client, fileBytes, fileName, mimeType, continuation)
                }
            }
        })
    }

    private fun executeSignedUpload(
        client: OkHttpClient,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        continuation: kotlin.coroutines.Continuation<String?>
    ) {
        val (cloudName, apiKey, apiSecret) = getCloudinaryCredentials()
        val timestamp = System.currentTimeMillis() / 1000
        val stringToSign = "timestamp=$timestamp$apiSecret"
        val signature = sha1(stringToSign)

        val signedBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBytes.toRequestBody(mimeType.toMediaType()))
            .addFormDataPart("api_key", apiKey)
            .addFormDataPart("timestamp", timestamp.toString())
            .addFormDataPart("signature", signature)
            .build()

        val signedRequest = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(signedBody)
            .build()

        client.newCall(signedRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Signed Cloudinary upload failed", e)
                continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "Signed upload unsuccessful: code=${resp.code} body=$bodyStr")
                        continuation.resume(null)
                        return
                    }

                    try {
                        val json = JSONObject(bodyStr)
                        val secureUrl = json.optString("secure_url")
                        if (!secureUrl.isNullOrBlank()) {
                            Log.d(TAG, "Signed Cloudinary upload successful: $secureUrl")
                            continuation.resume(secureUrl)
                        } else {
                            Log.e(TAG, "secure_url not found in Cloudinary response.")
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Cloudinary signed response", e)
                        continuation.resume(null)
                    }
                }
            }
        })
    }
}
