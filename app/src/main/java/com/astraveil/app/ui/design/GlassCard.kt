package com.astraveil.app.ui.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A glass card with built-in padding. This is the container every
 * dashboard section (System Status, Capabilities, Provider, ...)
 * should use instead of Material3 [androidx.compose.material3.Card].
 *
 * Glass is the container; M3 components (Text, Icon, Button) handle
 * the interaction inside.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    contentPadding: Int = 20,
    content: @Composable () -> Unit,
) {
    GlassSurface(
        modifier = modifier.padding(PaddingValues(2.dp)),
        cornerRadius = cornerRadius,
    ) {
        Column(modifier = Modifier.padding(contentPadding.dp)) {
            content()
        }
    }
}
