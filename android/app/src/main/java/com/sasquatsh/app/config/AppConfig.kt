package com.sasquatsh.app.config

import com.sasquatsh.app.BuildConfig

object AppConfig {
    val supabaseFunctionsUrl: String = BuildConfig.SUPABASE_FUNCTIONS_URL
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY
    val googleWebClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID
    const val APP_SCHEME = "sasquatsh"
    const val WEB_DOMAIN = "sasquatsh.com"
    const val PRICING_URL = "https://$WEB_DOMAIN/pricing"
    const val BILLING_URL = "https://$WEB_DOMAIN/billing"
}
