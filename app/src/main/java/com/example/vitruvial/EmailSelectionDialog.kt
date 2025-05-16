package com.example.vitruvial

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dialog for selecting billing codes to email
 */
class EmailSelectionDialog(private val context: Context) {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    /**
     * Show dialog for selecting billing codes to email
     */
    fun showDialog(
        patient: PatientRecord,
        onSendEmail: (String, List<BillingCode>) -> Unit
    ) {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }
        
        // Email address field
        val emailLabel = TextView(context).apply {
            text = "Email Address"
            textSize = 16f
        }
        layout.addView(emailLabel)
        
        val emailInput = EditText(context).apply {
            hint = "example@email.com"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        layout.addView(emailInput)
        
        // Separator
        layout.addView(createSeparator())
        
        // Billing codes selection title
        val billingTitle = TextView(context).apply {
            text = "Select Billing Records"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }
        layout.addView(billingTitle)
        
        // Get all billing codes, sorted by date
        val billingCodes = PatientService.getPatientBillingCodesSorted(patient.patientId)
        
        // Map to store checkboxes and their associated billing codes
        val checkboxMap = mutableMapOf<CheckBox, BillingCode>()
        
        if (billingCodes.isEmpty()) {
            // No billing codes message
            val noCodesText = TextView(context).apply {
                text = "No billing codes available"
                setPadding(0, 8, 0, 8)
            }
            layout.addView(noCodesText)
        } else {
            // Add checkbox for each billing code
            for (billingCode in billingCodes) {
                val codeLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 8, 0, 8)
                    
                    // Apply appropriate background color
                    if (billingCode.emailedDate != null) {
                        setBackgroundColor(ContextCompat.getColor(context, R.color.emailedGreen))
                    } else {
                        setBackgroundColor(ContextCompat.getColor(context, R.color.unemailedLight))
                    }
                }
                
                val checkbox = CheckBox(context).apply {
                    isChecked = false // Default to unchecked
                }
                
                // If this billing code was previously emailed, set the text color to green
                val displayText = StringBuilder().apply {
                    append("Code: ${billingCode.code}")
                    append(" | Date: ${dateFormat.format(billingCode.date)}")
                    if (!billingCode.referringDoctor.isNullOrEmpty()) {
                        append(" | Dr: ${billingCode.referringDoctor}")
                    }
                    
                    if (billingCode.emailedDate != null && billingCode.emailedTo != null) {
                        append("\nEmailed to: ${billingCode.emailedTo}")
                        append(" on ${dateFormat.format(billingCode.emailedDate!!)} at ${timeFormat.format(billingCode.emailedDate!!)}")
                    }
                }
                
                val textView = TextView(context).apply {
                    text = displayText.toString()
                    setPadding(16, 0, 0, 0)
                    if (billingCode.emailedDate != null) {
                        setTextColor(ContextCompat.getColor(context, R.color.emailedGreen))
                    }
                }
                
                codeLayout.addView(checkbox)
                codeLayout.addView(textView)
                layout.addView(codeLayout)
                
                // Store the checkbox with its associated billing code
                checkboxMap[checkbox] = billingCode
            }
        }
        
        // Build the dialog
        AlertDialog.Builder(context)
            .setTitle("Email Patient Record")
            .setView(layout)
            .setPositiveButton("Send") { _, _ ->
                val email = emailInput.text.toString().trim()
                
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    // Show error for invalid email
                    AlertDialog.Builder(context)
                        .setTitle("Invalid Email")
                        .setMessage("Please enter a valid email address.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setPositiveButton
                }
                
                // Get selected billing codes
                val selectedCodes = checkboxMap.entries
                    .filter { it.key.isChecked }
                    .map { it.value }
                
                if (selectedCodes.isEmpty()) {
                    // Show warning for no selected codes
                    AlertDialog.Builder(context)
                        .setTitle("No Codes Selected")
                        .setMessage("Please select at least one billing code to email.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setPositiveButton
                }
                
                // Callback with selected codes
                onSendEmail(email, selectedCodes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Creates a separator line for the dialog
     */
    private fun createSeparator(): View {
        return View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                1
            ).apply {
                setMargins(0, 16, 0, 16)
            }
        }
    }
} 