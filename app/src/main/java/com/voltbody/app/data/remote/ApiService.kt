package com.voltbody.app.data.remote

import com.voltbody.app.data.remote.dto.*
import com.voltbody.app.domain.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────────

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponseDto>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponseDto>

    // ── Profile ───────────────────────────────────────────────────────────────

    @GET("api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<AuthResponseDto>

    @PUT("api/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<Unit>

    @POST("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<Unit>

    @POST("api/workout/sessions")
    suspend fun logWorkoutSession(
        @Header("Authorization") token: String,
        @Body request: LogSessionRequest
    ): Response<Unit>

    // ── Workout logs ──────────────────────────────────────────────────────────

    @GET("api/workout/logs")
    suspend fun getWorkoutLogs(@Header("Authorization") token: String): Response<List<WorkoutLogDto>>

    @POST("api/workout/logs/sync")
    suspend fun syncWorkoutLogs(
        @Header("Authorization") token: String,
        @Body request: SyncLogsRequest
    ): Response<Unit>

    @GET("api/workout/weight-logs")
    suspend fun getWeightLogs(@Header("Authorization") token: String): Response<List<WeightLogDto>>

    @POST("api/workout/weight-logs/sync")
    suspend fun syncWeightLogs(
        @Header("Authorization") token: String,
        @Body request: SyncWeightLogsRequest
    ): Response<Unit>

    @GET("api/workout/photos")
    suspend fun getProgressPhotos(@Header("Authorization") token: String): Response<List<ProgressPhotoDto>>

    @POST("api/workout/photos")
    suspend fun addProgressPhoto(
        @Header("Authorization") token: String,
        @Body photo: ProgressPhotoDto
    ): Response<ProgressPhotoDto>

    // ── AI ────────────────────────────────────────────────────────────────────

    @POST("api/ai/generate-plan")
    suspend fun generatePlan(
        @Header("Authorization") token: String,
        @Body request: GeneratePlanRequest
    ): Response<GeneratePlanResponse>

    @POST("api/ai/generate-alternative-meal")
    suspend fun generateAlternativeMeal(
        @Header("Authorization") token: String,
        @Body request: GenerateAlternativeMealRequest
    ): Response<Meal>

    @POST("api/ai/generate-progress-report")
    suspend fun generateProgressReport(
        @Header("Authorization") token: String,
        @Body request: ProgressReportRequest
    ): Response<ProgressReportResponse>

    @POST("api/ai/enrich-routine")
    suspend fun enrichRoutine(
        @Header("Authorization") token: String,
        @Body request: EnrichRoutineRequest
    ): Response<EnrichRoutineResponse>

    // Sprint 3 — AI Coach conversational endpoint
    @POST("api/ai/chat")
    suspend fun aiChat(
        @Header("Authorization") token: String,
        @Body request: AiChatRequest
    ): Response<AiChatResponse>

    // ── Health ────────────────────────────────────────────────────────────────

    @GET("api/health")
    suspend fun health(): Response<Map<String, String>>
}
