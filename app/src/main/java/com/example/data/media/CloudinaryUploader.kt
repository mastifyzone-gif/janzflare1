package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

object CloudinaryUploader {

    private const val MAX_FREE_VIDEO_BYTES = 100L * 1024L * 1024L // 100 MB (Cloudinary Free plan unsigned limit)
    private const val MAX_FREE_IMAGE_BYTES = 10L * 1024L * 1024L  // 10 MB

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .build()
    }

    fun isConfigured(): Boolean =
        BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank() &&
            BuildConfig.CLOUDINARY_UPLOAD_PRESET.isNotBlank()

    fun buildCompatibleVideoUrl(url: String): String? {
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://res.cloudinary.com/")) return null
        val marker = "/video/upload/"
        val idx = trimmed.indexOf(marker)
        if (idx == -1) return null
        val prefix = trimmed.substring(0, idx + marker.length)
        val rest = trimmed.substring(idx + marker.length)
        if (rest.contains("vc_h264")) return null
        val normalizedRest = rest.replace(Regex("\\.[a-zA-Z0-9]+$"), ".mp4")
        return "${prefix}c_limit,w_1280,h_1280,vc_h264,f_mp4,q_auto/$normalizedRest"
    }

    fun formatBytesAsMb(bytes: Long): String {
        if (bytes <= 0L) return "0.0"
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f", mb)
    }

    suspend fun uploadVideoUri(
        context: Context,
        uri: Uri,
        folder: String = "janzflare/videos",
        onProgress: (bytesUploaded: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): String = withContext(Dispatchers.IO) {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
        require(cloudName.isNotBlank() && preset.isNotBlank()) {
            "Video upload service is temporarily unavailable. Please try again later."
        }

        val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
        val fileName = resolveFileName(context, uri, "video_${System.currentTimeMillis()}.mp4")
        val contentLength = resolveFileSize(context, uri)
        if (contentLength > MAX_FREE_VIDEO_BYTES) {
            val sizeMb = (contentLength / (1024.0 * 1024.0)).roundToInt()
            throw IllegalArgumentException(
                "Video size is ${sizeMb} MB. Maximum supported size is 100 MB per video — please trim or compress it."
            )
        }

        val streamingBody = object : RequestBody() {
            override fun contentType() = mimeType.toMediaType()
            override fun contentLength(): Long = if (contentLength > 0L) contentLength else -1L

            override fun writeTo(sink: BufferedSink) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Unable to read selected video")
                input.use { stream ->
                    val buffer = ByteArray(16 * 1024)
                    var uploaded = 0L
                    var lastReportedPct = -1
                    var lastReportedBytes = 0L
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                        uploaded += read
                        if (contentLength > 0L) {
                            val pct = ((uploaded * 100L) / contentLength).toInt().coerceIn(0, 100)
                            if (pct != lastReportedPct || uploaded - lastReportedBytes >= 256 * 1024L || uploaded >= contentLength) {
                                lastReportedPct = pct
                                lastReportedBytes = uploaded
                                onProgress(uploaded, contentLength)
                            }
                        } else if (uploaded - lastReportedBytes >= 256 * 1024L) {
                            lastReportedBytes = uploaded
                            onProgress(uploaded, uploaded)
                        }
                    }
                    onProgress(uploaded, if (contentLength > 0L) contentLength else uploaded)
                }
            }
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", preset)
            .addFormDataPart("folder", folder)
            .addFormDataPart("file", fileName, streamingBody)
            .build()

        executeUpload(
            url = "https://api.cloudinary.com/v1_1/$cloudName/video/upload",
            requestBody = body
        )
    }

    suspend fun uploadImageUri(
        context: Context,
        uri: Uri,
        folder: String,
        maxDimPx: Int = 1280
    ): String = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "jf_upload_${UUID.randomUUID()}.jpg")
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val srcMax = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (srcMax / sampleSize > maxDimPx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: throw IllegalStateException("Unable to read selected image")

            val scaled = if (max(decoded.width, decoded.height) > maxDimPx) {
                val ratio = maxDimPx.toFloat() / max(decoded.width, decoded.height).toFloat()
                val targetW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            FileOutputStream(tempFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            scaled.recycle()

            if (tempFile.length() > MAX_FREE_IMAGE_BYTES) {
                throw IllegalArgumentException("Image exceeds 10 MB upload limit")
            }
            uploadImageFile(tempFile, folder)
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    suspend fun uploadImageFile(
        file: File,
        folder: String
    ): String = withContext(Dispatchers.IO) {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
        require(cloudName.isNotBlank() && preset.isNotBlank()) {
            "Image upload service is temporarily unavailable. Please try again later."
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", preset)
            .addFormDataPart("folder", folder)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
            .build()

        executeUpload(
            url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload",
            requestBody = body
        )
    }

    private fun executeUpload(url: String, requestBody: RequestBody): String {
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (!response.isSuccessful) {
                val apiMsg = json?.optJSONObject("error")?.optString("message").orEmpty()
                throw IllegalStateException(
                    apiMsg.ifBlank { "Cloud upload failed (HTTP ${response.code})" }
                )
            }
            val secureUrl = json?.optString("secure_url").orEmpty()
            require(secureUrl.startsWith("https://")) {
                "Cloud upload did not return a valid HTTPS URL"
            }
            return secureUrl
        }
    }

    private fun resolveFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && c.moveToFirst() && !c.isNull(idx)) {
                return c.getLong(idx)
            }
        }
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }.getOrDefault(-1L)
    }

    private fun resolveFileName(context: Context, uri: Uri, fallback: String): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) {
                val name = c.getString(idx)
                if (!name.isNullOrBlank()) return name
            }
        }
        return fallback
    }
}
