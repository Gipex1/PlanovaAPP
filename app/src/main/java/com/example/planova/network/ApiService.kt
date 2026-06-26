package com.example.planova.network

import com.example.planova.data.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("api/users/register")
    fun register(@Body request: RegisterRequest): Call<Map<String, Long>>

    @POST("api/users/login")
    fun login(@Body request: LoginRequest): Call<Map<String, Long>>

    @GET("api/users/me")
    fun getMe(@Header("X-User-Id") userId: Long): Call<UserResponse>

    @PUT("api/users/me")
    fun updateMe(@Header("X-User-Id") userId: Long, @Body request: UpdateUserRequest): Call<UserResponse>

    @DELETE("api/users/me")
    fun deleteMe(@Header("X-User-Id") userId: Long): Call<Map<String, String>>

    @POST("api/plans/generate")
    fun generate(@Body request: GenerateRequest): Call<GenerateResponse>

    @POST("api/plans/generate")
    fun generate(
        @Header("X-User-Id") userId: Long,
        @Body request: GenerateRequest
    ): Call<GenerateResponse>

    @POST("api/plans")
    fun savePlan(@Header("X-User-Id") userId: Long, @Body plan: PlanRequest): Call<PlanResponse>

    @GET("api/plans")
    fun getPlans(@Header("X-User-Id") userId: Long): Call<List<PlanResponse>>

    @GET("api/plans/{planId}")
    fun getPlan(@Header("X-User-Id") userId: Long, @Path("planId") planId: Long): Call<PlanResponse>

    @PUT("api/plans/{planId}")
    fun updatePlan(@Header("X-User-Id") userId: Long, @Path("planId") planId: Long, @Body request: UpdatePlanRequest): Call<PlanResponse>

    @DELETE("api/plans/{planId}")
    fun deletePlan(@Header("X-User-Id") userId: Long, @Path("planId") planId: Long): Call<Map<String, String>>

    @POST("api/users/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<Map<String, String>>

    @PUT("api/plans/{planId}/steps/{stepId}")
    fun updateStep(@Header("X-User-Id") userId: Long, @Path("planId") planId: Long, @Path("stepId") stepId: Long, @Body request: UpdateStepRequest): Call<StepResponse>
}