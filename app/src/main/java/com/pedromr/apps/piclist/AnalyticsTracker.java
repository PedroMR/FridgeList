package com.pedromr.apps.piclist;

import android.os.Bundle;

/**
 * Created by pedro on 12/21/17.
 */

public abstract class AnalyticsTracker {

    public enum Event {
        ACTION_DRAW,
        ACTION_ERASE,
        BTN_MODE_DRAW,
        BTN_MODE_ERASE,
        BTN_CLEARALL,
        DLG_CLEARALL_CONFIRM,
        DLG_CLEARALL_CANCEL,
        BTN_UNDO,
        BTN_ABOUT,
        APP_OPEN,
        DLG_CAMERA_PERMISSION_ACCEPTED, DLG_CAMERA_PERMISSION, ACTION_PICTURE_TAKEN, BTN_PICTURE
    }

    public abstract void logEvent(Event event, Bundle parameters);
}
