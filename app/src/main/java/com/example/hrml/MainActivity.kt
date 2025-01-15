package com.example.hrml

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.hrml.databinding.ActivityMainBinding
import com.example.hrml.ui.login.LoginManager
import com.google.android.material.navigation.NavigationView

// Hoofdactiviteit van de app die de navigatie, uitlogfunctionaliteit en UI-elementen beheert
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration // Configuratie voor de navigatie in de ActionBar
    lateinit var binding: ActivityMainBinding // Binding om toegang te krijgen tot UI-elementen van de layout
    private lateinit var loginManager: LoginManager // LoginManager voor inlogbeheer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiseer de layoutbinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Stel de toolbar in als supportActionBar voor deze activiteit
        setSupportActionBar(binding.appBarMain.toolbar)

        // Initialiseer de LoginManager
        loginManager = LoginManager(this)

        // Verwijzingen naar de navigatie-elementen
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Stel de top-level bestemmingen in voor de AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_login, R.id.nav_home, R.id.nav_favorite, R.id.nav_job_details),
            binding.drawerLayout
        )

        // Koppel de ActionBar met de NavController om navigatie mogelijk te maken
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        // Voeg een click listener toe aan de uitlogknop in de navigatieheader
        val logoutImageView = navView.getHeaderView(0).findViewById<View>(R.id.btn_logout)
        logoutImageView.setOnClickListener {
            logoutUser()
        }

        // Update het e-mailadres in de navigatieheader bij het opstarten
        updateHeaderEmail()

        // Luister naar bestemmingveranderingen om de navigatiebalk en ActionBar dynamisch te beheren
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_login) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED) // Sluit de navigatiedrawer
                supportActionBar?.hide() // Verberg de ActionBar voor het inlogscherm
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED) // Ontgrendel de navigatiedrawer
                supportActionBar?.show() // Toon de ActionBar
            }
        }
    }

    // Functie om het e-mailadres in de navigatieheader bij te werken met de ingelogde gebruiker
    private fun updateHeaderEmail() {
        val navView: NavigationView = binding.navView
        val emailTextView = navView.getHeaderView(0).findViewById<TextView>(R.id.nav_header_email)

        val email = loginManager.getLoggedInEmail() ?: "Not logged in"
        Log.d("MainActivity", "Setting email in header: $email")
        emailTextView.text = email
    }


    // Functie om de gebruiker uit te loggen en terug te keren naar het inlogscherm
    private fun logoutUser() {
        // Voer de uitlogactie uit en verwijder de inloggegevens
        loginManager.logout()

        // Navigeer naar het inlogfragment en sluit de navigatiedrawer
        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_login)
        binding.drawerLayout.closeDrawers()

        // Verberg de ActionBar na het uitloggen
        supportActionBar?.hide()

        // Werk de navigatieheader bij om aan te geven dat de gebruiker is uitgelogd
        updateHeaderEmail()
    }

    // Creëer het menu in de ActionBar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true // Retourneer true om aan te geven dat het menu beschikbaar is
    }

    // Ondersteunt het navigeren terug naar hogere niveaus in de navigatiehiërarchie
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Beheer de acties voor geselecteerde items in het optiemenu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(binding.navView) // Open de navigatiedrawer bij selectie van het home-icoon
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
