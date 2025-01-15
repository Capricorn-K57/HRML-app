package com.example.hrml.DataClasses.VacancyManager

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.hrml.ui.home.Job
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * VacancyManager is verantwoordelijk voor het ophalen van vacatures van een externe server.
 * Het verwerkt de verkregen gegevens en maakt een lijst van vacature-objecten (Job).
 *
 * @param context De context wordt gebruikt voor UI-gerelateerde acties, zoals het tonen van Toasts.
 */
class VacancyManager(private val context: Context) {

    // OkHttpClient voor het uitvoeren van HTTP-aanvragen met specifieke time-outinstellingen.
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Verbindt binnen 30 seconden.
        .readTimeout(30, TimeUnit.SECONDS)  // Leest binnen 30 seconden.
        .writeTimeout(30, TimeUnit.SECONDS)  // Schrijft binnen 30 seconden.
        .build()

    /**
     * Haalt vacatures op van de server en verwerkt de response om een lijst van Job-objecten te creëren.
     * Het resultaat wordt via een callback geretourneerd.
     *
     * @param callback Een lambda die een lijst van Job-objecten of null ontvangt bij een fout.
     */
    fun fetchVacancies(callback: (List<Job>?) -> Unit) {
        // De URL van de server waarmee de vacatures worden opgehaald.
        val url = "https://hrml-t4-system-p4wm.onrender.com/get_vacancy_titles_and_descriptions"

        // Bouwt de GET-aanvraag met de juiste URL en content header.
        val request = Request.Builder()
            .url(url)
            .get()  // Geeft aan dat het een GET-aanvraag is.
            .addHeader("Content-Type", "application/json")  // Specificeert dat de aanvraag JSON verwacht.
            .build()

        // Voert de aanvraag asynchroon uit.
        client.newCall(request).enqueue(object : Callback {
            // Foutbehandelingsmethode als de aanvraag mislukt.
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VacancyManager", "Fetch vacancies request failed", e)
                // Toont een foutmelding aan de gebruiker via een Toast.
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Vacatures ophalen mislukt", Toast.LENGTH_SHORT).show()
                }
                // Roept de callback aan met null om aan te geven dat de aanvraag niet geslaagd is.
                callback(null)
            }

            // Methode die wordt aangeroepen als de aanvraag succesvol is.
            override fun onResponse(call: Call, response: Response) {
                // Haalt de body van de response op als een string.
                val responseBody = response.body?.string()
                Log.d("VacancyManager", "Response: $responseBody")

                // Controleert of de response succesvol was.
                if (response.isSuccessful) {
                    try {
                        // Lijst van vacatures die uit de response worden opgebouwd.
                        val vacancies = mutableListOf<Job>()
                        val vacanciesArray = JSONArray(responseBody)

                        // Verwerkt elke vacature in de JSON-array.
                        for (i in 0 until vacanciesArray.length()) {
                            // Haalt de gegevens van elke vacature op.
                            val vacancyJson = vacanciesArray.getJSONObject(i)

                            // Maakt een nieuw Job-object met de gegevens van de vacature.
                            val job = Job(
                                title = vacancyJson.getString("functietitel"),
                                description = vacancyJson.getString("beschrijving"),
                                id = vacancyJson.getString("_id"),
                                reactions = vacancyJson.getInt("reactions"),
                                isFavorite = false  // Stelt de initiële favorietstatus in op false.
                            )

                            // Voegt de nieuwe vacature toe aan de lijst van vacatures.
                            vacancies.add(job)
                        }

                        // Roept de callback aan met de lijst van vacatures.
                        callback(vacancies)

                    } catch (e: Exception) {
                        // Logt en toont een foutmelding als er een fout optreedt tijdens het verwerken van de response.
                        Log.e("VacancyManager", "Error parsing response", e)
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show()
                        }
                        // Roept de callback aan met null als de verwerking mislukt.
                        callback(null)
                    }
                } else {
                    // Logt en toont een foutmelding als de HTTP response niet succesvol was.
                    Log.e("VacancyManager", "Fetch vacancies failed: ${response.code}, ${response.message}")
                    (context as? Activity)?.runOnUiThread {
                        Toast.makeText(context, "Vacatures ophalen mislukt ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    // Roept de callback aan met null om aan te geven dat de aanvraag niet succesvol was.
                    callback(null)
                }
            }
        })
    }
}
