package com.example.vitruvial

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.vitruvial.databinding.FragmentAddRecordBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddRecordFragment : Fragment() {

    private val TAG = "AddRecordFragment"
    private var _binding: FragmentAddRecordBinding? = null
    private val binding get() = _binding!!
    
    private var currentPhotoPath: String? = null
    private var currentPhotoUri: Uri? = null
    private var imageSelected = false
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }
        
        if (cameraGranted && storageGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(requireContext(), "Camera and storage permissions are required", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Register for camera result
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                currentPhotoUri?.let { uri ->
                    val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                    binding.imageViewDocument.setImageBitmap(bitmap)
                    binding.imageViewDocument.visibility = View.VISIBLE
                    imageSelected = true
                    updateExtractButtonState()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error loading camera image", e)
            }
        }
    }
    
    // Register for gallery result
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            try {
                imageUri?.let { uri ->
                    currentPhotoUri = uri
                    val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                    binding.imageViewDocument.setImageBitmap(bitmap)
                    binding.imageViewDocument.visibility = View.VISIBLE
                    imageSelected = true
                    updateExtractButtonState()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error loading gallery image", e)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Take photo button
        binding.buttonTakePhoto.setOnClickListener {
            checkCameraPermissionsAndDispatchIntent()
        }
        
        // Choose from gallery button
        binding.buttonChooseGallery.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }
        
        // Extract text button
        binding.buttonExtractText.setOnClickListener {
            // Save current photo URI and path to be accessed in SecondFragment
            val args = Bundle().apply {
                putString("photoUriString", currentPhotoUri.toString())
                putString("photoPath", currentPhotoPath)
            }
            findNavController().navigate(R.id.action_AddRecordFragment_to_SecondFragment, args)
        }
        
        // Back button
        binding.buttonBackToHome.setOnClickListener {
            findNavController().navigate(R.id.action_AddRecordFragment_to_FirstFragment)
        }
        
        // Reset extract button state
        updateExtractButtonState()
    }
    
    private fun checkCameraPermissionsAndDispatchIntent() {
        val cameraPermission = Manifest.permission.CAMERA
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(requireContext(), cameraPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(cameraPermission)
        }
        
        if (ContextCompat.checkSelfPermission(requireContext(), storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(storagePermission)
        }
        
        if (permissionsToRequest.isEmpty()) {
            // All permissions granted, proceed
            dispatchTakePictureIntent()
        } else {
            // Request permissions
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun checkStoragePermissionAndPickImage() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(requireContext(), storagePermission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(storagePermission))
        } else {
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(pickImageIntent)
        }
    }
    
    private fun updateExtractButtonState() {
        binding.buttonExtractText.isEnabled = imageSelected
    }
    
    private fun dispatchTakePictureIntent() {
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            
            // Create the file where the photo should go
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error creating image file", ex)
                null
            }
            
            // Continue only if the file was successfully created
            photoFile?.also {
                try {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.vitruvial.fileprovider",
                        it
                    )
                    currentPhotoUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    
                    // Launch camera without checking for resolveActivity
                    // This is needed because on Android 11+ resolveActivity can return null
                    // even when camera apps are available
                    takePictureLauncher.launch(takePictureIntent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error launching camera", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error launching camera", e)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Camera setup error", e)
        }
    }
    
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(null)
        
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 