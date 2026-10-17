package com.example.bingdailywallpapers.util

import android.content.Context
import com.example.bingdailywallpapers.data.model.BingImage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object WallpaperPreferences {
    private const val PREF_NAME = "bing_wallpaper_prefs"
    private const val KEY_AUTO_WALLPAPER = "is_auto_wallpaper_enabled"
    private const val KEY_FAVORITES = "favorite_wallpapers_json"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val KEY_THEME_MODE = "theme_mode" // "system", "dark", "light"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, BingImage::class.java)
    private val adapter = moshi.adapter<List<BingImage>>(listType)

    fun isAutoWallpaperEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_WALLPAPER, false)
    }

    fun setAutoWallpaperEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_WALLPAPER, enabled).apply()
    }

    fun getThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_MODE, "system") ?: "system"
    }

    fun setThemeMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getFavoriteWallpapers(context: Context): List<BingImage> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isFavorite(context: Context, image: BingImage): Boolean {
        val favorites = getFavoriteWallpapers(context)
        val targetId = image.fullImageUrl.ifEmpty { image.urlbase ?: image.url ?: image.hsh }
        return favorites.any {
            (it.fullImageUrl.ifEmpty { it.urlbase ?: it.url ?: it.hsh }) == targetId
        }
    }

    fun toggleFavorite(context: Context, image: BingImage): Boolean {
        val favorites = getFavoriteWallpapers(context).toMutableList()
        val targetId = image.fullImageUrl.ifEmpty { image.urlbase ?: image.url ?: image.hsh }
        val existingIndex = favorites.indexOfFirst {
            (it.fullImageUrl.ifEmpty { it.urlbase ?: it.url ?: it.hsh }) == targetId
        }
        val isFav: Boolean
        if (existingIndex >= 0) {
            favorites.removeAt(existingIndex)
            isFav = false
        } else {
            favorites.add(0, image)
            isFav = true
        }
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = adapter.toJson(favorites)
        prefs.edit().putString(KEY_FAVORITES, json).apply()
        return isFav
    }
}
