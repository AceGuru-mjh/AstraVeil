package com.astraveil.app

import android.os.Bundle
import android.util.Log
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
