package com.astraveil.app.ui.design

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glass card — legacy container, unified into [AstraCard] (P2-17).
 *
 * Delegates to [AstraCard] with `tier = SurfaceTier.CONTENT`. New code
 * should call [AstraCard] directly; this shim exists only so existing
 * call sites keep compiling without a flag-day rewrite.
 *
 * @deprecated Use [AstraCard] with `tier = SurfaceTier.CONTENT`.
 */
@Deprecated(
    "Unified into AstraCard (P2-17). Use AstraCard(tier = SurfaceTier.CONTENT).",
    ReplaceWith("AstraCard(modifier, contentPadding = contentPadding.dp, content = content)"),
)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    contentPadding: Int = 20,
    content: @Composable ColumnScope.() -> Unit,
) = AstraCard(
    modifier = modifier,
    tier = SurfaceTier.CONTENT,
    contentPadding = contentPadding.dp,
    content = content,
)
