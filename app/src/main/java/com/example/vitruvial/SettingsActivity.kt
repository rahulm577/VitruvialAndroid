package com.example.vitruvial

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.vitruvial.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Load existing API key if available
        val apiKey = getApiKey()
        if (apiKey.isNotEmpty()) {
            binding.apiKeyInput.setText(apiKey)
        }
        
        binding.saveButton.setOnClickListener {
            val newApiKey = binding.apiKeyInput.text.toString().trim()
            if (newApiKey.isEmpty()) {
                binding.apiKeyInputLayout.error = "API key cannot be empty"
                return@setOnClickListener
            }
            
            // Save API key securely
            saveApiKey(newApiKey)
            Toast.makeText(this, "API key saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun getEncryptedSharedPreferences() = try {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            this,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences for debugging only
        // In production, you would want to handle this error differently
        Toast.makeText(this, "Error creating secure storage", Toast.LENGTH_LONG).show()
        getSharedPreferences("app_settings", MODE_PRIVATE)
    }
    
    private fun getApiKey(): String {
        return getEncryptedSharedPreferences().getString("anthropic_api_key", "") ?: ""
    }
    
    private fun saveApiKey(apiKey: String) {
        getEncryptedSharedPreferences().edit().putString("anthropic_api_key", apiKey).apply()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 