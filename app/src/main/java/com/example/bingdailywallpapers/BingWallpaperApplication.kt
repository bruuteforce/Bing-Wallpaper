package com.example.bingdailywallpapers

import android.app.Application
import android.content.pm.ApplicationInfo
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger

class BingWallpaperApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB disk cache for offline persistence and smooth scrolling
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .apply {
                if (isDebug) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
