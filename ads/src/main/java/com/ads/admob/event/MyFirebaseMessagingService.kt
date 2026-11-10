package com.ads.admob.event

import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "FCMService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit {
            putString(KEY_FCM_TOKEN, token)
        }
        Log.d(tag, "FCM registration token updated")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data.containsKey(UNINSTALL_TRACKING_KEY)) {
            Log.d(tag, "Ignored Adjust uninstall-tracking ping")
            return
        }
        Log.d(tag, "FCM message received from: ${message.from}")
    }

    companion object {
        private const val PREFERENCES_NAME = "fcm_registration"
        private const val KEY_FCM_TOKEN = "token"
        private const val UNINSTALL_TRACKING_KEY = "af-uinstall-tracking"

        fun getCurrentToken(context: android.content.Context): String? =
            context.applicationContext
                .getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
    }
}
