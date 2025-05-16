package com.example.vitruvial.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SupportFactory

/**
 * Main database class for the application
 */
@Database(
    entities = [PatientEntity::class, BillingCodeEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun patientDao(): PatientDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Get or create the database instance
         * @param password Encryption password for SQLCipher
         */
        fun getInstance(context: Context, password: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = createDatabase(context, password)
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Create an encrypted database with SQLCipher
         */
        private fun createDatabase(context: Context, password: String): AppDatabase {
            // Create a SupportFactory with the password to configure database encryption
            val passphrase = password.toByteArray()
            val factory = SupportFactory(passphrase)
            
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "patient_records_database"
            )
            .fallbackToDestructiveMigration() // For simplicity - in production you'd use a proper migration strategy
            .openHelperFactory(factory) // Apply encryption with SQLCipher
            .build()
        }
    }
} 