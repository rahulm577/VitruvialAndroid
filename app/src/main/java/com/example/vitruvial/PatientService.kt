package com.example.vitruvial

import com.example.vitruvial.database.BillingCodeEntity
import com.example.vitruvial.database.PatientDao
import com.example.vitruvial.database.PatientEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Service for managing patient records
 */
object PatientService {
    
    // In-memory storage of patient records
    private val patientRecords = mutableMapOf<String, PatientRecord>()
    
    // Current active patient (the one being processed)
    private var currentPatientId: String? = null
    
    // Coroutine scope for database operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Database DAO reference
    private var patientDao: PatientDao? = null
    
    /**
     * Load patient records from database into memory
     */
    fun loadFromDatabase(dao: PatientDao) {
        patientDao = dao
        
        serviceScope.launch {
            val patientsWithBillingCodes = dao.getAllPatientsWithBillingCodes()
            
            synchronized(patientRecords) {
                patientRecords.clear()
                
                for (patientWithCodes in patientsWithBillingCodes) {
                    val patientEntity = patientWithCodes.patient
                    val billingCodes = patientWithCodes.billingCodes.map { entity ->
                        BillingCode(
                            code = entity.code,
                            date = entity.date,
                            referringDoctor = entity.referringDoctor,
                            emailedDate = entity.emailedDate,
                            emailedTo = entity.emailedTo
                        )
                    }.toMutableList()
                    
                    val patientRecord = PatientRecord(
                        patientId = patientEntity.patientId,
                        patientName = patientEntity.patientName,
                        extractedInfo = patientEntity.extractedInfo,
                        billingCodes = billingCodes,
                        creationDate = patientEntity.creationDate
                    )
                    
                    patientRecords[patientEntity.patientId] = patientRecord
                }
            }
        }
    }
    
    /**
     * Create a new patient record from extracted information
     */
    fun createPatientRecord(patientName: String, extractedInfo: Map<String, String>): String {
        // Check if a matching patient already exists
        val existingPatient = findExistingPatient(extractedInfo)
        
        if (existingPatient != null) {
            // Use the existing patient ID
            currentPatientId = existingPatient.patientId
            return existingPatient.patientId
        }
        
        // Create a new patient record
        val patientId = UUID.randomUUID().toString()
        val creationDate = Date()
        val patientRecord = PatientRecord(
            patientId = patientId,
            patientName = patientName,
            extractedInfo = extractedInfo,
            creationDate = creationDate
        )
        
        patientRecords[patientId] = patientRecord
        currentPatientId = patientId
        
        // Save to database
        savePatientToDatabase(patientRecord)
        
        return patientId
    }
    
    /**
     * Save a patient record to the database
     */
    private fun savePatientToDatabase(patientRecord: PatientRecord) {
        patientDao?.let { dao ->
            serviceScope.launch {
                // Create patient entity
                val patientEntity = PatientEntity(
                    patientId = patientRecord.patientId,
                    patientName = patientRecord.patientName,
                    extractedInfo = patientRecord.extractedInfo,
                    creationDate = patientRecord.creationDate
                )
                
                // Insert/update patient
                dao.insertPatient(patientEntity)
                
                // Insert/update billing codes
                if (patientRecord.billingCodes.isNotEmpty()) {
                    val billingCodeEntities = patientRecord.billingCodes.map { code ->
                        BillingCodeEntity(
                            patientId = patientRecord.patientId,
                            code = code.code,
                            date = code.date,
                            referringDoctor = code.referringDoctor,
                            emailedDate = code.emailedDate,
                            emailedTo = code.emailedTo
                        )
                    }
                    dao.insertBillingCodes(billingCodeEntities)
                }
            }
        }
    }
    
    /**
     * Find an existing patient with matching identifying information
     */
    private fun findExistingPatient(extractedInfo: Map<String, String>): PatientRecord? {
        // Create a temporary patient record for comparison
        val tempPatient = PatientRecord(
            patientId = "",
            patientName = "",
            extractedInfo = extractedInfo
        )
        
        // Check all existing patients for a match
        return patientRecords.values.find { it.isSamePatient(tempPatient) }
    }
    
    /**
     * Add billing codes to the current patient
     */
    fun addBillingCodesToCurrentPatient(billingCodes: List<BillingCode>) {
        currentPatientId?.let { patientId ->
            patientRecords[patientId]?.let { patientRecord ->
                patientRecord.billingCodes.addAll(billingCodes)
                
                // Save updated billing codes to database
                saveBillingCodesToDatabase(patientId, billingCodes)
            }
        }
    }
    
    /**
     * Save billing codes to the database
     */
    private fun saveBillingCodesToDatabase(patientId: String, billingCodes: List<BillingCode>) {
        patientDao?.let { dao ->
            serviceScope.launch {
                val billingCodeEntities = billingCodes.map { code ->
                    BillingCodeEntity(
                        patientId = patientId,
                        code = code.code,
                        date = code.date,
                        referringDoctor = code.referringDoctor,
                        emailedDate = code.emailedDate,
                        emailedTo = code.emailedTo
                    )
                }
                dao.insertBillingCodes(billingCodeEntities)
            }
        }
    }
    
    /**
     * Update emailed status for a billing code
     */
    fun updateBillingCodeEmailStatus(patientId: String, billingCode: BillingCode, emailedDate: Date, emailedTo: String) {
        // Update in memory
        patientRecords[patientId]?.billingCodes?.find { 
            it.code == billingCode.code && it.date == billingCode.date 
        }?.apply {
            this.emailedDate = emailedDate
            this.emailedTo = emailedTo
        }
        
        // Update in database
        patientDao?.let { dao ->
            serviceScope.launch {
                dao.updateBillingCodeEmailStatus(
                    patientId = patientId,
                    code = billingCode.code,
                    date = billingCode.date,
                    emailedDate = emailedDate,
                    emailedTo = emailedTo
                )
            }
        }
    }
    
    /**
     * Get the current patient record
     */
    fun getCurrentPatient(): PatientRecord? {
        return currentPatientId?.let { patientRecords[it] }
    }
    
    /**
     * Get a patient record by ID
     */
    fun getPatientById(patientId: String): PatientRecord? {
        return patientRecords[patientId]
    }
    
    /**
     * Get all patient records
     */
    fun getAllPatients(): List<PatientRecord> {
        return patientRecords.values.toList()
    }
    
    /**
     * Get all patient records sorted by date (newest first)
     */
    fun getAllPatientsSortedByDate(): List<PatientRecord> {
        return patientRecords.values.sortedByDescending { it.creationDate }
    }
    
    /**
     * Get all billing codes for a patient, sorted by date (newest first)
     */
    fun getPatientBillingCodesSorted(patientId: String): List<BillingCode> {
        return patientRecords[patientId]?.billingCodes?.sortedByDescending { it.date } ?: emptyList()
    }
    
    /**
     * Reset the current patient ID
     */
    fun resetCurrentPatient() {
        currentPatientId = null
    }
    
    /**
     * Update an existing patient record
     */
    fun updatePatientRecord(patient: PatientRecord) {
        if (patient.patientId.isNotEmpty()) {
            patientRecords[patient.patientId] = patient
            
            // Save to database
            savePatientToDatabase(patient)
        }
    }
    
    /**
     * Check if any billing codes for a patient have been emailed
     */
    fun hasEmailedBillingCodes(patientId: String): Boolean {
        val billingCodes = patientRecords[patientId]?.billingCodes ?: return false
        // Return true only if there are billing codes and ALL have been emailed
        return billingCodes.isNotEmpty() && billingCodes.all { it.emailedDate != null }
    }
    
    /**
     * Delete a billing code for a patient
     */
    fun deleteBillingCode(patientId: String, billingCode: BillingCode) {
        // Delete from memory
        patientRecords[patientId]?.billingCodes?.removeIf { 
            it.code == billingCode.code && it.date == billingCode.date 
        }
        
        // Delete from database
        patientDao?.let { dao ->
            serviceScope.launch {
                dao.deleteBillingCode(patientId, billingCode.code, billingCode.date)
            }
        }
    }
    
    /**
     * Delete a patient and all associated billing codes
     */
    fun deletePatient(patientId: String) {
        // Delete from memory
        patientRecords.remove(patientId)
        
        // Delete from database
        patientDao?.let { dao ->
            serviceScope.launch {
                dao.deletePatientWithBillingCodes(patientId)
            }
        }
    }
} 