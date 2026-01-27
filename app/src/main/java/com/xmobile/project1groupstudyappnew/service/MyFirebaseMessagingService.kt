package com.xmobile.project1groupstudyappnew.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToDatabase(token)
    }

    private fun saveTokenToDatabase(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseDatabase.getInstance().getReference("UserTokens")
        db.child(user.uid).setValue(token)
    }
}
