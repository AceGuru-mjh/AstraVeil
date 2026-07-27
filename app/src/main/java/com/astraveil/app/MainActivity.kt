package com.astraveil.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.astraveil.app.ui.AstraVeilApp
import com.astraveil.app.ui.theme.AstraVeilTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("AstraVeil", "MainActivity.onCreate")

        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            Log.w("AstraVeil", "enableEdgeToEdge failed", t)
        }

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
    }
}
