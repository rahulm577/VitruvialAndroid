package com.example.vitruvial

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.vitruvial.databinding.FragmentBillingBinding
import java.util.Calendar
import java.util.Date

/**
 * Fragment for adding billing codes to patient records
 */
class BillingFragment : Fragment() {

    private var _binding: FragmentBillingBinding? = null
    private val binding get() = _binding!!
    
    // List to store billing codes and dates
    private val billingCodes = mutableListOf<BillingCode>()
    
    // Photo URI for displaying the image
    private var photoUriString: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get photo URI from arguments
        arguments?.let { args ->
            photoUriString = args.getString("photoUriString")
        }

        // Add billing code button
        binding.buttonAddBillingCode.setOnClickListener {
            addBillingCodeField()
        }
        
        // Save record button - save billing codes and navigate to first fragment
        binding.buttonSaveBillingCodes.setOnClickListener {
            saveBillingCodes()
            // Navigate back to first fragment
            findNavController().navigate(R.id.action_BillingFragment_to_FirstFragment)
        }
        
        // Go back button - go back to editing extracted text
        binding.buttonCancelBilling.setOnClickListener {
            // Navigate back to second fragment with the photo URI
            val args = Bundle().apply {
                putString("photoUriString", photoUriString)
            }
            findNavController().navigate(R.id.action_BillingFragment_to_SecondFragment, args)
        }
        
        // Add initial billing code field
        addBillingCodeField()
    }
    
    private fun addBillingCodeField() {
        val codeLayout = layoutInflater.inflate(R.layout.billing_code_item, null) as LinearLayout
        val datePicker = codeLayout.findViewById<DatePicker>(R.id.datePickerBilling)
        val deleteButton = codeLayout.findViewById<ImageButton>(R.id.buttonDeleteBillingCode)
        
        // Set date picker to current date
        val calendar = Calendar.getInstance()
        datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            null
        )
        
        // Set up delete button
        deleteButton.setOnClickListener {
            // Don't allow deletion if this is the only billing code field
            if (binding.layoutBillingCodes.childCount > 1) {
                binding.layoutBillingCodes.removeView(codeLayout)
            } else {
                Toast.makeText(
                    requireContext(),
                    "At least one billing code is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        binding.layoutBillingCodes.addView(codeLayout)
    }
    
    private fun saveBillingCodes() {
        billingCodes.clear()
        
        // Get each billing code and date
        for (i in 0 until binding.layoutBillingCodes.childCount) {
            val codeLayout = binding.layoutBillingCodes.getChildAt(i) as LinearLayout
            val etBillingCode = codeLayout.findViewById<EditText>(R.id.editTextBillingCode)
            val etReferringDoctor = codeLayout.findViewById<EditText>(R.id.editTextReferringDoctor)
            val datePicker = codeLayout.findViewById<DatePicker>(R.id.datePickerBilling)
            
            val code = etBillingCode.text.toString()
            val referringDoctor = etReferringDoctor.text.toString()
            
            // Create a date from DatePicker
            val calendar = Calendar.getInstance()
            calendar.set(
                datePicker.year,
                datePicker.month,
                datePicker.dayOfMonth
            )
            val date = calendar.time
            
            if (code.isNotEmpty()) {
                billingCodes.add(BillingCode(code, date, referringDoctor))
            }
        }
        
        // Save to patient record
        if (billingCodes.isNotEmpty()) {
            PatientService.addBillingCodesToCurrentPatient(billingCodes)
            
            // Get the saved patient data for confirmation message
            val patient = PatientService.getCurrentPatient()
            val patientName = patient?.patientName ?: "Unknown"
            val count = billingCodes.size
            
            Toast.makeText(
                requireContext(),
                "Saved $count billing code${if (count > 1) "s" else ""} for $patientName",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "No billing codes entered",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 