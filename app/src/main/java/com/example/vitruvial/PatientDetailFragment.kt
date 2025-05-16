package com.example.vitruvial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.vitruvial.databinding.FragmentPatientDetailBinding
import com.example.vitruvial.databinding.ItemBillingDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class PatientDetailFragment : Fragment() {

    private var _binding: FragmentPatientDetailBinding? = null
    private val binding get() = _binding!!
    
    private var patientId: String? = null
    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get patient ID from arguments
        arguments?.let { args ->
            patientId = args.getString("patientId")
        }
        
        // Load and display patient information
        loadPatientDetails()
        
        // Set up back button
        binding.buttonBackToList.setOnClickListener {
            findNavController().navigate(R.id.action_PatientDetailFragment_to_PatientListFragment)
        }
        
        // Set up email button
        binding.buttonEmailRecord.setOnClickListener {
            patientId?.let { id ->
                val patient = PatientService.getPatientById(id)
                patient?.let { showEmailSelectionDialog(it) }
            }
        }
    }
    
    private fun showEmailSelectionDialog(patient: PatientRecord) {
        val dialog = EmailSelectionDialog(requireContext())
        dialog.showDialog(patient) { emailAddress, selectedBillingCodes ->
            // Send the email with selected billing codes
            EmailHelper.sendPatientRecordEmail(
                requireContext(),
                patient,
                selectedBillingCodes,
                emailAddress
            )
            
            // Refresh the UI to show updated emailed status
            loadPatientDetails()
        }
    }
    
    private fun loadPatientDetails() {
        patientId?.let { id ->
            val patient = PatientService.getPatientById(id)
            
            patient?.let {
                // Set patient title
                binding.textviewPatientDetailTitle.text = patient.patientName
                
                // Personal information
                val firstName = patient.extractedInfo["firstName"] ?: ""
                val lastName = patient.extractedInfo["lastName"] ?: ""
                val fullName = "$firstName $lastName".trim()
                
                binding.textviewDetailName.text = "Full Name: $fullName"
                binding.textviewDetailDob.text = "Date of Birth: ${patient.extractedInfo["dateOfBirth"] ?: "Unknown"}"
                binding.textviewDetailAddress.text = "Address: ${patient.extractedInfo["address"] ?: "Unknown"}"
                binding.textviewDetailPhone.text = "Phone: ${patient.extractedInfo["phoneNumber"] ?: "Unknown"}"
                binding.textviewDetailMedicare.text = "Medicare Number: ${patient.extractedInfo["medicareNumber"] ?: "Unknown"}"
                binding.textviewDetailHealthcareFund.text = "Healthcare Fund: ${patient.extractedInfo["healthcareFund"] ?: "Unknown"}"
                binding.textviewDetailHealthcareFundNumber.text = "Healthcare Fund Number: ${patient.extractedInfo["healthcareFundNumber"] ?: "Unknown"}"
                
                // Billing codes
                val billingCodes = PatientService.getPatientBillingCodesSorted(id)
                
                if (billingCodes.isEmpty()) {
                    binding.textviewNoBilling.visibility = View.VISIBLE
                    binding.layoutBillingCodes.visibility = View.GONE
                } else {
                    binding.textviewNoBilling.visibility = View.GONE
                    binding.layoutBillingCodes.visibility = View.VISIBLE
                    
                    // Add billing codes to the layout
                    binding.layoutBillingCodes.removeAllViews()
                    
                    for (billingCode in billingCodes) {
                        val billingItemBinding = ItemBillingDetailBinding.inflate(
                            layoutInflater, binding.layoutBillingCodes, false
                        )
                        
                        billingItemBinding.textviewBillingCode.text = "Billing Code: ${billingCode.code}"
                        billingItemBinding.textviewBillingDate.text = "Date: ${dateFormat.format(billingCode.date)}"
                        billingItemBinding.textviewReferringDoctor.text = "Referring Doctor: ${billingCode.referringDoctor.ifEmpty { "Not specified" }}"
                        
                        // Check if this billing code has been emailed
                        if (billingCode.emailedDate != null && billingCode.emailedTo != null) {
                            // Show emailed status
                            billingItemBinding.textviewEmailedStatus.visibility = View.VISIBLE
                            billingItemBinding.textviewEmailedStatus.text = 
                                "Emailed to: ${billingCode.emailedTo} on ${dateFormat.format(billingCode.emailedDate!!)} at ${timeFormat.format(billingCode.emailedDate!!)}"
                            
                            // Apply green background to the card
                            billingItemBinding.root.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.emailedGreen)
                            )
                        } else {
                            billingItemBinding.textviewEmailedStatus.visibility = View.GONE
                            // Use light background for un-emailed records for better readability
                            billingItemBinding.root.setCardBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.unemailedLight)
                            )
                        }
                        
                        binding.layoutBillingCodes.addView(billingItemBinding.root)
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 