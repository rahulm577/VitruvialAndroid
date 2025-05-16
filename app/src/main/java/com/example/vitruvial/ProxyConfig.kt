package com.example.vitruvial

/**
 * Configuration for the proxy server that handles API requests to Claude
 * This avoids exposing the API key in the client application
 */
object ProxyConfig {
    // Update with your server's URL
    const val PROXY_BASE_URL = "https://your-domain.com/api/"
    
    // Must match APP_TOKEN in your server's .env file
    const val APP_TOKEN = "app_token_123456"
} 