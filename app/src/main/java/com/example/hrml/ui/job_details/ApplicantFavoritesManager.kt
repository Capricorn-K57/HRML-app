package com.example.hrml.ui.job_details

import android.content.Context
import android.util.Log
import com.example.hrml.ui.home.Applicant

// Singleton object voor het beheren van favoriete sollicitanten per vacature
object ApplicantFavoritesManager {
    // Map om de favorietenstatus van sollicitanten bij te houden, gekoppeld aan een pair van jobId en applicantId
    val favoriteApplicantMap = mutableMapOf<Pair<String, String>, Boolean>()

    // Cache voor sollicitanten per jobId, om herhaaldelijke netwerkaanroepen te vermijden
    private val applicantCache = mutableMapOf<String, MutableMap<String, Applicant>>()

    // Functie om een sollicitant toe te voegen aan de cache voor een specifieke vacature
    fun cacheApplicant(jobId: String, applicant: Applicant) {
        // Zorg ervoor dat er een MutableMap is voor de opgegeven jobId als die nog niet bestaat
        if (!applicantCache.containsKey(jobId)) {
            applicantCache[jobId] = mutableMapOf()
        }
        // Voeg de sollicitant toe aan de juiste MutableMap onder het jobId
        applicantCache[jobId]?.put(applicant.id, applicant)
    }

    // Functie om de naam van een sollicitant op te halen aan de hand van jobId en applicantId
    fun getApplicantNameById(jobId: String, applicantId: String): String {
        return applicantCache[jobId]?.get(applicantId)?.name ?: "Onbekend"
    }

    // Functie om de matching score van een sollicitant op te halen aan de hand van jobId en applicantId
    fun getApplicantMatchingScoreById(jobId: String, applicantId: String): Double {
        return applicantCache[jobId]?.get(applicantId)?.matchingScore ?: 0.0
    }

    // Functie om de favorietenstatus van een sollicitant op te slaan (of bij te werken)
    fun setFavorite(context: Context, jobId: String, applicantId: String, isFavorite: Boolean) {
        // Werk de status bij in de favoriteApplicantMap
        favoriteApplicantMap[Pair(jobId, applicantId)] = isFavorite

        // Haal de SharedPreferences op om de favorietenstatus persistent op te slaan
        val sharedPref = context.getSharedPreferences("FavoriteApplicants", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            // Sla de favorietenstatus op per combinatie van jobId en applicantId
            putBoolean("${jobId}_$applicantId", isFavorite)
            apply() // Toepassen van de wijzigingen in SharedPreferences
        }

        // Log de actie voor debugging
        Log.d("ApplicantFavoritesManager", "Set favorite status for applicant $applicantId in job $jobId to $isFavorite")
    }

    // Functie om de favorietenstatus van een sollicitant op te halen uit SharedPreferences
    fun isFavorite(context: Context, jobId: String, applicantId: String): Boolean {
        val sharedPref = context.getSharedPreferences("FavoriteApplicants", Context.MODE_PRIVATE)
        Log.d("ApplicantFavoritesManager", "Checking favorite status for jobId: $jobId, userId: $applicantId")

        // Haal de favorietenstatus op uit SharedPreferences, standaard is het 'false'
        return sharedPref.getBoolean("${jobId}_$applicantId", false)
    }

    // Functie die de lijst van sollicitanten bijwerkt met de juiste favorietenstatus
    fun updateApplicantsWithFavorites(context: Context, applicants: List<Applicant>, jobId: String): List<Applicant> {
        return applicants.map { applicant ->
            // Haal de favorietenstatus voor elke sollicitant op
            val isFav = isFavorite(context, jobId, applicant.id)

            // Cache de sollicitant met de bijgewerkte favorietenstatus
            if (isFav) cacheApplicant(jobId, applicant.copy(isFavorite = isFav))

            // Geef een kopie van de sollicitant terug met de favorietenstatus toegevoegd
            applicant.copy(isFavorite = isFav)
        }
    }

    // Functie om de cache voor sollicitanten voor een specifieke vacature te wissen
    fun clearApplicantCacheForJob(jobId: String) {
        applicantCache[jobId]?.clear() // Wis de cache voor de opgegeven jobId
    }

    // Functie om de lijst van favoriete sollicitanten voor een specifieke vacature op te halen
    fun getFavoriteApplicantsForJob(context: Context, jobId: String): List<Applicant> {
        val sharedPref = context.getSharedPreferences("FavoriteApplicants", Context.MODE_PRIVATE)
        Log.d("ApplicantFavoritesManager", "Fetching favorite applicants for jobId: $jobId")

        // Filter de sollicitanten op basis van hun favorietenstatus
        return favoriteApplicantMap.filterKeys { it.first == jobId }
            .mapNotNull { (key, isFavorite) ->
                val applicantId = key.second
                val fromSharedPref = sharedPref.getBoolean("${jobId}_$applicantId", false)

                // Voeg de sollicitant toe als deze als favoriet is gemarkeerd, hetzij in de map of in de SharedPreferences
                if (isFavorite || fromSharedPref) {
                    // Haal de naam en de matching score van de sollicitant op
                    val name = getApplicantNameById(jobId, applicantId)
                    val matchingScore = getApplicantMatchingScoreById(jobId, applicantId)

                    Log.d("ApplicantFavoritesManager", "Adding favorite applicant: $applicantId for job $jobId with name: $name and score: $matchingScore")

                    // Maak een kopie van de sollicitant met de juiste waarden en voeg deze toe aan de lijst
                    applicantCache[jobId]?.get(applicantId)?.copy(
                        isFavorite = true,
                        matchingScore = matchingScore
                    )
                } else {
                    Log.d("ApplicantFavoritesManager", "Applicant $applicantId for job $jobId is not a favorite")
                    null // Sollicitant wordt niet toegevoegd als hij geen favoriet is
                }
            }
            .filterNotNull() // Filter null-waarden uit de lijst (sollicitanten die geen favoriet zijn)
    }
}
