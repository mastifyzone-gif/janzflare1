package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.FlareOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageCropRotateDialog(
    sourceUri: Uri,
    isCircularAvatar: Boolean,
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var sourceBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadError by remember(sourceUri) { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var rotationDeg by remember(sourceUri) { mutableIntStateOf(0) }
    var zoom by remember(sourceUri) { mutableFloatStateOf(1f) }
    var panOffset by remember(sourceUri) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            runCatching { decodeNormalizedBitmap(context, sourceUri, maxDim = 1600) }
                .onSuccess { bmp -> sourceBitmap = bmp }
                .onFailure { err -> loadError = err.localizedMessage ?: "Failed to load image" }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D14)),
            color = Color(0xFF0B0D14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (!isSaving) onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = if (isCircularAvatar) "Adjust Profile Photo" else "Adjust Channel Cover",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    TextButton(
                        onClick = {
                            rotationDeg = 0
                            zoom = 1f
                            panOffset = Offset.Zero
                        },
                        enabled = !isSaving
                    ) {
                        Text("Reset", color = FlareOrange)
                    }
                }

                Spacer(Modifier.height(8.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .background(Color(0xFF131622), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val containerW = with(density) { maxWidth.toPx() }
                    val containerH = with(density) { maxHeight.toPx() }
                    val cropRatio = if (isCircularAvatar) 1f else 2.5f

                    val cropW = if (isCircularAvatar) {
                        min(containerW, containerH) * 0.78f
                    } else {
                        containerW * 0.90f
                    }
                    val cropH = if (isCircularAvatar) cropW else (cropW / cropRatio).coerceAtMost(containerH * 0.75f)
                    val cropRect = Rect(
                        left = (containerW - cropW) / 2f,
                        top = (containerH - cropH) / 2f,
                        right = (containerW + cropW) / 2f,
                        bottom = (containerH + cropH) / 2f
                    )

                    val bmp = sourceBitmap
                    when {
                        loadError != null -> {
                            Text(
                                text = loadError.orEmpty(),
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                        bmp == null -> {
                            CircularProgressIndicator(color = FlareOrange)
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, gestureZoom, _ ->
                                            val newZoom = (zoom * gestureZoom).coerceIn(1f, 3.5f)
                                            zoom = newZoom
                                            val maxPanX = cropW * 0.85f * newZoom
                                            val maxPanY = cropH * 0.85f * newZoom
                                            panOffset = Offset(
                                                x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                                y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Crop preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            translationX = panOffset.x
                                            translationY = panOffset.y
                                            scaleX = zoom
                                            scaleY = zoom
                                            rotationZ = rotationDeg.toFloat()
                                        }
                                )

                                // Dimmed overlay outside the crop window
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cutoutPath = Path().apply {
                                        if (isCircularAvatar) {
                                            addOval(cropRect)
                                        } else {
                                            addRoundRect(
                                                androidx.compose.ui.geometry.RoundRect(
                                                    rect = cropRect,
                                                    cornerRadius = CornerRadius(24f, 24f)
                                                )
                                            )
                                        }
                                    }
                                    clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                                        drawRect(Color.Black.copy(alpha = 0.65f))
                                    }
                                    if (isCircularAvatar) {
                                        drawOval(
                                            color = Color.White,
                                            topLeft = cropRect.topLeft,
                                            size = Size(cropRect.width, cropRect.height),
                                            style = Stroke(width = 3f)
                                        )
                                    } else {
                                        drawRoundRect(
                                            color = Color.White,
                                            topLeft = cropRect.topLeft,
                                            size = Size(cropRect.width, cropRect.height),
                                            cornerRadius = CornerRadius(24f, 24f),
                                            style = Stroke(width = 3f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom controls + save button capture geometry cleanly
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pinch or drag to adjust • Tap rotate to turn",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Zoom Slider + Rotate Controls
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Zoom", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.width(10.dp))
                        Slider(
                            value = zoom,
                            onValueChange = { zoom = it },
                            valueRange = 1f..3.5f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            String.format(java.util.Locale.US, "%.1fx", zoom),
                            style = MaterialTheme.typography.labelMedium,
                            color = FlareOrange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { rotationDeg = (rotationDeg - 90 + 360) % 360 },
                            enabled = sourceBitmap != null && !isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RotateLeft, null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Rotate -90°", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { rotationDeg = (rotationDeg + 90) % 360 },
                            enabled = sourceBitmap != null && !isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RotateRight, null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Rotate +90°", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val densityLocal = LocalDensity.current
                        val approxContainerW = with(densityLocal) { maxWidth.toPx() }
                        val approxContainerH = approxContainerW * 1.15f
                        val cropRatio = if (isCircularAvatar) 1f else 2.5f
                        val cropW = if (isCircularAvatar) {
                            min(approxContainerW, approxContainerH) * 0.78f
                        } else {
                            approxContainerW * 0.90f
                        }
                        val cropH = if (isCircularAvatar) cropW else cropW / cropRatio

                        Button(
                            onClick = {
                                val bmp = sourceBitmap ?: return@Button
                                isSaving = true
                                scope.launch {
                                    val croppedUri = withContext(Dispatchers.IO) {
                                        renderCroppedToCacheFile(
                                            context = context,
                                            source = bmp,
                                            rotationDeg = rotationDeg,
                                            zoom = zoom,
                                            panOffset = panOffset,
                                            previewW = approxContainerW.coerceAtLeast(100f),
                                            previewH = approxContainerH.coerceAtLeast(100f),
                                            cropW = cropW.coerceAtLeast(80f),
                                            cropH = cropH.coerceAtLeast(40f),
                                            outWidth = if (isCircularAvatar) 720 else 1440,
                                            outHeight = if (isCircularAvatar) 720 else 576
                                        )
                                    }
                                    isSaving = false
                                    onCropped(croppedUri)
                                }
                            },
                            enabled = sourceBitmap != null && !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = FlareOrange),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Applying...", color = Color.White)
                            } else {
                                Icon(Icons.Default.Check, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Apply & Use Photo", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodeNormalizedBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    val rawMax = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    var sampleSize = 1
    while (rawMax / sampleSize > maxDim * 2) sampleSize *= 2

    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: throw IllegalStateException("Could not open selected image")

    val exifRotation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)

    return if (exifRotation != 0f) {
        val m = Matrix().apply { postRotate(exifRotation) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        if (rotated !== decoded) decoded.recycle()
        rotated
    } else {
        decoded
    }
}

private fun renderCroppedToCacheFile(
    context: Context,
    source: Bitmap,
    rotationDeg: Int,
    zoom: Float,
    panOffset: Offset,
    previewW: Float,
    previewH: Float,
    cropW: Float,
    cropH: Float,
    outWidth: Int,
    outHeight: Int
): Uri {
    val rotMatrix = Matrix().apply { postRotate(rotationDeg.toFloat()) }
    val rotated = if (rotationDeg % 360 != 0) {
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, rotMatrix, true)
    } else {
        source
    }

    // Match Image(..., ContentScale.Fit) inside preview box of size (previewW, previewH)
    val fitScale = min(previewW / rotated.width.toFloat(), previewH / rotated.height.toFloat())
    val totalPreviewScale = fitScale * zoom

    val destBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(destBitmap)
    canvas.drawColor(android.graphics.Color.BLACK)

    val outScaleX = outWidth.toFloat() / cropW
    val outScaleY = outHeight.toFloat() / cropH
    val outScale = (outScaleX + outScaleY) / 2f

    val drawMatrix = Matrix().apply {
        postTranslate(-rotated.width / 2f, -rotated.height / 2f)
        postScale(totalPreviewScale * outScale, totalPreviewScale * outScale)
        postTranslate(
            outWidth / 2f + panOffset.x * outScale,
            outHeight / 2f + panOffset.y * outScale
        )
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(rotated, drawMatrix, paint)
    if (rotated !== source) rotated.recycle()

    val outFile = File(context.cacheDir, "jf_cropped_${UUID.randomUUID()}.jpg")
    FileOutputStream(outFile).use { stream ->
        destBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    }
    destBitmap.recycle()
    return Uri.fromFile(outFile)
}
