package com.example.vitruvial

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for emailing patient records
 */
class EmailHelper {
    companion object {
        private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        
        /**
         * Send an email with patient record details
         */
        fun sendPatientRecordEmail(
            context: Context,
            patient: PatientRecord,
            selectedBillingCodes: List<BillingCode> = emptyList(),
            emailAddress: String
        ) {
            val subject = "Patient Record: ${patient.patientName}"
            
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, createEmailBody(patient, selectedBillingCodes))
            }
            
            if (emailIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(emailIntent)
                
                // Mark the billing codes as emailed
                val currentDate = Date()
                selectedBillingCodes.forEach { billingCode ->
                    // Update in memory object
                    billingCode.emailedDate = currentDate
                    billingCode.emailedTo = emailAddress
                    
                    // Update in database
                    PatientService.updateBillingCodeEmailStatus(
                        patientId = patient.patientId,
                        billingCode = billingCode,
                        emailedDate = currentDate,
                        emailedTo = emailAddress
                    )
                }
            }
        }
        
        /**
         * Create the email body with patient and billing information
         */
        private fun createEmailBody(
            patient: PatientRecord,
            selectedBillingCodes: List<BillingCode>
        ): String {
            val sb = StringBuilder()
            
            // Patient information
            sb.appendLine("PATIENT INFORMATION")
            sb.appendLine("------------------")
            sb.appendLine("Name: ${getPatientFullName(patient)}")
            sb.appendLine("Date of Birth: ${patient.extractedInfo["dateOfBirth"] ?: "Unknown"}")
            sb.appendLine("Medicare Number: ${patient.extractedInfo["medicareNumber"] ?: "Unknown"}")
            sb.appendLine("Address: ${patient.extractedInfo["address"] ?: "Unknown"}")
            sb.appendLine("Phone: ${patient.extractedInfo["phoneNumber"] ?: "Unknown"}")
            sb.appendLine("Healthcare Fund: ${patient.extractedInfo["healthcareFund"] ?: "Unknown"}")
            sb.appendLine("Healthcare Fund Number: ${patient.extractedInfo["healthcareFundNumber"] ?: "Unknown"}")
            sb.appendLine()
            
            // Billing codes
            sb.appendLine("BILLING INFORMATION")
            sb.appendLine("------------------")
            
            if (selectedBillingCodes.isEmpty()) {
                sb.appendLine("No billing codes selected.")
            } else {
                selectedBillingCodes.forEach { billingCode ->
                    sb.appendLine("Billing Code: ${billingCode.code}")
                    sb.appendLine("Date: ${dateFormat.format(billingCode.date)}")
                    sb.appendLine("Referring Doctor: ${billingCode.referringDoctor.ifEmpty { "Not specified" }}")
                    sb.appendLine()
                }
            }
            
            return sb.toString()
        }
        
        /**
         * Gets the patient's full name from extracted info, or falls back to the display name
         */
        private fun getPatientFullName(patient: PatientRecord): String {
            val firstName = patient.extractedInfo["firstName"] ?: ""
            val lastName = patient.extractedInfo["lastName"] ?: ""
            val fullName = "$firstName $lastName".trim()
            
            return if (fullName.isNotEmpty()) fullName else patient.patientName
        }
    }
} 