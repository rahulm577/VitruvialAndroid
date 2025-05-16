package com.example.vitruvial.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.util.Date

/**
 * Data Access Object for the patients and billing codes tables
 */
@Dao
interface PatientDao {
    // Patient queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)
    
    @Query("SELECT * FROM patients ORDER BY creationDate DESC")
    suspend fun getAllPatients(): List<PatientEntity>
    
    @Query("SELECT * FROM patients WHERE patientId = :patientId")
    suspend fun getPatientById(patientId: String): PatientEntity?
    
    @Update
    suspend fun updatePatient(patient: PatientEntity)
    
    // Billing code queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillingCode(billingCode: BillingCodeEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillingCodes(billingCodes: List<BillingCodeEntity>)
    
    @Query("SELECT * FROM billing_codes WHERE patientId = :patientId ORDER BY date DESC")
    suspend fun getBillingCodesForPatient(patientId: String): List<BillingCodeEntity>
    
    @Query("UPDATE billing_codes SET emailedDate = :emailedDate, emailedTo = :emailedTo WHERE patientId = :patientId AND code = :code AND date = :date")
    suspend fun updateBillingCodeEmailStatus(patientId: String, code: String, date: Date, emailedDate: Date?, emailedTo: String?)
    
    /**
     * Delete a patient and all associated billing codes
     */
    @Transaction
    suspend fun deletePatientWithBillingCodes(patientId: String) {
        deleteBillingCodesForPatient(patientId)
        deletePatient(patientId)
    }
    
    @Query("DELETE FROM patients WHERE patientId = :patientId")
    suspend fun deletePatient(patientId: String)
    
    @Query("DELETE FROM billing_codes WHERE patientId = :patientId")
    suspend fun deleteBillingCodesForPatient(patientId: String)
    
    /**
     * Delete a specific billing code
     */
    @Query("DELETE FROM billing_codes WHERE patientId = :patientId AND code = :code AND date = :date")
    suspend fun deleteBillingCode(patientId: String, code: String, date: Date)
    
    /**
     * Patient with all billing codes
     */
    @Transaction
    suspend fun getPatientWithBillingCodes(patientId: String): PatientWithBillingCodes? {
        val patient = getPatientById(patientId) ?: return null
        val billingCodes = getBillingCodesForPatient(patientId)
        return PatientWithBillingCodes(patient, billingCodes)
    }
    
    @Transaction
    suspend fun getAllPatientsWithBillingCodes(): List<PatientWithBillingCodes> {
        val patients = getAllPatients()
        val result = mutableListOf<PatientWithBillingCodes>()
        
        for (patient in patients) {
            val billingCodes = getBillingCodesForPatient(patient.patientId)
            result.add(PatientWithBillingCodes(patient, billingCodes))
        }
        
        return result
    }
}

/**
 * Helper class for patient with all billing codes
 */
data class PatientWithBillingCodes(
    val patient: PatientEntity,
    val billingCodes: List<BillingCodeEntity>
) 