package com.astraveil.app

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.astraveil.app.ui.AstraVeilApp
import com.astraveil.app.ui.theme.AstraVeilTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("AstraVeil", "MainActivity.onCreate start")

        // Window-level background blur (API 31+).
        // This is the Android equivalent of CSS backdrop-filter: blur().
        // It blurs the content BEHIND the entire window, giving all
        // LiquidGlass surfaces a real frosted-glass background.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
            )
            window.attributes = window.attributes.apply {
                blurBehindRadius = 48
            }
        }

        try {
            setContent {
                AstraVeilTheme {
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
}
