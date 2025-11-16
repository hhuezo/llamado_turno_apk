package com.example.llamadoturno

import android.Manifest
import android.os.Build
import androidx.compose.runtime.*
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SolicitarPermisosBluetooth(onGranted: @Composable () -> Unit) {

    val permisos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // ANDROID 12+
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    } else {
        // ANDROID 11 o menor
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val state = rememberMultiplePermissionsState(permisos)

    LaunchedEffect(Unit) {
        state.launchMultiplePermissionRequest()
    }

    if (state.allPermissionsGranted) {
        onGranted()
    }
}
