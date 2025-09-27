package com.lifelover.companion159.network

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NetworkMonitor(private val context: Context) {

    val isOnline: Boolean
        get() = true // Заглушка

    val isOnlineFlow: Flow<Boolean> = flowOf(true) // Заглушка
}