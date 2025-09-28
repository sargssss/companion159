package com.lifelover.companion159.data.remote.config

import com.lifelover.companion159.BuildConfig

object SupabaseConfig {
    val SUPABASE_URL = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    const val TABLE_INVENTORY = "inventory_items"
    const val TABLE_USERS = "profiles"

    val isConfigured: Boolean
        get() = SUPABASE_URL.isNotBlank() &&
                SUPABASE_ANON_KEY.isNotBlank() &&
                SUPABASE_URL != "\"\"" &&
                SUPABASE_ANON_KEY != "\"\""
}