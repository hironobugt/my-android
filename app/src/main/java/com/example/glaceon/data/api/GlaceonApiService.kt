package com.example.glaceon.data.api

import com.example.glaceon.data.model.*
import com.example.glaceon.config.AppConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

interface GlaceonApiService {
    
    @POST("auth")
    @Headers("Content-Type: application/json")
    suspend fun auth(@Body request: AuthRequest): AuthResponse
    
    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>
    
    @POST("auth/register")
    suspend fun resendConfirmationCode(@Body request: AuthRequest): Response<AuthResponse>
    
    @POST("auth/register")
    suspend fun forgotPassword(@Body request: AuthRequest): Response<AuthResponse>
    
    @POST("auth/register")
    suspend fun resetPassword(@Body request: AuthRequest): Response<AuthResponse>
    
    @POST("archive/upload")
    suspend fun uploadFile(
        @Header("Authorization") token: String,
        @Body request: UploadRequest
    ): Response<UploadResponse>
    
    @GET("archive/list")
    suspend fun getArchiveList(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50,
        @Query("continuationToken") continuationToken: String? = null
    ): Response<ArchiveListResponse>
    
    @GET("archive/{archiveId}")
    suspend fun getArchive(
        @Header("Authorization") token: String,
        @Path("archiveId") archiveId: String
    ): Response<RestoreResponse>
    
    @DELETE("archive/{archiveId}")
    suspend fun deleteArchive(
        @Header("Authorization") token: String,
        @Path("archiveId") archiveId: String
    ): Response<Unit>
    
    @GET("archive/{archiveId}/thumbnail")
    suspend fun getThumbnail(
        @Header("Authorization") token: String,
        @Path("archiveId") archiveId: String
    ): Response<okhttp3.ResponseBody>
    
    // Billing API endpoints
    @POST("billing")
    suspend fun billingAction(
        @Header("Authorization") token: String,
        @Body request: BillingRequest
    ): Response<BillingResponse>
    
    @POST("usage")
    suspend fun getUsage(
        @Header("Authorization") token: String,
        @Body request: UsageRequest
    ): Response<UsageResponse>
    
    companion object {
        fun create(): GlaceonApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (AppConfig.IS_DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(AppConfig.API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(GlaceonApiService::class.java)
        }
    }
}