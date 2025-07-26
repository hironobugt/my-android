package com.example.glaceon.data.api

import com.example.glaceon.config.AppConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Get BASE_URL from AppConfig (which reads from BuildConfig)
    private val BASE_URL = AppConfig.API_BASE_URL

    private val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level =
                        when (AppConfig.LOG_LEVEL) {
                            AppConfig.LogLevel.NONE -> HttpLoggingInterceptor.Level.NONE
                            AppConfig.LogLevel.BASIC -> HttpLoggingInterceptor.Level.BASIC
                            AppConfig.LogLevel.HEADERS -> HttpLoggingInterceptor.Level.HEADERS
                            AppConfig.LogLevel.BODY -> HttpLoggingInterceptor.Level.BODY
                        }
            }

    private val okHttpClient =
            OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(AppConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(AppConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(AppConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build()

    private val retrofit =
            Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

    val glaceonApi: GlaceonApiService = retrofit.create(GlaceonApiService::class.java)
}
