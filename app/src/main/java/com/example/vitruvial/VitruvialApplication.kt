package com.example.vitruvial

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.vitruvial.database.AppDatabase
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.sqlcipher.database.SQLiteDatabase
import java.security.SecureRandom

class VitruvialApplication : Application() {
    
    // Application-wide coroutine scope for database operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Lazy initialization of database
    val database by lazy { AppDatabase.getInstance(this, getDatabasePassword()) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize SQLCipher
        SQLiteDatabase.loadLibs(this)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Fetch remote config
        applicationScope.launch {
            RemoteConfigManager.fetchAndActivate()
        }
        
        // Initialize the database in the background
        applicationScope.launch {
            // This ensures the database is created when the app starts
            database.openHelper.writableDatabase
            
            // Load patient data from database into memory
            PatientService.loadFromDatabase(database.patientDao())
        }
    }
    
    /**
     * Get database encryption password from secure storage or generate a new one
     */
    private fun getDatabasePassword(): String {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        val sharedPreferences = try {
            EncryptedSharedPreferences.create(
                this,
                "secure_database_settings",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences for debugging only
            // In production, you would want to handle this differently
            return generateAndStoreNewDatabasePassword()
        }
        
        var password = sharedPreferences.getString("database_encryption_key", "")
        
        if (password.isNullOrEmpty()) {
            password = generateAndStoreNewDatabasePassword()
        }
        
        return password
    }
    
    /**
     * Generate a secure random password for database encryption
     */
    private fun generateAndStoreNewDatabasePassword(): String {
        // Generate a secure random password
        val random = SecureRandom()
        val passwordBytes = ByteArray(32) // 256 bits
        random.nextBytes(passwordBytes)
        
        // Convert to hexadecimal string
        val password = passwordBytes.joinToString("") { "%02x".format(it) }
        
        // Store in encrypted shared preferences
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "secure_database_settings",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            sharedPreferences.edit().putString("database_encryption_key", password).apply()
        } catch (e: Exception) {
            // If we can't store in encrypted preferences, this is a serious issue
            // In a production app, you'd want to handle this with more robust error handling
        }
        
        return password
    }
} 