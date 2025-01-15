package com.example.hrml.ui.login

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

// Klasse die verantwoordelijk is voor de login-functionaliteit
class LoginManager(private val context: Context) {

    // SharedPreferences om gebruikersinformatie lokaal op te slaan (bijvoorbeeld inlogstatus en e-mailadres)
    private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // OkHttpClient voor het uitvoeren van HTTP-verzoeken
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Zet de timeout in voor de verbinding
        .readTimeout(30, TimeUnit.SECONDS)     // Zet de timeout in voor het lezen van data
        .writeTimeout(30, TimeUnit.SECONDS)    // Zet de timeout in voor het schrijven van data
        .build()

    // Functie om in te loggen met e-mailadres en wachtwoord
    fun login(email: String, password: String, callback: (Boolean) -> Unit) {
        val url = "https://hrml-t4-system-p4wm.onrender.com/login"  // De URL van de login API
        val hashedPassword = hashPassword(password)  // Versleutel het wachtwoord met SHA-256
        val json = JSONObject().apply {
            put("email", email)  // Voeg het e-mailadres toe aan de JSON
            put("password", hashedPassword)  // Voeg het versleutelde wachtwoord toe aan de JSON
        }

        // Maak het request body aan voor de POST-aanroep
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url(url)  // Stel de URL in
            .post(body)  // Stel de body in voor een POST-aanroep
            .addHeader("Content-Type", "application/json")  // Voeg de content-type header toe
            .build()

        // Voer de HTTP-aanroep uit in een achtergrondthread
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("LoginManager", "Login request failed", e)  // Log de fout bij mislukking
                callback(false)  // Geef aan dat de login is mislukt
                // Toon een toastmelding op de UI-thread
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Inloggen mislukt", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()  // Haal de response body als string op
                Log.d("LoginManager", "Response: $responseBody")

                // Controleer of de response succesvol is (statuscode 200)
                if (response.isSuccessful) {
                    try {
                        val responseJson = JSONObject(responseBody)  // Parseer de JSON response
                        val message = responseJson.getString("message")  // Haal het bericht op uit de response
                        val role = responseJson.getString("role")  // Haal de gebruikersrol op

                        // Controleer of de login succesvol was en de rol correct is
                        if (message == "Login successful" && role == "recruiter") {
                            // Sla inlogstatus op in SharedPreferences
                            sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                            saveEmail(email)  // Sla het e-mailadres op in SharedPreferences
                            callback(true)  // Geef aan dat de login is gelukt
                            // Toon een toastmelding op de UI-thread bij succes
                            (context as? Activity)?.runOnUiThread {
                                Toast.makeText(context, "Inloggen gelukt", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("LoginManager", "Invalid credentials or role: $message, $role")  // Log als de credentials of rol ongeldig zijn
                            callback(false)  // Geef aan dat de login is mislukt
                            // Toon een foutmelding op de UI-thread
                            (context as? Activity)?.runOnUiThread {
                                Toast.makeText(context, "Ongeldig emailadres en/of wachtwoord", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LoginManager", "Error parsing response", e)  // Log fouten bij het parsen van de response
                        callback(false)  // Geef aan dat de login is mislukt
                        // Toon een foutmelding op de UI-thread
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "Error parsing response", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("LoginManager", "Login failed: ${response.code}, ${response.message}")  // Log bij mislukte response
                    callback(false)  // Geef aan dat de login is mislukt
                    // Toon een foutmelding op de UI-thread
                    (context as? Activity)?.runOnUiThread {
                        Toast.makeText(context, "Inloggen mislukt ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Controleert of de gebruiker al is ingelogd door de status uit SharedPreferences op te halen
    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("isLoggedIn", false)  // Retourneer de opgeslagen inlogstatus
    }

    // Haalt het e-mailadres van de ingelogde gebruiker op uit SharedPreferences
    fun getLoggedInEmail(): String? {
        return sharedPreferences.getString("loggedInEmail", null)  // Retourneer het opgeslagen e-mailadres
    }

    // Slaat het e-mailadres van de ingelogde gebruiker op in SharedPreferences
    private fun saveEmail(email: String) {
        sharedPreferences.edit().putString("loggedInEmail", email).apply()  // Bewaar het e-mailadres in SharedPreferences
    }

    // Logt de gebruiker uit door de inlogstatus en het e-mailadres uit SharedPreferences te verwijderen
    fun logout() {
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()  // Zet de inlogstatus op false
        sharedPreferences.edit().remove("loggedInEmail").apply()  // Verwijder het opgeslagen e-mailadres
    }

    // Versleutelt het wachtwoord met SHA-256 voor veilige overdracht van wachtwoorden
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")  // Haal een SHA-256 MessageDigest instance
        val hashBytes = digest.digest(password.toByteArray())  // Versleutel het wachtwoord
        return hashBytes.joinToString("") { "%02x".format(it) }  // Zet de bytes om in een hex string
    }
}
