package com.drawtaxi.app.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory

private const val DEFAULT_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun rememberMapView(context: Context): MapView {
    return remember {
        MapView(context)
    }
}

@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition? = null,
    styleUrl: String = DEFAULT_STYLE,
    isMyLocationEnabled: Boolean = false,
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val context = LocalContext.current
    val mapView = rememberMapView(context)
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.getMapAsync { mapLibreMap ->
                mapLibreMap.setStyle(styleUrl)
                mapRef = mapLibreMap
                if (cameraPosition != null) {
                    mapLibreMap.cameraPosition = cameraPosition
                }
                onMapReady(mapLibreMap)
            }
            mapView
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
