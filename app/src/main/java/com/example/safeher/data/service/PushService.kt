package com.example.safeher.data.service

import android.util.Log
import com.huawei.hms.push.HmsMessageService

class PushService : HmsMessageService() {

    private val TAG = "PushService"

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        if (!token.isNullOrEmpty()) {
            Log.i(TAG, "onNewToken: $token")
        }
    }

    override fun onMessageReceived(remoteMessage: com.huawei.hms.push.RemoteMessage?) {
        super.onMessageReceived(remoteMessage)

        remoteMessage?.let {
            if (it.data.isNotEmpty()) {
                Log.d(TAG, "Message data payload: " + it.data)

            }
        }
    }
}