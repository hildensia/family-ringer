package com.familyringer;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Application class — signs in anonymously on first launch.
 * Anonymous auth gives every device a stable uid without requiring
 * the user to create an account.
 */
public class FamilyRingerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        // Sign in anonymously if not already signed in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                .addOnFailureListener(e ->
                    android.util.Log.e("FamilyRingerApp", "Anonymous auth failed", e));
        }
    }
}
