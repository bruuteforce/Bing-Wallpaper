package com.example.bingdailywallpapers.data.network

import com.example.bingdailywallpapers.data.model.BingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BingApiService {
    @GET("HPImageArchive.aspx")
    suspend fun getWallpapers(
        @Query("format") format: String = "js",
        @Query("idx") idx: Int = 0,
        @Query("n") n: Int = 20,
        @Query("mkt") mkt: String = "en-IN"
    ): BingResponse

    companion object {
        const val BASE_URL = "https://www.bing.com/"
    }
}
