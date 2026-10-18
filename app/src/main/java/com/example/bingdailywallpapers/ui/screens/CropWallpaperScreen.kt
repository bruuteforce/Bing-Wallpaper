package com.example.bingdailywallpapers.ui.screens

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropWallpaperScreen(
    imageUrl: String,
    title: String?,
    onBackClick: () -> Unit,
    onApplyWallpaper: (Bitmap, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load bitmap from URL
    LaunchedEffect(imageUrl) {
        isLoading = true
        errorMessage = null
        val result = withContext(Dispatchers.IO) {
            try {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val res = loader.execute(request)
                if (res is SuccessResult && res.drawable is BitmapDrawable) {
                    Result.success((res.drawable as BitmapDrawable).bitmap)
                } else {
                    Result.failure(IllegalStateException("Failed to load image as bitmap"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        result.fold(
            onSuccess = {
                bitmap = it
                isLoading = false
            },
            onFailure = {
                errorMessage = it.localizedMessage ?: "Failed to load image"
                isLoading = false
            }
        )
    }

    // Pan and zoom states
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Container width and height captured from BoxWithConstraints
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    // Device screen aspect ratio (standard smartphone portrait ratio 9:19.5)
    val deviceAspectRatio = 9f / 19.5f

    val performCropAndApply: (Int) -> Unit = { wallpaperFlag ->
        val bmp = bitmap
        if (bmp != null && containerWidth > 0f && containerHeight > 0f) {
            val maxCropW = containerWidth * 0.9f
            val maxCropH = containerHeight * 0.9f

            val cropW: Float
            val cropH: Float
            if (maxCropW / deviceAspectRatio <= maxCropH) {
                cropW = maxCropW
                cropH = cropW / deviceAspectRatio
            } else {
                cropH = maxCropH
                cropW = cropH * deviceAspectRatio
            }

            val cropped = computeCroppedBitmap(
                bitmap = bmp,
                containerW = containerWidth,
                containerH = containerHeight,
                cropW = cropW,
                cropH = cropH,
                scale = scale,
                offset = offset
            )
            onApplyWallpaper(cropped, wallpaperFlag)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Crop & Apply Wallpaper", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(Color.Black)
        ) {
            // Cropping canvas container with 100% unobstructed visibility (weighted)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = offset + pan
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                containerWidth = constraints.maxWidth.toFloat()
                containerHeight = constraints.maxHeight.toFloat()

                val containerW = containerWidth
                val containerH = containerHeight

                val bmp = bitmap
                if (bmp != null && containerW > 0f && containerH > 0f) {
                    val maxCropW = containerW * 0.9f
                    val maxCropH = containerH * 0.9f

                    val cropW: Float
                    val cropH: Float
                    if (maxCropW / deviceAspectRatio <= maxCropH) {
                        cropW = maxCropW
                        cropH = cropW / deviceAspectRatio
                    } else {
                        cropH = maxCropH
                        cropW = cropH * deviceAspectRatio
                    }

                    val cropLeft = (containerW - cropW) / 2f
                    val cropTop = (containerH - cropH) / 2f

                    val bitmapW = bmp.width.toFloat()
                    val bitmapH = bmp.height.toFloat()

                    val baseScale = maxOf(cropW / bitmapW, cropH / bitmapH)
                    val totalScale = baseScale * scale

                    val drawnW = bitmapW * totalScale
                    val drawnH = bitmapH * totalScale

                    val imgLeft = (containerW - drawnW) / 2f + offset.x
                    val imgTop = (containerH - drawnH) / 2f + offset.y

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawContext.canvas.save()
                        drawContext.canvas.translate(imgLeft, imgTop)
                        drawContext.canvas.scale(totalScale, totalScale)
                        drawImage(bmp.asImageBitmap())
                        drawContext.canvas.restore()

                        val overlayColor = Color.Black.copy(alpha = 0.65f)
                        // Top
                        drawRect(overlayColor, Offset(0f, 0f), Size(containerW, cropTop))
                        // Bottom
                        drawRect(overlayColor, Offset(0f, cropTop + cropH), Size(containerW, containerH - (cropTop + cropH)))
                        // Left
                        drawRect(overlayColor, Offset(0f, cropTop), Size(cropLeft, cropH))
                        // Right
                        drawRect(overlayColor, Offset(cropLeft + cropW, cropTop), Size(containerW - (cropLeft + cropW), cropH))

                        // Border
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(cropLeft, cropTop),
                            size = Size(cropW, cropH),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                } else if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "Error loading image",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Dedicated bottom action card / sheet cleanly separated below the canvas so nothing overlaps the image being cropped
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Crop & Apply Target",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { performCropAndApply(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(text = "Set Both (Home & Lock)", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { performCropAndApply(WallpaperManager.FLAG_SYSTEM) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Smartphone, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text(text = "Home Screen", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { performCropAndApply(WallpaperManager.FLAG_LOCK) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text(text = "Lock Screen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun computeCroppedBitmap(
    bitmap: Bitmap,
    containerW: Float,
    containerH: Float,
    cropW: Float,
    cropH: Float,
    scale: Float,
    offset: Offset
): Bitmap {
    val bitmapW = bitmap.width.toFloat()
    val bitmapH = bitmap.height.toFloat()

    val baseScale = maxOf(cropW / bitmapW, cropH / bitmapH)
    val totalScale = baseScale * scale

    val drawnW = bitmapW * totalScale
    val drawnH = bitmapH * totalScale

    val imgLeft = (containerW - drawnW) / 2f + offset.x
    val imgTop = (containerH - drawnH) / 2f + offset.y

    val cropLeft = (containerW - cropW) / 2f
    val cropTop = (containerH - cropH) / 2f

    val relX = cropLeft - imgLeft
    val relY = cropTop - imgTop

    val srcX = (relX / totalScale).coerceIn(0f, bitmapW)
    val srcY = (relY / totalScale).coerceIn(0f, bitmapH)
    val srcW = (cropW / totalScale).coerceAtMost(bitmapW - srcX).coerceAtLeast(1f)
    val srcH = (cropH / totalScale).coerceAtMost(bitmapH - srcY).coerceAtLeast(1f)

    val finalX = srcX.toInt().coerceIn(0, bitmap.width - 1)
    val finalY = srcY.toInt().coerceIn(0, bitmap.height - 1)
    val finalW = srcW.toInt().coerceAtMost(bitmap.width - finalX).coerceAtLeast(1)
    val finalH = srcH.toInt().coerceAtMost(bitmap.height - finalY).coerceAtLeast(1)

    return Bitmap.createBitmap(bitmap, finalX, finalY, finalW, finalH)
}
