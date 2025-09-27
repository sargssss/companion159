package com.lifelover.companion159.data.remote.api

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import com.lifelover.companion159.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.Auth
import javax.inject.Singleton

@Singleton
object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = SupabaseConfig.SUPABASE_URL,
        supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        //install(Realtime)
    }
}