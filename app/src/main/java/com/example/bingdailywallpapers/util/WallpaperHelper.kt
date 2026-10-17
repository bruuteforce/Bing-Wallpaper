package com.example.bingdailywallpapers.util

import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object WallpaperHelper {

    enum class WallpaperTarget {
        HOME_SCREEN,
        LOCK_SCREEN,
        BOTH
    }

    /**
     * Sets the wallpaper from an image URL.
     */
    suspend fun setWallpaperFromUrl(
        context: Context,
        imageUrl: String,
        target: WallpaperTarget
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Bitmap must not be hardware config to be passed to WallpaperManager
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        setWallpaperBitmap(context, bitmap, target)
                    } else {
                        Result.failure<Unit>(IllegalStateException("Loaded image is not a BitmapDrawable"))
                    }
                } else {
                    Result.failure<Unit>(IllegalStateException("Failed to load image from URL: $imageUrl"))
                }
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }
        }
    }

    /**
     * Sets the wallpaper from a Bitmap.
     */
    fun setWallpaperBitmap(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperTarget
    ): Result<Unit> {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            when (target) {
                WallpaperTarget.HOME_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                WallpaperTarget.LOCK_SCREEN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                WallpaperTarget.BOTH -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure<Unit>(e)
        }
    }

    /**
     * Sets the wallpaper from a Bitmap with raw wallpaperFlag.
     */
    fun setWallpaperBitmapWithFlag(
        context: Context,
        bitmap: Bitmap,
        wallpaperFlag: Int
    ): Result<Unit> {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, wallpaperFlag)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the wallpaper image to public Pictures directory.
     */
    suspend fun downloadWallpaper(
        context: Context,
        imageUrl: String,
        title: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        val safeTitle = title?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "Wallpaper"
                        val filename = "Bing_${safeTitle}_${System.currentTimeMillis()}.jpg"

                        val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val resolver = context.contentResolver
                            val contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BingWallpapers")
                            }
                            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            uri?.let { resolver.openOutputStream(it) }
                        } else {
                            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            val appDir = File(imagesDir, "BingWallpapers")
                            if (!appDir.exists()) appDir.mkdirs()
                            val imageFile = File(appDir, filename)
                            FileOutputStream(imageFile)
                        }

                        if (fos != null) {
                            fos.use { fosStream ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fosStream)
                            }
                            Result.success(Unit)
                        } else {
                            Result.failure<Unit>(IllegalStateException("Failed to open output stream for download"))
                        }
                    } else {
                        Result.failure<Unit>(IllegalStateException("Loaded image is not a BitmapDrawable"))
                    }
                } else {
                    Result.failure<Unit>(IllegalStateException("Failed to load image for download"))
                }
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }
        }
    }

    /**
     * Helper method 1: Downloads/caches image to a local file in cacheDir.
     */
    suspend fun downloadImageToCache(
        context: Context,
        imageUrl: String
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        val cacheFile = File(context.cacheDir, "wallpaper_crop_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(cacheFile).use { fos ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                        }
                        Result.success(cacheFile)
                    } else {
                        Result.failure(IllegalStateException("Loaded image is not a BitmapDrawable"))
                    }
                } else {
                    Result.failure(IllegalStateException("Failed to load image from URL: $imageUrl"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Helper method 2: Generates a FileProvider Uri for the local file.
     */
    fun getFileProviderUri(
        context: Context,
        file: File
    ): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Helper method 3: Creates the crop and set wallpaper intent using WallpaperManager.getCropAndSetWallpaperIntent(uri).
     */
    fun getCropAndSetWallpaperIntent(
        context: Context,
        uri: Uri
    ): Intent {
        val wallpaperManager = WallpaperManager.getInstance(context)
        return wallpaperManager.getCropAndSetWallpaperIntent(uri).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Launches the native wallpaper crop and set activity using helper methods.
     */
    suspend fun cropAndSetWallpaper(
        context: Context,
        imageUrl: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val downloadResult = downloadImageToCache(context, imageUrl)
            if (downloadResult.isSuccess) {
                val file = downloadResult.getOrThrow()
                val uri = getFileProviderUri(context, file)
                val intent = getCropAndSetWallpaperIntent(context, uri)
                try {
                    withContext(Dispatchers.Main) {
                        context.startActivity(intent)
                    }
                    Result.success(Unit)
                } catch (e: Exception) {
                    // Fallback to direct setting
                    val loader = Coil.imageLoader(context)
                    val request = ImageRequest.Builder(context).data(imageUrl).allowHardware(false).build()
                    val result = loader.execute(request)
                    if (result is SuccessResult && result.drawable is BitmapDrawable) {
                        setWallpaperBitmap(context, (result.drawable as BitmapDrawable).bitmap, WallpaperTarget.BOTH)
                    } else {
                        Result.failure(e)
                    }
                }
            } else {
                Result.failure(downloadResult.exceptionOrNull() ?: IllegalStateException("Failed to download image to cache"))
            }
        }
    }

    /**
     * Shares wallpaper image and details with friends using Intent.ACTION_SEND.
     */
    fun shareWallpaper(
        context: Context,
        imageUrl: String,
        title: String?,
        copyright: String?
    ) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title ?: "Bing Daily Wallpaper")
            putExtra(Intent.EXTRA_TEXT, "Check out this amazing wallpaper: ${title ?: "Bing Wallpaper"}\n${copyright ?: ""}\n$imageUrl")
        }
        val shareIntent = Intent.createChooser(sendIntent, null).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(shareIntent)
    }
}
