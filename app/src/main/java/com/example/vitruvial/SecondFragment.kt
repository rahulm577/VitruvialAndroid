package com.example.vitruvial

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.vitruvial.databinding.FragmentSecondBinding
import android.provider.MediaStore
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Fragment that displays the document image and editable extracted text
 */
class SecondFragment : Fragment() {
    private val TAG = "SecondFragment"
    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    
    private var photoUriString: String? = null
    private var photoPath: String? = null
    private var imageBitmap: Bitmap? = null
    private val anthropicClient by lazy { AnthropicClient(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get photo URI and path from arguments
        arguments?.let { args ->
            photoUriString = args.getString("photoUriString")
            photoPath = args.getString("photoPath")
        }
        
        // Load the image
        loadImage()
        
        // Perform OCR and use Claude to extract patient info
        extractText()

        // Cancel button - go back to the add record fragment
        binding.buttonCancel.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_AddRecordFragment)
        }
        
        // Confirm button - proceed to the billing fragment
        binding.buttonConfirm.setOnClickListener {
            // Create a map with all the fields
            val extractedInfo = collectFormData()
            
            // Create a patient record
            val fullName = "${extractedInfo["firstName"] ?: ""} ${extractedInfo["lastName"] ?: ""}".trim()
            val patientName = if (fullName.isNotEmpty()) fullName else "Unknown Patient"
            
            PatientService.createPatientRecord(patientName, extractedInfo)
            
            // Navigate to the billing fragment, passing the photoUriString
            val args = Bundle().apply {
                putString("photoUriString", photoUriString)
            }
            findNavController().navigate(R.id.action_SecondFragment_to_BillingFragment, args)
        }
    }
    
    private fun loadImage() {
        try {
            photoUriString?.let { uriString ->
                val uri = Uri.parse(uriString)
                imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                binding.imageViewDocumentPreview.setImageBitmap(imageBitmap)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error loading image: ${e.message}", e)
        }
    }
    
    private fun extractText() {
        // Show loading state
        showLoadingState(true)
        
        lifecycleScope.launch {
            try {
                // Step 1: Perform OCR on the image
                val recognizedText = performOCR()
                
                if (recognizedText.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        showLoadingState(false)
                        binding.edittextName.setText("")
                        Toast.makeText(context, getString(R.string.no_text_found), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                Log.d(TAG, "OCR text recognized: ${recognizedText.take(100)}...")
                
                // Step 2: Send the OCR text to Claude API for extraction
                val patientInfo = withContext(Dispatchers.IO) {
                    anthropicClient.extractPatientInfo(recognizedText)
                }
                
                // Step 3: Populate the form fields with the extracted info
                withContext(Dispatchers.Main) {
                    populateFormFields(patientInfo)
                    showLoadingState(false)
                    Toast.makeText(context, getString(R.string.extraction_successful), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting text: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoadingState(false)
                    Toast.makeText(context, getString(R.string.extraction_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showLoadingState(isLoading: Boolean) {
        val loadingText = "Loading..."
        if (isLoading) {
            binding.edittextName.setText(loadingText)
            binding.edittextDob.setText(loadingText)
            binding.edittextAddress.setText(loadingText)
            binding.edittextPhone.setText(loadingText)
            binding.edittextMedicare.setText(loadingText)
            binding.edittextHealthcareFund.setText(loadingText)
            binding.edittextHealthcareFundNumber.setText(loadingText)
        }
    }
    
    private fun populateFormFields(patientInfo: PatientInfo) {
        val fullName = "${patientInfo.firstName} ${patientInfo.lastName}".trim()
        
        binding.edittextName.setText(fullName)
        binding.edittextDob.setText(patientInfo.dateOfBirth)
        binding.edittextAddress.setText(patientInfo.address)
        binding.edittextPhone.setText(patientInfo.phoneNumber)
        binding.edittextMedicare.setText(patientInfo.medicareNumber)
        binding.edittextHealthcareFund.setText(patientInfo.healthcareFund)
        binding.edittextHealthcareFundNumber.setText(patientInfo.healthcareFundNumber)
    }
    
    private fun collectFormData(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        // Split name into first and last name if possible
        val fullName = binding.edittextName.text.toString().trim()
        val nameParts = fullName.split(" ", limit = 2)
        
        result["firstName"] = if (nameParts.isNotEmpty()) nameParts[0] else ""
        result["lastName"] = if (nameParts.size > 1) nameParts[1] else ""
        
        result["dateOfBirth"] = binding.edittextDob.text.toString().trim()
        result["address"] = binding.edittextAddress.text.toString().trim()
        result["phoneNumber"] = binding.edittextPhone.text.toString().trim()
        result["medicareNumber"] = binding.edittextMedicare.text.toString().trim()
        result["healthcareFund"] = binding.edittextHealthcareFund.text.toString().trim()
        result["healthcareFundNumber"] = binding.edittextHealthcareFundNumber.text.toString().trim()
        
        return result
    }
    
    private suspend fun performOCR(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val bitmap = imageBitmap ?: return@withContext ""
            
            // Create an InputImage from the bitmap
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            // Create a TextRecognizer
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            // Process the image and wait for the result
            val result = suspendCoroutine<Text> { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { text ->
                        continuation.resume(text)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
            
            // Extract the recognized text
            result.text
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed: ${e.message}", e)
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}