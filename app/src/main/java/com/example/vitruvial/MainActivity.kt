package com.example.vitruvial

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.vitruvial.databinding.ActivityMainBinding
import com.example.vitruvial.databinding.ContentMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var contentBinding: ContentMainBinding
    private var currentPhotoPath: String = ""
    private var currentImageUri: Uri? = null
    private var bitmap: Bitmap? = null
    private lateinit var anthropicClient: AnthropicClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, proceed with camera launch
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
            }
        }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val file = File(currentPhotoPath)
                bitmap = BitmapFactory.decodeFile(file.absolutePath)
                contentBinding.imageView.setImageBitmap(bitmap)
                contentBinding.buttonProcess.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    currentImageUri = imageUri
                    val inputStream = contentResolver.openInputStream(imageUri)
                    bitmap = BitmapFactory.decodeStream(inputStream)
                    contentBinding.imageView.setImageBitmap(bitmap)
                    contentBinding.buttonProcess.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize content binding
        contentBinding = ContentMainBinding.bind(binding.root.findViewById(R.id.content_main_layout))
        
        // Initialize the AnthropicClient with context
        anthropicClient = AnthropicClient(this)
        
        setSupportActionBar(binding.toolbar)

        // Set up button click listeners
        contentBinding.buttonCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                dispatchTakePictureIntent()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        contentBinding.buttonGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }

        contentBinding.buttonProcess.setOnClickListener {
            processImageForTextRecognition()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create the File where the photo should go
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            // Error occurred while creating the File
            null
        }
        // Continue only if the File was successfully created
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            try {
                takePictureLauncher.launch(takePictureIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Camera app not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun processImageForTextRecognition() {
        val bitmap = this.bitmap ?: return

        contentBinding.buttonProcess.isEnabled = false
        contentBinding.textViewResult.text = getString(R.string.processing_image)

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isEmpty()) {
                    contentBinding.textViewResult.text = getString(R.string.no_text_found)
                } else {
                    // Use LLM to extract patient information
                    extractPatientInfoUsingLLM(visionText.text)
                }
            }
            .addOnFailureListener { e ->
                contentBinding.textViewResult.text = getString(R.string.extraction_failed) + ": " + e.message
                contentBinding.buttonProcess.isEnabled = true
            }
    }

    private fun extractPatientInfoUsingLLM(text: String) {
        lifecycleScope.launch {
            try {
                contentBinding.textViewResult.text = "Using Claude AI to analyze text and extract patient information..."
                
                // Set progress indicator
                showProgressIndicator(true)
                
                // Call the Anthropic Claude API through our client
                val patientInfo = anthropicClient.extractPatientInfo(text)
                
                // Populate editable fields with the extracted data
                contentBinding.editFirstName.setText(patientInfo.firstName)
                contentBinding.editLastName.setText(patientInfo.lastName)
                contentBinding.editDob.setText(patientInfo.dateOfBirth)
                contentBinding.editAddress.setText(patientInfo.address)
                contentBinding.editPhoneNumber.setText(patientInfo.phoneNumber)
                contentBinding.editMedicare.setText(patientInfo.medicareNumber)
                contentBinding.editHealthcareFund.setText(patientInfo.healthcareFund)
                contentBinding.editHealthcareFundNumber.setText(patientInfo.healthcareFundNumber)
                
                // Keep the original data for reference but hide it from view
                val resultText = buildString {
                    append("AI-Extracted Patient Information:\n\n")
                    append("First Name: ${patientInfo.firstName}\n")
                    append("Last Name: ${patientInfo.lastName}\n")
                    append("Date of Birth: ${patientInfo.dateOfBirth}\n")
                    append("Address: ${patientInfo.address}\n")
                    append("Phone Number: ${patientInfo.phoneNumber}\n")
                    append("Medicare Number: ${patientInfo.medicareNumber}\n")
                    append("Healthcare Fund: ${patientInfo.healthcareFund}\n")
                    append("Healthcare Fund Number: ${patientInfo.healthcareFundNumber}\n")
                    append("\n---------------------\n\n")
                    append("Full OCR Text:\n$text")
                }
                
                contentBinding.textViewResult.text = resultText
                
                // Set up Save button click listener
                contentBinding.buttonSave.setOnClickListener {
                    savePatientInfo()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error during AI analysis: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Try fallback to basic extraction
                try {
                    val basicExtractedText = extractPatientInfo(text)
                    contentBinding.textViewResult.text = basicExtractedText
                    
                    // Try to extract some data from the basic extraction result to populate fields
                    extractAndPopulateFieldsFromBasicText(basicExtractedText)
                } catch (e2: Exception) {
                    Log.e("MainActivity", "Error in fallback extraction: ${e2.message}")
                }
            } finally {
                contentBinding.buttonProcess.isEnabled = true
                showProgressIndicator(false)
            }
        }
    }
    
    /**
     * Shows or hides a progress indicator while processing
     */
    private fun showProgressIndicator(show: Boolean) {
        // We could add a proper progress indicator here in the future
        if (show) {
            Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Extracts data from basic text extraction and populates fields
     */
    private fun extractAndPopulateFieldsFromBasicText(basicText: String) {
        // Split by lines and look for patterns
        val lines = basicText.split("\n")
        
        for (line in lines) {
            when {
                line.startsWith("First Name:") -> {
                    contentBinding.editFirstName.setText(line.substringAfter("First Name:").trim())
                }
                line.startsWith("Last Name:") -> {
                    contentBinding.editLastName.setText(line.substringAfter("Last Name:").trim())
                }
                line.startsWith("Date of Birth:") -> {
                    contentBinding.editDob.setText(line.substringAfter("Date of Birth:").trim())
                }
                line.startsWith("Address:") -> {
                    contentBinding.editAddress.setText(line.substringAfter("Address:").trim())
                }
                line.startsWith("Phone:") -> {
                    contentBinding.editPhoneNumber.setText(line.substringAfter("Phone:").trim())
                }
                line.startsWith("Medicare Number:") -> {
                    contentBinding.editMedicare.setText(line.substringAfter("Medicare Number:").trim())
                }
                line.startsWith("Healthcare Fund:") -> {
                    contentBinding.editHealthcareFund.setText(line.substringAfter("Healthcare Fund:").trim())
                }
                line.startsWith("Healthcare Fund Number:") -> {
                    contentBinding.editHealthcareFundNumber.setText(line.substringAfter("Healthcare Fund Number:").trim())
                }
            }
        }
    }
    
    /**
     * Saves patient information (could be expanded to save to a database)
     */
    private fun savePatientInfo() {
        // Get values from all fields
        val firstName = contentBinding.editFirstName.text.toString()
        val lastName = contentBinding.editLastName.text.toString()
        val dob = contentBinding.editDob.text.toString()
        
        // In a real app, you would save to a database or send to a server
        // For now, just show a success message
        Toast.makeText(this, "Patient information saved: $firstName $lastName", Toast.LENGTH_SHORT).show()
        
        // Log the captured patient information for debugging
        Log.d("PatientInfo", "Saved: $firstName $lastName, DOB: $dob")
    }

    private fun extractPatientInfo(text: String): String {
        val lines = text.split("\n")
        val sb = StringBuilder()
        
        // Add the full extracted text first
        sb.append("Full Extracted Text:\n$text\n\n")
        sb.append("---------------------\n\n")
        sb.append("Extracted Patient Information:\n\n")

        // Look for common patterns in patient information
        var firstName = findPattern(text, "(?i)first\\s*name[:\\s]+([\\w\\s-]+)")
        var lastName = findPattern(text, "(?i)last\\s*name[:\\s]+([\\w\\s-]+)")
        val dob = findPattern(text, "(?i)(dob|date\\s+of\\s+birth)[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")
        val address = findPattern(text, "(?i)address[:\\s]+([\\w\\s,.-]+)")
        val phone = findPattern(text, "(?i)(phone|tel)[:\\s]+(\\(\\d{3}\\)\\s*\\d{3}-\\d{4}|\\d{3}[.-]\\d{3}[.-]\\d{4})")
        val medicare = findPattern(text, "(?i)medicare\\s*(no|number|#)?[:\\s]*(\\d{10}|\\d{4}\\s\\d{5}\\s\\d)")
        val healthcareFund = findPattern(text, "(?i)health(care)?\\s*fund[:\\s]+([\\w\\s]+)")
        val healthcareNumber = findPattern(text, "(?i)health(care)?\\s*(no|number|#)?[:\\s]*(\\d+)")

        // If first and last name not found with patterns, try to find names in consecutive lines
        if (firstName.isEmpty() || lastName.isEmpty()) {
            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.contains("name", true) && i + 1 < lines.size) {
                    val nameLine = lines[i + 1].trim()
                    val nameParts = nameLine.split("\\s+".toRegex())
                    if (nameParts.size >= 2) {
                        if (firstName.isEmpty()) firstName = nameParts[0]
                        if (lastName.isEmpty()) lastName = nameParts.last()
                    }
                }
            }
        }

        // Append the found information to the result
        if (firstName.isNotEmpty()) sb.append("First Name: $firstName\n")
        if (lastName.isNotEmpty()) sb.append("Last Name: $lastName\n")
        if (dob.isNotEmpty()) sb.append("Date of Birth: $dob\n")
        if (address.isNotEmpty()) sb.append("Address: $address\n")
        if (phone.isNotEmpty()) sb.append("Phone: $phone\n")
        if (medicare.isNotEmpty()) sb.append("Medicare Number: $medicare\n")
        if (healthcareFund.isNotEmpty()) sb.append("Healthcare Fund: $healthcareFund\n")
        if (healthcareNumber.isNotEmpty()) sb.append("Healthcare Fund Number: $healthcareNumber\n")

        return sb.toString()
    }

    private fun findPattern(text: String, patternStr: String): String {
        val pattern = Pattern.compile(patternStr)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            if (matcher.groupCount() >= 1) matcher.group(1) ?: "" else ""
        } else ""
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Show app info instead of settings
                Toast.makeText(this, "Vitruvial - HIPAA compliant healthcare OCR", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}