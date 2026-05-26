package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DrawTaxiScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (topBar != null) {
            Box { topBar() }
        }
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            content(PaddingValues())
        }
        if (bottomBar != null) {
            Box { bottomBar() }
        }
    }
}
