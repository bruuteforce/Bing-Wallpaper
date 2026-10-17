package com.example.bingdailywallpapers.data.repository

import com.example.bingdailywallpapers.data.model.BingImage
import com.example.bingdailywallpapers.data.network.BingApiClient
import com.example.bingdailywallpapers.data.network.BingApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BingWallpaperRepository(
    private val apiService: BingApiService = BingApiClient.apiService
) {
    suspend fun fetchWallpapers(idx: Int = 0, n: Int = 20, mkt: String = "en-US"): Result<List<BingImage>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWallpapers(idx = idx, n = n, mkt = mkt)
                val images = response.images ?: emptyList()
                Result.success(images)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
