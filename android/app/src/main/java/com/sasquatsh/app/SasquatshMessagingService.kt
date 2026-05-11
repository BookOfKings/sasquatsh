package com.sasquatsh.app

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SasquatshMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle push notifications (chat messages, event updates, etc.)
        super.onMessageReceived(remoteMessage)
    }

    override fun onNewToken(token: String) {
        // FCM token refreshed — could send to backend for push targeting
        super.onNewToken(token)
    }
}
