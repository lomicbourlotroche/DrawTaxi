package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.drawtaxi.app.ui.components.core.DrawTaxiText
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
import com.drawtaxi.app.ui.theme.drawTaxiColors

@Composable
fun DrawTaxiScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Column(modifier = modifier.fillMaxSize().background(drawTaxiColors().background)) {
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

@Preview(showBackground = true)
@Composable
fun DrawTaxiScaffoldPreview() {
    DrawTaxiTheme {
        DrawTaxiScaffold(
            topBar = {
                DrawTaxiTopBar(title = { DrawTaxiTopBarTitle("Accueil") })
            },
            content = {
                DrawTaxiText("Contenu principal")
            }
        )
    }
}