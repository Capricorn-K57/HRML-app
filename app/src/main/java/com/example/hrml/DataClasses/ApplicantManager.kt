package com.example.hrml.DataClasses.ApplicantManager

import android.content.Context
import android.util.Log
import com.example.hrml.ui.home.Applicant
import com.example.hrml.ui.job_details.ApplicantFavoritesManager
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * ApplicantManager is verantwoordelijk voor het ophalen, verwerken en beheren van sollicitanten
 * die aan een specifieke vacature gekoppeld zijn. Het ondersteunt het ophalen van gegevens van een
 * externe API en het toggelen van de favorietstatus van sollicitanten.
 *
 * @param context Context voor toegang tot bronnen zoals gedeelde voorkeuren.
 */
class ApplicantManager(private val context: Context) {

    // OkHttpClient wordt gebruikt voor netwerkcommunicatie.
    private val client = OkHttpClient()

    /**
     * Haalt de lijst met sollicitanten op voor een specifieke vacature.
     * Maakt een asynchrone API-aanroep en retourneert de resultaten via een callback.
     *
     * @param jobId De unieke ID van de vacature waarvoor sollicitanten worden opgehaald.
     * @param callback Een lambda die de lijst van sollicitanten retourneert of null bij een fout.
     */
    fun fetchApplicantsForJob(jobId: String, callback: (List<Applicant>?) -> Unit) {
        // De URL van de API voor het ophalen van sollicitanten voor de gegeven vacature.
        val url =
            "https://hrml-t4-system-p4wm.onrender.com/get_vacancy_applicants?vacature_id=$jobId"
        Log.d("ApplicantManager", "Fetching applicants for jobId: $jobId with URL: $url")

        // Bouwt de HTTP GET-aanvraag.
        val request = Request.Builder().url(url).get().build()

        // Voert de netwerkoproep asynchroon uit.
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Logt en behandelt een fout bij het uitvoeren van de aanvraag.
                Log.e("ApplicantManager", "Failed to fetch applicants for jobId: $jobId", e)
                callback(null) // Retourneert null via de callback bij een fout.
            }

            override fun onResponse(call: Call, response: Response) {
                // Haalt de response body op.
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    // Parseert de response en geeft de sollicitantenlijst door via de callback.
                    val applicants = parseApplicants(responseBody, jobId)
                    callback(applicants)
                } else {
                    // Logt een fout als de response niet succesvol is.
                    Log.e("ApplicantManager", "Failed to fetch applicants: ${response.message}")
                    callback(null)
                }
            }
        })
    }

    /**
     * Parseert een JSON-respons naar een lijst van Applicant-objecten.
     *
     * @param responseBody De JSON-string met sollicitantgegevens.
     * @param jobId De ID van de vacature waarvoor sollicitanten worden opgehaald.
     * @return Een lijst met Applicant-objecten.
     */
    private fun parseApplicants(responseBody: String, jobId: String): List<Applicant> {
        val applicants = mutableListOf<Applicant>()

        // Converteert de JSON-string naar een JSONArray.
        val jsonArray = JSONArray(responseBody)
        Log.d("ApplicantManager", "Parsing ${jsonArray.length()} applicants from response")

        // Itereert door elke JSONObject in de JSONArray.
        for (i in 0 until jsonArray.length()) {
            try {
                val jsonObject = jsonArray.getJSONObject(i)

                // Controleert of de sollicitant favoriet is.
                val isFavorite = ApplicantFavoritesManager.isFavorite(
                    context,
                    jobId,
                    jsonObject.getString("user_id")
                )

                // Maakt een Applicant-object van de JSON-gegevens.
                val applicant = Applicant(
                    id = jsonObject.getString("user_id"),
                    name = jsonObject.getString("name"),
                    matchingScore = jsonObject.optDouble("matchingscore", 0.0),
                    isFavorite = isFavorite
                )
                Log.d("ApplicantManager", "Parsed applicant: $applicant")

                // Voegt de parsed sollicitant toe aan de lijst.
                applicants.add(applicant)
            } catch (e: Exception) {
                // Logt fouten bij het parsen van een specifieke sollicitant.
                Log.e("ApplicantManager", "Error parsing applicant at index $i", e)
            }
        }
        return applicants
    }

    /**
     * Wijzigt de favorietstatus van een sollicitant en slaat de nieuwe status op.
     *
     * @param applicant Het Applicant-object waarvan de favorietstatus wordt gewijzigd.
     * @param jobId De ID van de vacature waaraan de sollicitant gekoppeld is.
     */
    fun toggleFavoriteStatus(applicant: Applicant, jobId: String) {
        // Wisselt de huidige favorietstatus van de sollicitant.
        val currentStatus = applicant.isFavorite
        val newStatus = !currentStatus
        applicant.isFavorite = newStatus

        // Slaat de nieuwe favorietstatus op via ApplicantFavoritesManager.
        ApplicantFavoritesManager.setFavorite(context, jobId, applicant.id, newStatus)

        Log.d(
            "ApplicantManager",
            "Toggled favorite status for applicant ${applicant.id} in job $jobId to $newStatus"
        )
    }
}
