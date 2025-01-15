package com.example.hrml

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.hrml.ui.login.LoginManager

// LauncherScreen is de opstartschermactiviteit die kort wordt weergegeven bij het opstarten van de app
class LauncherScreen : AppCompatActivity() {

    private lateinit var loginManager: LoginManager // LoginManager voor het beheren van inlogstatus en loginfunctionaliteit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schakelt edge-to-edge weergeven in voor een moderne, randloze gebruikersinterface
        enableEdgeToEdge()

        // Bepaal de layout voor dit scherm
        setContentView(R.layout.activity_launcher_screen)

        // Initialiseer de LoginManager
        loginManager = LoginManager(this)

        // Pas systeeminvoegingen toe voor een randloze ervaring die rekening houdt met de systeemstaven (zoals de statusbalk)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Start MainActivity na een vertraging van 2000 ms om een splash-effect te creëren
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java) // Maak een intent aan om MainActivity te starten
            startActivity(intent)                               // Start MainActivity
            finish()                                            // Sluit het huidige (LauncherScreen) activiteit af
        }, 2000) // Splash vertraging van 2000 ms (2 seconden)
    }
}
