package com.example.hrml.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hrml.R

// Fragment dat de login-functionaliteit beheert
class LoginFragment : Fragment() {

    // LoginManager wordt gebruikt om de inlogstatus en login-functies te beheren
    private lateinit var loginManager: LoginManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate de layout voor dit fragment en retourneer de view
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    // Functie die wordt aangeroepen nadat de view is gecreëerd
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialiseer de loginManager met de context van het fragment
        loginManager = LoginManager(requireContext())

        // Controleer of de gebruiker al ingelogd is en navigeer direct door naar het home-fragment
        if (loginManager.isUserLoggedIn()) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        // Stel een click listener in voor de login-knop
        view.findViewById<View>(R.id.btn_login).setOnClickListener {
            // Haal het ingevoerde e-mailadres en wachtwoord op uit de EditText-velden
            val email = view.findViewById<EditText>(R.id.txt_email).text.toString()
            val password = view.findViewById<EditText>(R.id.txt_password).text.toString()

            // Roep de login-functie aan in loginManager met ingevoerd email en wachtwoord
            loginManager.login(email, password) { success ->
                // Update de UI op de hoofdthread
                requireActivity().runOnUiThread {
                    if (success) {
                        // Navigeer naar het home-fragment als de login succesvol is
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {
                        // Toon een foutmelding bij een mislukte login
                        Toast.makeText(requireContext(), "Ongeldig e-mailadres of wachtwoord", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
