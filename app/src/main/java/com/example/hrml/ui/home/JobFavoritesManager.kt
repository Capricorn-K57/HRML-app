package com.example.hrml.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hrml.DataClasses.JobSharedPrefManager

// Singleton object dat het beheer van favoriete vacatures afhandelt
object JobFavoritesManager {
    // Set die de favorieten bijhoudt, waarbij de jobId de sleutel is
    private val favoriteJobs = mutableSetOf<String>()

    // LiveData voor het bijhouden van veranderingen in de lijst van favorieten
    private val _favoriteJobsLiveData = MutableLiveData<Set<String>>()
    val favoriteJobsLiveData: LiveData<Set<String>> get() = _favoriteJobsLiveData

    // Functie om een vacature toe te voegen of te verwijderen uit de favorieten
    fun setFavorite(jobId: String, isFavorite: Boolean, context: Context) {
        // Als de vacature als favoriet wordt gemarkeerd, voeg deze dan toe aan de set
        if (isFavorite) {
            favoriteJobs.add(jobId)
        } else {
            // Als de vacature niet meer als favoriet wordt gemarkeerd, verwijder deze uit de set
            favoriteJobs.remove(jobId)
        }

        // Update de LiveData zodat de veranderingen kunnen worden opgepikt door de UI
        _favoriteJobsLiveData.value = favoriteJobs

        // Gebruik de SharedPreferences manager om de favorietstatus op te slaan
        val sharedPrefManager = JobSharedPrefManager(context)
        sharedPrefManager.setJobFavorite(jobId, isFavorite)

        // Log de actie voor debugging
        Log.d("JobFavoritesManager", "Set favorite for jobId: $jobId to $isFavorite")
        Log.d("JobFavoritesManager", "Updated favoriteJobs: $favoriteJobs")
    }

    // Functie om de favorieten te initialiseren bij het opstarten van de applicatie
    fun initialize(context: Context) {
        // Laad de opgeslagen favorieten uit de SharedPreferences
        val sharedPrefManager = JobSharedPrefManager(context)
        favoriteJobs.addAll(sharedPrefManager.getJobFavorites())

        // Update de LiveData met de geladen favorieten
        _favoriteJobsLiveData.value = favoriteJobs

        // Log de geladen favorieten voor debugging
        Log.d("JobFavoritesManager", "Initialized with favorites: $favoriteJobs")
    }

    // Functie om te controleren of een specifieke vacature als favoriet is gemarkeerd
    fun isFavorite(jobId: String): Boolean {
        return favoriteJobs.contains(jobId) // Retourneer true als jobId in de set zit
    }

    // Functie om alle favoriete vacature-IDs op te halen
    fun getFavoriteJobIds(): List<String> {
        return favoriteJobs.toList() // Retourneer de lijst van favoriete vacature-IDs
    }
}
