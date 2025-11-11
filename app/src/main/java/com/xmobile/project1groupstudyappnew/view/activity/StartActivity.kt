package com.xmobile.project1groupstudyappnew.view.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityStartBinding
import java.util.Locale

class StartActivity : BaseActivity() {

    private lateinit var binding: ActivityStartBinding
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initCloudinary()
        setupButtons()
        listenAuthState()
        createNotificationChannel()
        applyUserPreferences()
    }

    private fun initCloudinary() {
        try {
            MediaManager.get()
        } catch (_: IllegalStateException) {
            val config = hashMapOf("cloud_name" to "du9rpxawb")
            MediaManager.init(this, config)
        }
    }

    private fun setupButtons() {
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun listenAuthState() {
        firebaseAuth.addAuthStateListener { auth ->
            auth.currentUser?.let {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "group_channel",
                "Group Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyUserPreferences() {
        val prefs = getSharedPreferences("choose", MODE_PRIVATE)
        applyTheme(prefs.getInt("chooseTheme", R.string.light))
        applyLanguage(prefs.getInt("chooseLanguage", 0))
    }

    private fun applyTheme(themeId: Int) {
        val themeStr = getString(themeId)
        when (themeStr) {
            getString(R.string.dark) -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.Theme_Project1GroupStudyAppNew_Dark)
            }
            getString(R.string.light) -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.Theme_Project1GroupStudyAppNew)
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                setTheme(
                    if (nightMode == Configuration.UI_MODE_NIGHT_YES)
                        R.style.Theme_Project1GroupStudyAppNew_Dark
                    else
                        R.style.Theme_Project1GroupStudyAppNew
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLanguage(langId: Int) {
        val langCode = when (if (langId == 0) "vi" else getString(langId)) {
            getString(R.string.vietnamese) -> "vi"
            getString(R.string.english) -> "en"
            else -> "vi"
        }

        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
