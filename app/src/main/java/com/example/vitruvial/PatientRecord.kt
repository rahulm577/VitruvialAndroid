package com.example.vitruvial

import java.util.Date

/**
 * Class to represent a patient record with associated billing codes
 */
data class PatientRecord(
    val patientId: String,
    val patientName: String,
    val extractedInfo: Map<String, String>,
    val billingCodes: MutableList<BillingCode> = mutableListOf(),
    val creationDate: Date = Date()
) {
    /**
     * Determines if this patient is the same as another based on key identifiers
     */
    fun isSamePatient(other: PatientRecord): Boolean {
        val firstName = extractedInfo["firstName"] ?: ""
        val lastName = extractedInfo["lastName"] ?: ""
        val dob = extractedInfo["dateOfBirth"] ?: ""
        val address = extractedInfo["address"] ?: ""
        val medicare = extractedInfo["medicareNumber"] ?: ""
        
        val otherFirstName = other.extractedInfo["firstName"] ?: ""
        val otherLastName = other.extractedInfo["lastName"] ?: ""
        val otherDob = other.extractedInfo["dateOfBirth"] ?: ""
        val otherAddress = other.extractedInfo["address"] ?: ""
        val otherMedicare = other.extractedInfo["medicareNumber"] ?: ""
        
        // If Medicare numbers match and are not empty, consider them the same patient
        if (medicare.isNotEmpty() && medicare == otherMedicare) {
            return true
        }
        
        // If name, DOB and address all match, consider them the same patient
        return firstName == otherFirstName && 
               lastName == otherLastName && 
               dob == otherDob &&
               address == otherAddress
    }
}