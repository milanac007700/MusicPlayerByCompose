package com.milanac007.demo.musicplayerbycompose.ui.components

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.milanac007.demo.musicplayerbycompose.viewModels.MusicViewModel

@Composable
fun MusicApp(
    viewModel: MusicViewModel,
    onPermissionGranted: ()-> Unit,
) {
    RequestPermisssion(viewModel, onPermissionGranted)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermisssion(
    viewModel: MusicViewModel,
    onPermissionGranted: ()-> Unit
) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        val permissionState = rememberPermissionState(READ_EXTERNAL_STORAGE)
        if (permissionState.status != PermissionStatus.Granted) {
            SideEffect {
                permissionState.launchPermissionRequest()
            }
        } else {
            onPermissionGranted()
            MusicMainScreen(viewModel)
        }
    } else {
        val permissionState = rememberMultiplePermissionsState(listOf(POST_NOTIFICATIONS ,READ_MEDIA_AUDIO, READ_MEDIA_IMAGES))
        if (!permissionState.allPermissionsGranted) {
            SideEffect {
                permissionState.launchMultiplePermissionRequest()
            }
        } else {
            onPermissionGranted()
            MusicMainScreen(viewModel)
        }
    }
}
