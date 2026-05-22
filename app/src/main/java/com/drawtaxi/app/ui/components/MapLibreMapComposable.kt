package com.drawtaxi.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory

private const val DEFAULT_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition? = null,
    styleUrl: String = DEFAULT_STYLE,
    isMyLocationEnabled: Boolean = false,
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.let {
                it.onPause()
                it.onStop()
                it.onDestroy()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(it).apply {
                onCreate(null)
                onStart()
                onResume()
                getMapAsync { mapLibreMap ->
                    mapLibreMap.setStyle(styleUrl)
                    mapRef = mapLibreMap
                    if (cameraPosition != null) {
                        mapLibreMap.cameraPosition = cameraPosition
                    }
                    onMapReady(mapLibreMap)
                }
                mapViewRef = this
            }
        },
        update = {
            cameraPosition?.let { pos ->
                mapRef?.let { map ->
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(pos))
                }
            }
        }
    )
}
