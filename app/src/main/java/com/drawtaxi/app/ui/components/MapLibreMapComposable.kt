package com.drawtaxi.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle

private const val DEFAULT_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    cameraState: CameraState = rememberCameraState(),
    styleUrl: String = DEFAULT_STYLE,
    options: MapOptions = MapOptions(),
    content: @Composable () -> Unit = {}
) {
    MaplibreMap(
        modifier = modifier,
        cameraState = cameraState,
        baseStyle = BaseStyle.Uri(styleUrl),
        options = options,
        content = content
    )
}
