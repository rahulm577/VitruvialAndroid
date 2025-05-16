package com.example.vitruvial

/**
 * Data class to hold patient information extracted from OCR text
 */
data class PatientInfo(
    var firstName: String = "",
    var lastName: String = "",
    var dateOfBirth: String = "",
    var address: String = "",
    var phoneNumber: String = "",
    var medicareNumber: String = "",
    var healthcareFund: String = "",
    var healthcareFundNumber: String = "",
    var referringDoctor: String = ""
) 