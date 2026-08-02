package com.astraveil.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.astraveil.app.ui.AstraVeilApp
import com.astraveil.app.ui.theme.AstraVeilTheme
import com.astraveil.app.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        results.forEach { (perm, granted) ->
            Log.i("MainActivity", "Permission $perm → $granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.i("AstraVeil", "MainActivity.onCreate start")

        requestNeededPermissions()

        // Read the persisted theme mode so the user's PreferencesScreen
        // choice actually drives the color scheme. Recreating the Activity
        // (which PreferencesScreen does on theme change) re-reads this.
        val themeMode = ThemeMode.fromString(
            getSharedPreferences("astra_ui_prefs", MODE_PRIVATE)
                .getString("theme_mode", "dark")
        )

        try {
            setContent {
                AstraVeilTheme(themeMode = themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AstraVeilApp()
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("AstraVeil", "setContent failed", t)
        }

        Log.i("AstraVeil", "MainActivity.onCreate end")
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()

        // Notification (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Legacy storage (API 23-32 only, for directory scanning)
        // .avm import uses SAF (OpenDocument) which needs NO storage permission.
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
