package com.example.glaceon.data.api

import com.example.glaceon.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface GlaceonApiService {
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
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
}