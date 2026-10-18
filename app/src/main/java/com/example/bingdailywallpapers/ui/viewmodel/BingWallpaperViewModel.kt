package com.example.bingdailywallpapers.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingdailywallpapers.data.model.BingImage
import com.example.bingdailywallpapers.data.model.isFavoritedIn
import com.example.bingdailywallpapers.data.repository.BingWallpaperRepository
import com.example.bingdailywallpapers.util.WallpaperHelper
import com.example.bingdailywallpapers.util.WallpaperPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BingUiState(
    val dailyWallpaper: BingImage? = null,
    val galleryWallpapers: List<BingImage> = emptyList(),
    val favoriteWallpapers: List<BingImage> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class BingWallpaperViewModel(
    application: Application,
    private val repository: BingWallpaperRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, BingWallpaperRepository())

    private val initialFallback = getFallbackWallpapers()

    private val _favoriteWallpapers = MutableStateFlow<List<BingImage>>(emptyList())
    val favoriteWallpapers: StateFlow<List<BingImage>> = _favoriteWallpapers.asStateFlow()

    private val _uiState = MutableStateFlow(
        BingUiState(
            dailyWallpaper = initialFallback.firstOrNull(),
            galleryWallpapers = initialFallback,
            favoriteWallpapers = emptyList(),
            isLoading = false,
            isRefreshing = false,
            errorMessage = null
        )
    )
    val uiState: StateFlow<BingUiState> = _uiState.asStateFlow()

    init {
        fetchWallpapers()
    }

    fun loadFavorites(context: Context) {
        val favorites = WallpaperPreferences.getFavoriteWallpapers(context)
        _favoriteWallpapers.value = favorites
        _uiState.update { it.copy(favoriteWallpapers = favorites) }
    }

    fun toggleFavorite(context: Context, image: BingImage): Boolean {
        val isFav = WallpaperPreferences.toggleFavorite(context, image)
        loadFavorites(context)
        return isFav
    }

    fun isFavorite(image: BingImage): Boolean {
        return image.isFavoritedIn(_favoriteWallpapers.value)
    }

    fun isFavorite(context: Context, image: BingImage): Boolean {
        val inState = isFavorite(image)
        if (inState || _favoriteWallpapers.value.isNotEmpty()) {
            return inState
        }
        return WallpaperPreferences.isFavorite(context, image)
    }

    fun shareWallpaper(context: Context, image: BingImage) {
        WallpaperHelper.shareWallpaper(
            context = context,
            imageUrl = image.fullImageUrl,
            title = image.title,
            copyright = image.copyright
        )
    }

    fun fetchWallpapers(isRefresh: Boolean = false) {
        val context = getApplication<Application>()
        val market = WallpaperPreferences.getBingMarket(context)

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            if (throwable is CancellationException) return@CoroutineExceptionHandler
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = if (isRefresh) "Network error: ${throwable.localizedMessage ?: "Unable to fetch wallpapers"}. Keeping offline sample wallpapers." else null
                )
            }
        }

        viewModelScope.launch(exceptionHandler) {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            }

            val result = repository.fetchWallpapers(idx = 0, n = 20, mkt = market)
            result.fold(
                onSuccess = { images ->
                    if (images.isNotEmpty()) {
                        val daily = images.firstOrNull()
                        _uiState.update {
                            it.copy(
                                dailyWallpaper = daily,
                                galleryWallpapers = images,
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = null
                            )
                        }
                    } else {
                        // Keep fallback data smoothly without blocking UI
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = if (isRefresh) "No wallpapers returned from server. Keeping offline sample wallpapers." else null
                            )
                        }
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) return@fold
                    // Keep fallback data smoothly without blocking UI
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = if (isRefresh) "Network error: ${error.localizedMessage ?: "Unable to fetch wallpapers"}. Keeping offline sample wallpapers." else null
                        )
                    }
                }
            )
        }
    }

    fun setMarketAndFetch(market: String) {
        val context = getApplication<Application>()
        WallpaperPreferences.setBingMarket(context, market)
        fetchWallpapers(isRefresh = true)
    }

    private fun getFallbackWallpapers(): List<BingImage> {
        return listOf(
            BingImage(
                startdate = "20260330",
                fullstartdate = "202603300000",
                enddate = "20260331",
                url = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1920&q=80",
                urlbase = "",
                copyright = "Yosemite Valley, California (Unsplash)",
                copyrightlink = "",
                title = "Yosemite Valley Serenity",
                quiz = "",
                wp = true,
                hsh = "fallback_1"
            ),
            BingImage(
                startdate = "20260329",
                fullstartdate = "202603290000",
                enddate = "20260330",
                url = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1920&q=80",
                urlbase = "",
                copyright = "Misty Forest, Oregon (Unsplash)",
                copyrightlink = "",
                title = "Misty Pine Forest",
                quiz = "",
                wp = true,
                hsh = "fallback_2"
            ),
            BingImage(
                startdate = "20260328",
                fullstartdate = "202603280000",
                enddate = "20260329",
                url = "https://images.unsplash.com/photo-1426604966848-d7adacbd02bff?auto=format&fit=crop&w=1920&q=80",
                urlbase = "",
                copyright = "Alpine Mountain Range (Unsplash)",
                copyrightlink = "",
                title = "Majestic Mountain Range",
                quiz = "",
                wp = true,
                hsh = "fallback_3"
            ),
            BingImage(
                startdate = "20260327",
                fullstartdate = "202603270000",
                enddate = "20260328",
                url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1920&q=80",
                urlbase = "",
                copyright = "Tropical Beach Sunset (Unsplash)",
                copyrightlink = "",
                title = "Golden Beach Sunset",
                quiz = "",
                wp = true,
                hsh = "fallback_4"
            )
        )
    }

    fun setWallpaper(
        context: Context,
        imageUrl: String,
        target: WallpaperHelper.WallpaperTarget,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = WallpaperHelper.setWallpaperFromUrl(context, imageUrl, target)
            result.fold(
                onSuccess = { onComplete(true, null) },
                onFailure = { err -> onComplete(false, err.localizedMessage ?: "Failed to set wallpaper") }
            )
        }
    }

    /**
     * Downloads the image to cache, obtains the Uri, and launches the system crop and set intent.
     */
    fun cropAndSetWallpaper(
        context: Context,
        imageUrl: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val downloadResult = WallpaperHelper.downloadImageToCache(context, imageUrl)
            if (downloadResult.isSuccess) {
                val file = downloadResult.getOrThrow()
                val uri = WallpaperHelper.getFileProviderUri(context, file)
                val intent = WallpaperHelper.getCropAndSetWallpaperIntent(context, uri)
                try {
                    withContext(Dispatchers.Main) {
                        context.startActivity(intent)
                    }
                    onComplete(true, null)
                } catch (e: Exception) {
                    val setRes = WallpaperHelper.setWallpaperFromUrl(context, imageUrl, WallpaperHelper.WallpaperTarget.BOTH)
                    if (setRes.isSuccess) {
                        onComplete(true, null)
                    } else {
                        onComplete(false, e.localizedMessage ?: "Failed to launch crop intent")
                    }
                }
            } else {
                val setRes = WallpaperHelper.setWallpaperFromUrl(context, imageUrl, WallpaperHelper.WallpaperTarget.BOTH)
                if (setRes.isSuccess) {
                    onComplete(true, null)
                } else {
                    onComplete(false, downloadResult.exceptionOrNull()?.localizedMessage ?: "Failed to download image")
                }
            }
        }
    }

    fun downloadWallpaper(
        context: Context,
        imageUrl: String,
        title: String?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = WallpaperHelper.downloadWallpaper(context, imageUrl, title)
            result.fold(
                onSuccess = { onComplete(true, null) },
                onFailure = { err -> onComplete(false, err.localizedMessage ?: "Failed to download wallpaper") }
            )
        }
    }

    fun applyCroppedWallpaper(
        context: Context,
        bitmap: Bitmap,
        wallpaperFlag: Int,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = WallpaperHelper.setWallpaperBitmapWithFlag(context, bitmap, wallpaperFlag)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onComplete(true, null) },
                    onFailure = { err -> onComplete(false, err.localizedMessage ?: "Failed to set wallpaper") }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
