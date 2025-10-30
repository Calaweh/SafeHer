package com.example.safeher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.safeher.ui.navigation.App
import com.example.safeher.ui.theme.SafeHerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.maps.MapsInitializer
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import kotlinx.coroutines.withContext
import com.google.firebase.FirebaseOptions


@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "HmsUpdateChecker"
        private const val HMS_MAP_MODULE = "HwMapKit"
        private const val HMS_LOCATION_MODULE = "HwLocationKit"
    }

    private var showBackgroundLocationDialog by mutableStateOf(false)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted && coarseLocationGranted) {
            Log.d(TAG, "Location permissions granted")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundLocationIfNeeded()
            }
        } else {
            Log.e(TAG, "Location permissions denied")
            Toast.makeText(this, "Location permissions are required for location sharing", Toast.LENGTH_LONG).show()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Background location granted")
        } else {
            Log.w(TAG, "Background location denied - location sharing may stop when app is closed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AGConnectServicesConfig.fromContext(this)

        checkAndRequestLocationPermissions()

        getToken()

        testRemoteConfig()

        setupRxJavaErrorHandler()

        val isArmArchitecture = Build.SUPPORTED_ABIS.any { it.contains("arm", ignoreCase = true) }

        if (isArmArchitecture) {
            try {
                MapsInitializer.setApiKey("DgEDADECaluZSQg97ljDr5L7mWEB2dnpdZfnKhmW7KYY0XiK7B6arrz+T+QraG7a7a/ZFMlX1bksd7hahKFIp6lrxrjKZsSsYlFEiA==")
                MapsInitializer.initialize(this)
                Log.i(TAG, "HMS Maps initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize HMS Maps", e)
            }
        } else {
            Log.w(TAG, "Skipping HMS Maps initialization: Unsupported architecture (${Build.SUPPORTED_ABIS.joinToString()})")
        }

        setContent {
            SafeHerTheme {
                Surface(modifier = Modifier) {
                    val windowSize = calculateWindowSizeClass(this)

                    // Only check for HMS Core updates on ARM devices
                    if (isArmArchitecture) {
                        checkAndPromptForHmsCoreUpdate()
                    }

                    App(
                        windowSize = windowSize.widthSizeClass,
                        finishActivity = { finish() }
                    )

                    if (showBackgroundLocationDialog) {
                        BackgroundLocationDialog(
                            onDismiss = { showBackgroundLocationDialog = false },
                            onConfirm = {
                                showBackgroundLocationDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun testRemoteConfig() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                remoteConfig.fetchAndActivate().await()
                val apiKey = remoteConfig.getString("GEMINI_API_KEY")

                val maskedKey = apiKey.take(6) + "..." + apiKey.takeLast(4)
                if (apiKey.isNotEmpty()) {
                    val maskedKey = apiKey.take(6) + "..." + apiKey.takeLast(4)
                    Log.d("RemoteConfigTest", "✅ Gemini API key fetched: $maskedKey (masked)")
                } else {
                    Log.e("RemoteConfigTest", "❌ No key found! Check Firebase Console.")
                }

            } catch (e: Exception) {
                Log.e("RemoteConfigTest", "❌ Error fetching key: ${e.message}")
            }
        }
    }

    private fun setupRxJavaErrorHandler() {
        try {
            RxJavaPlugins.setErrorHandler { throwable ->
                if (throwable is UndeliverableException) {
                    val cause = throwable.cause
                    if (cause is UnsatisfiedLinkError || cause?.message?.contains("dynamicloader") == true || cause?.message?.contains("apiLeveL") == true) {
                        Log.e(TAG, "Suppressed HMS-related UndeliverableException", throwable)
                        // Don't crash - expected on some devices/emulators or with loader issues
                    } else {
                        Thread.currentThread().uncaughtExceptionHandler?.uncaughtException(Thread.currentThread(), throwable)
                    }
                } else {
                    Thread.currentThread().uncaughtExceptionHandler?.uncaughtException(Thread.currentThread(), throwable)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set RxJava error handler", e)
        }
    }

    private val hmsUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "HMS Core update process successful. Retrying HMS-dependent actions.")
            Toast.makeText(this, "HMS Core updated. Please try again.", Toast.LENGTH_LONG).show()
            // Optionally restart activity or refresh map
        } else {
            Log.w(TAG, "HMS Core update was cancelled or failed. Result code: ${result.resultCode}")
            Toast.makeText(this, "HMS Core update is required for maps.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndPromptForHmsCoreUpdate() {
        val apiAvailability = HuaweiApiAvailability.getInstance()
        val resultCode = apiAvailability.isHuaweiMobileServicesAvailable(this)

        when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.i(TAG, "HMS Core is available. No action needed.")
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Log.w(TAG, "HMS Core version update is required.")
                showUpdateDialog(apiAvailability, resultCode)
            }
            ConnectionResult.SERVICE_MISSING -> {
                Log.w(TAG, "HMS Core is missing.")
                showUpdateDialog(apiAvailability, resultCode)
            }
            ConnectionResult.SERVICE_DISABLED -> {
                Log.w(TAG, "HMS Core is disabled.")
                showUpdateDialog(apiAvailability, resultCode)
            }
            else -> {
                Log.e(TAG, "An unrecoverable error occurred with HMS Core. Result code: $resultCode")
                Toast.makeText(this, "HMS services are not available on this device.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showUpdateDialog(apiAvailability: HuaweiApiAvailability, resultCode: Int) {
        val pendingIntent = apiAvailability.getErrPendingIntent(this, resultCode, 0)
        if (pendingIntent != null) {
            try {
                hmsUpdateLauncher.launch(
                    androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch HMS update prompt", e)
                Toast.makeText(this, "Failed to start HMS update. Please update manually via AppGallery.", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e(TAG, "No pending intent for HMS update")
            Toast.makeText(this, "Unable to update HMS Core automatically. Please check AppGallery.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appId = "115664543"

                val token = HmsInstanceId.getInstance(this@MainActivity).getToken(appId, "HCM")

                withContext(Dispatchers.Main) {
                    if (!token.isNullOrEmpty()) {
                        Log.i(TAG, "HMS Push Token: $token")
                    }
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Failed to get HMS token: ${e.message}")
            }
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocation || !coarseLocation) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Already have foreground location, check if we need background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundLocationIfNeeded()
            }
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!backgroundLocationGranted) {
                showBackgroundLocationDialog = true
            }
        }
    }
}

@Composable
fun BackgroundLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Background Location Access") },
        text = {
            Text("To share your location continuously, please select 'Allow all the time' in the next screen.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}