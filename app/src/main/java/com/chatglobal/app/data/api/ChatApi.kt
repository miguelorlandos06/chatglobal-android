package com.chatglobal.app.data.api

import com.chatglobal.app.data.model.*
import retrofit2.http.*

interface ChatApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): MeResponse
}
