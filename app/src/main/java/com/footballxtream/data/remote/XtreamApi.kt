package com.footballxtream.data.remote

import com.footballxtream.data.remote.dto.LiveCategoryDto
import com.footballxtream.data.remote.dto.LiveStreamDto
import com.footballxtream.data.remote.dto.LoginResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {

    @GET("player_api.php")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String,
    ): LoginResponse

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories",
    ): List<LiveCategoryDto>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null,
        @Query("action") action: String = "get_live_streams",
    ): List<LiveStreamDto>
}
