package com.voltbody.app.data.remote.dto

import com.squareup.moshi.JsonClass
import com.voltbody.app.domain.model.UserProfile
import com.voltbody.app.domain.model.WorkoutDay
import com.voltbody.app.domain.model.WorkoutLog

@JsonClass(generateAdapter = true)
data class AiChatMessage(
    val role: String,   // "user" | "assistant"
    val content: String
)

@JsonClass(generateAdapter = true)
data class AiChatContext(
    val profile: UserProfile? = null,
    val todayWorkout: WorkoutDay? = null,
    val recentLogs: List<WorkoutLog> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiChatRequest(
    val messages: List<AiChatMessage>,
    val context: AiChatContext
)

@JsonClass(generateAdapter = true)
data class AiChatResponse(
    val reply: String,
    val tokensUsed: Int? = null
)
