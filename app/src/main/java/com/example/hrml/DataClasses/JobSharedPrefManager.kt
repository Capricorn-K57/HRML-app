package com.example.hrml.DataClasses

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * JobSharedPrefManager beheert het opslaan en ophalen van favoriete vacatures
 * met behulp van SharedPreferences. Het ondersteunt functies zoals het toevoegen,
 * verwijderen en controleren van favorietstatussen.
 *
 * @param context Context voor toegang tot de SharedPreferences van de app.
 */
class JobSharedPrefManager(context: Context) {

    // SharedPreferences object voor het opslaan van gegevens lokaal in de app.
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("job_preferences", Context.MODE_PRIVATE)

    /**
     * Slaat een set van favoriete vacature-IDs op in SharedPreferences.
     *
     * @param jobIds De set van vacature-IDs die als favoriet worden opgeslagen.
     */
    fun saveJobFavorites(jobIds: Set<String>) {
        // Start een bewerkingssessie en slaat de set van favoriete vacatures op.
        sharedPreferences.edit()
            .putStringSet("favorite_jobs", jobIds)
            .apply() // Past de wijzigingen asynchroon toe.
        Log.d("JobSharedPrefManager", "Saving favorites: $jobIds")
    }

    /**
     * Stelt de favorietstatus van een specifieke vacature in.
     * Voegt een vacature-ID toe of verwijdert het uit de lijst van favorieten.
     *
     * @param jobId De ID van de vacature.
     * @param isFavorite Boolean waarde die aangeeft of de vacature favoriet moet zijn.
     */
    fun setJobFavorite(jobId: String, isFavorite: Boolean) {
        // Haalt de huidige lijst van favoriete vacatures op.
        val currentFavorites = getJobFavorites().toMutableSet()

        if (isFavorite) {
            // Voeg de vacature toe als favoriet.
            currentFavorites.add(jobId)
        } else {
            // Verwijder de vacature uit de lijst van favorieten.
            currentFavorites.remove(jobId)
        }

        // Sla de bijgewerkte lijst op in SharedPreferences.
        saveJobFavorites(currentFavorites)
        Log.d("JobSharedPrefManager", "Set job $jobId as favorite: $isFavorite")
    }

    /**
     * Haalt de set van favoriete vacatures op uit SharedPreferences.
     *
     * @return Een set van vacature-IDs die als favoriet zijn gemarkeerd.
     */
    fun getJobFavorites(): Set<String> {
        return sharedPreferences.getStringSet("favorite_jobs", emptySet()) ?: emptySet()
    }

    /**
     * Controleert of een specifieke vacature als favoriet is gemarkeerd.
     *
     * @param jobId De ID van de vacature die gecontroleerd wordt.
     * @return Boolean waarde die aangeeft of de vacature favoriet is.
     */
    fun isJobFavorite(jobId: String): Boolean {
        val isFavorite = getJobFavorites().contains(jobId)
        Log.d("JobSharedPrefManager", "Is job $jobId favorite? $isFavorite")
        return isFavorite
    }

    /**
     * Voegt een vacature toe aan de lijst van favorieten.
     *
     * @param jobId De ID van de vacature die toegevoegd moet worden.
     */
    fun addJobFavorite(jobId: String) {
        setJobFavorite(jobId, true)
    }

    /**
     * Verwijdert een vacature uit de lijst van favorieten.
     *
     * @param jobId De ID van de vacature die verwijderd moet worden.
     */
    fun removeJobFavorite(jobId: String) {
        setJobFavorite(jobId, false)
    }
}
