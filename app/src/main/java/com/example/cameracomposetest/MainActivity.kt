package com.example.cameracomposetest

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.view.WindowCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContent {
            CameraView()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraView() {
    var permissionAlreadyRequested by rememberSaveable { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA) {
        permissionAlreadyRequested = true
    }

    if (!permissionAlreadyRequested) {
        SideEffect { cameraPermissionState.launchPermissionRequest() }
    }

    if (cameraPermissionState.status == PermissionStatus.Granted) {
        Camera()
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun Camera(
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var checked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
    ) {
        val context = LocalContext.current
        val previewView = remember {
            PreviewView(context).also {
                it.scaleType = PreviewView.ScaleType.FILL_CENTER
                it.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                it.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }
        val preview = remember { Preview.Builder().build() }

        val imageCapture = remember { ImageCapture.Builder().build() }

        val imageAnalyzer = remember { ImageAnalysis.Builder().build() }

        val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }

        LaunchedEffect(cameraSelector) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.await()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("DEBUG", "Use case binding failed", exc)
            }
        }

        LaunchedEffect(checked) {
            if (checked) {
                Log.d("DEBUG", "setSurfaceProvider to null")
                preview.setSurfaceProvider(null) // This doesn't work, camera is not paused
            } else {
                Log.d("DEBUG", "resume output to camera")
                preview.setSurfaceProvider(previewView.surfaceProvider)
            }
        }

        AndroidView(factory = { previewView })

        // Toggle button for pausing & resuming camera
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                }
            )
        }
    }
}
