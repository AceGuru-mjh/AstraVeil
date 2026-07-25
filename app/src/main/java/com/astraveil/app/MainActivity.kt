package com.astraveil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.astraveil.app.ui.AstraVeilApp
import com.astraveil.app.ui.theme.AstraVeilTheme

/**
 * The single Compose activity hosting AstraUI.
 *
 * Edge-to-edge is enabled with default dark scrims so the deep AstraVeil
 * background bleeds through the system bars. The window is then handed to
 * [AstraVeilApp] which sets up the Scaffold + NavHost.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Deep dark edge-to-edge: status & nav bars translucent over the
        // AstraVeil background.
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
