package com.pedromr.apps.piclist;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by pedro on 12/21/17.
 */

public class FirebaseAnalyticsTracker extends AnalyticsTracker {
    private final FirebaseAnalytics firebaseAnalytics;

    public FirebaseAnalyticsTracker(FirebaseAnalytics firebaseAnalytics) {
        this.firebaseAnalytics = firebaseAnalytics;
    }

    @Override
    public void logEvent(Event event, Bundle parameters) {
        firebaseAnalytics.logEvent(event.name(), parameters);
    }
}
