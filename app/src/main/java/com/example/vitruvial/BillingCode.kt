package com.example.vitruvial

import java.util.Date

/**
 * Class representing a billing code with a date and referring doctor
 */
data class BillingCode(
    val code: String,
    val date: Date,
    val referringDoctor: String = "",
    var emailedDate: Date? = null,
    var emailedTo: String? = null
) 