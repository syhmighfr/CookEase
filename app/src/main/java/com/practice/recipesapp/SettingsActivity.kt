package com.practice.recipesapp

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchTheme: SwitchMaterial
    private lateinit var spinnerLanguage: Spinner
    private lateinit var etBudget: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale() // Load language
        setContentView(R.layout.activity_settings)

        switchTheme = findViewById(R.id.switchTheme)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        etBudget = findViewById(R.id.etBudget)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        switchTheme.isChecked = prefs.getBoolean("darkMode", false)
        etBudget.setText(prefs.getString("budget", ""))

        // Language Spinner
        val langOptions = arrayOf("English", "Malay")
        spinnerLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langOptions)

        val savedLang = prefs.getString("language", "en")
        spinnerLanguage.setSelection(if (savedLang == "ms") 1 else 0)

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            prefs.edit().putBoolean("darkMode", isChecked).apply()
        }

        btnSave.setOnClickListener {
            val selectedLang = if (spinnerLanguage.selectedItem.toString() == "Malay") "ms" else "en"
            setLocale(selectedLang)
            prefs.edit()
                .putString("language", selectedLang)
                .putString("budget", etBudget.text.toString().trim())
                .apply()

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            recreate() // Refresh UI
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putString("language", langCode).apply()
    }

    private fun loadLocale() {
        val lang = getSharedPreferences("settings", Context.MODE_PRIVATE).getString("language", "en")
        setLocale(lang ?: "en")
    }
}