package com.lifelover.companion159.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Uav(
    val id: Int,
    val title: String,
    val sn: String,
    val crewId: Int
)