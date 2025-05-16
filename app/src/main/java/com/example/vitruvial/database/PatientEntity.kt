package com.example.vitruvial.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.vitruvial.PatientRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Database entity class for patient records
 */
@Entity(tableName = "patients")
@TypeConverters(Converters::class)
data class PatientEntity(
    @PrimaryKey
    val patientId: String,
    val patientName: String,
    val extractedInfo: Map<String, String>,
    val creationDate: Date
)

/**
 * Database entity class for billing codes
 */
@Entity(
    tableName = "billing_codes",
    primaryKeys = ["patientId", "code", "date"]
)
@TypeConverters(Converters::class)
data class BillingCodeEntity(
    val patientId: String,
    val code: String,
    val date: Date,
    val referringDoctor: String = "",
    val emailedDate: Date? = null,
    val emailedTo: String? = null
)

/**
 * Type converters for Room database
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromMapString(value: String?): Map<String, String> {
        if (value == null) {
            return emptyMap()
        }
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }
    
    @TypeConverter
    fun fromMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }
} 