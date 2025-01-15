package com.example.hrml.ui.home

// Data class die informatie over een vacature bevat
data class Job(
    val title: String,          // Titel van de vacature
    val description: String,     // Beschrijving van de vacature
    val id: String,              // Unieke identificatie van de vacature
    val reactions: Int,          // Aantal reacties (sollicitanten) op de vacature
    var isFavorite: Boolean      // Boolean die aangeeft of de vacature als favoriet is gemarkeerd
)
