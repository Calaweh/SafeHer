package com.example.safeher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.safeher.ui.navigation.App
import com.example.safeher.ui.theme.SafeHerTheme
import com.google.ai.client.generativeai.type.Schema.Companion.int
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.feature.dynamicinstall.FeatureCompat
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.setApiKey("DgEDADECaluZSQg97ljDr5L7mWEB2dnpdZfnKhmW7KYY0XiK7B6arrz+T+QraG7a7a/ZFMlX1bksd7hahKFIp6lrxrjKZsSsYlFEiA==")
        MapsInitializer.initialize(this)
        enableEdgeToEdge()
        setContent {
            SafeHerTheme {
                Surface(modifier = Modifier) {
                    val windowSize = calculateWindowSizeClass(this)
                    App(
                        windowSize = windowSize.widthSizeClass,
                        finishActivity = { finish() }
                    )
                }
            }
        }
    }
}