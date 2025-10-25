package com.example.safeher.ui.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.safeher.data.model.LiveLocation
import androidx.compose.runtime.key

private const val TAG = "StaticMapPreview"

@Composable
fun StaticMapPreview(
    location: LiveLocation?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Log.d(TAG, "Composable recomposed. Location: $location")

    val locationKey = remember(location) {
        "${location?.location?.latitude}_${location?.location?.longitude}"
    }

    val mapHtml = remember(location) {
        if (location?.location != null) {
            val lat = location.location.latitude
            val lon = location.location.longitude
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    html, body {
                        width: 100vw;
                        height: 100vh;
                        overflow: hidden;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: 0;
                        display: block;
                    }
                </style>
                <script>
                    function setupLinkHandler() {X  
                        try {
                            const iframe = document.querySelector('iframe');
                            if (iframe && iframe.contentWindow) {
                                iframe.contentWindow.addEventListener('click', function(event) {
                                    const link = event.target.closest('a');
                                    if (link && link.href && link.href.startsWith('intent://')) {
                                        window.android.onLinkClicked(link.href);
                                        event.preventDefault();
                                    }
                                }, true);
                            }
                        } catch (e) {
                            console.error('Error accessing iframe: ', e);
                        }
                    }
                    window.addEventListener('load', setupLinkHandler);
                </script>
            </head>
            <body>
                <iframe
                    src="https://maps.google.com/maps?q=$lat,$lon&z=15&output=embed"
                    allowfullscreen
                    loading="lazy">
                </iframe>
            </body>
            </html>
            """.trimIndent().trimIndent()
        } else {
            Log.w(TAG, "Location or location.location is null. Cannot generate map HTML.")
            null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (mapHtml != null && location?.location != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                key(locationKey) {
                    Log.d(TAG, "Displaying WebView for location: $locationKey")
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportMultipleWindows(true)
                                    allowFileAccess = true
                                    allowContentAccess = true
                                }
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onLinkClicked(href: String) {
                                        Log.d(TAG, "JavaScript interface received link: $href")
                                        if (href.startsWith("intent://")) {
                                            try {
                                                val intentUri = Uri.parse(href)
                                                val query = intentUri.getQueryParameter("q")
                                                Log.d(TAG, "Extracted query parameter: $query")
                                                if (query != null) {
                                                    val latLon = query.split(",").map { it.toDoubleOrNull() }
                                                    if (latLon.size == 2 && latLon[0] != null && latLon[1] != null) {
                                                        val lat = latLon[0]!!
                                                        val lon = latLon[1]!!
                                                        Log.d(TAG, "Launching map with lat: $lat, lon: $lon")
                                                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon"))
                                                        mapIntent.setPackage("com.google.android.apps.maps")
                                                        context.startActivity(mapIntent)
                                                    } else {
                                                        Log.w(TAG, "Invalid lat/lon in query: $query")
                                                    }
                                                }
                                                // Fallback to original location
                                                val fallbackLat = location.location.latitude
                                                val fallbackLon = location.location.longitude
                                                Log.d(TAG, "Falling back to original location: lat=$fallbackLat, lon=$fallbackLon")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$fallbackLat,$fallbackLon?q=$fallbackLat,$fallbackLon"))
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(mapIntent)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error handling intent URL: ${e.message}", e)
                                                Toast.makeText(context, "Failed to open map. Please install Google Maps.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }, "android")
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        Log.d(TAG, "WebView page finished loading: $url")
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        Log.d(TAG, "Attempting to override URL loading: $url")
                                        if (url?.startsWith("intent://") == true) {
                                            Log.d(TAG, "Intercepting intent URL: $url")
                                            return true
                                        }
                                        return false
                                    }
                                }

                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: Message?
                                    ): Boolean {
                                        Log.d(TAG, "New window creation attempted - opening Google Maps")

                                        // Open Google Maps with the location
                                        val lat = location.location.latitude
                                        val lon = location.location.longitude
                                        val mapIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                                        )
                                        mapIntent.setPackage("com.google.android.apps.maps")

                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: ActivityNotFoundException) {
                                            Log.e(TAG, "Google Maps not installed", e)
                                            Toast.makeText(
                                                context,
                                                "Google Maps app not installed.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        return true
                                    }
                                }
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { webView ->
                            Log.d(TAG, "WebView update called. Loading data.")
                            webView.loadDataWithBaseURL(
                                "https://www.google.com",
                                mapHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    )
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        val lat = location.location.latitude
                        val lon = location.location.longitude
                        val intentUri = Uri.parse("google.navigation:q=$lat,$lon")
                        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: ActivityNotFoundException) {
                            Log.e(TAG, "Failed to start Google Maps activity.", e)
                            Toast.makeText(context, "Google Maps app not installed.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    text = { Text("Directions") },
                    icon = { Icon(Icons.Outlined.Directions, contentDescription = "Directions") }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Log.d(TAG, "Displaying 'No location to display.' message.")
                Text("No location to display.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}