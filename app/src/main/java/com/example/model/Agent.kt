package com.example.model

import androidx.compose.ui.graphics.Color

data class Agent(
    val name: String,
    val role: String,
    val motto: String,
    val avatarChar: String,
    val baseColor: Color,
    val status: AgentStatus = AgentStatus.Idle
)

enum class AgentStatus {
    Idle,
    Thinking,
    Responding,
    Done
}

object AgentRoster {
    val director = Agent(
        name = "Director AI",
        role = "System Architect & Lead Coordinator",
        motto = "Breaking down complexity, coordinating distributed execution.",
        avatarChar = "💼",
        baseColor = Color(0xFF6366F1) // Indigo
    )

    val designer = Agent(
        name = "Designer AI",
        role = "Senior UX/UI Specialist",
        motto = "Elegant typography, dynamic spacing, modern dark interfaces.",
        avatarChar = "🎨",
        baseColor = Color(0xFFEC4899) // Pink
    )

    val developer = Agent(
        name = "Developer AI",
        role = "Lead Android & Kotlin Engineer",
        motto = "Solid Clean Architecture, robust local caching, high performance.",
        avatarChar = "💻",
        baseColor = Color(0xFF10B981) // Emerald Green
    )

    val tester = Agent(
        name = "QA AI",
        role = "Automated Systems & QA Engineer",
        motto = "Comprehensive Robolectric testing, coverage, boundary edge cases.",
        avatarChar = "🧪",
        baseColor = Color(0xFFF59E0B) // Amber
    )

    val marketer = Agent(
        name = "Copywriter AI",
        role = "Growth & Marketing Strategist",
        motto = "User acquisition campaigns, engaging onboarding copy, product messaging.",
        avatarChar = "✍️",
        baseColor = Color(0xFF8B5CF6) // Violet
    )

    val allAgents = listOf(director, designer, developer, tester, marketer)

    fun getByName(name: String): Agent {
        return allAgents.find { it.name.equals(name, ignoreCase = true) } ?: director
    }
}
