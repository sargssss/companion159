package com.lifelover.companion159.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Equipment(
    val id: Int,
    val title: String,
    val amount: Int,
    val crewId: Int
)