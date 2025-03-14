package com.example.telepathy;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class TelepathyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}