package com.example.hrml.ui.home

// Data class die informatie over een sollicitant bevat
data class Applicant(
    val id: String,           // Unieke identificatie van de sollicitant
    val name: String,         // Naam van de sollicitant
    var matchingScore: Double, // Score die de mate van overeenkomst met de vacature aangeeft
    var isFavorite: Boolean = false   // Boolean die aangeeft of de sollicitant als favoriet is gemarkeerd

)
