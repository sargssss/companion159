package com.lifelover.companion159.data.remote.config

import com.lifelover.companion159.BuildConfig

object SupabaseConfig {
    const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    const val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured: Boolean
        get() = SUPABASE_URL.isNotBlank() &&
                SUPABASE_ANON_KEY.isNotBlank() &&
                SUPABASE_URL != "\"\"" &&
                SUPABASE_ANON_KEY != "\"\""
}