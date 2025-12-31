package com.example.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AutoClickerService extends AccessibilityService {

    private static AutoClickerService instance;
    private Handler handler;
    private Runnable clickRunnable;
    private boolean isClicking = false;

    private int clickX = -1;
    private int clickY = -1;
    private long clickInterval = 1000;
    private int repeatCount = 0;
    private int currentClickCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to handle accessibility events for auto-clicking
    }

    @Override
    public void onInterrupt() {
        stopClicking();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopClicking();
        instance = null;
    }

    public static AutoClickerService getInstance() {
        return instance;
    }

    public static boolean isServiceEnabled() {
        return instance != null;
    }

    public void setClickPosition(int x, int y) {
        this.clickX = x;
        this.clickY = y;
    }

    public void setClickInterval(long interval) {
        this.clickInterval = interval;
    }

    public void setRepeatCount(int count) {
        this.repeatCount = count;
    }

    public void startClicking() {
        if (clickX < 0 || clickY < 0) {
            sendStatusUpdate("Please set a click position first");
            return;
        }

        if (isClicking) {
            return;
        }

        isClicking = true;
        currentClickCount = 0;

        clickRunnable = new Runnable() {
            @Override
            public void run() {
                if (isClicking) {
                    performClick(clickX, clickY);
                    currentClickCount++;

                    // Check if we should continue clicking
                    if (repeatCount == 0 || currentClickCount < repeatCount) {
                        handler.postDelayed(this, clickInterval);
                    } else {
                        stopClicking();
                        sendStatusUpdate("Clicking completed: " + currentClickCount + " clicks");
                    }
                }
            }
        };

        handler.post(clickRunnable);
        sendStatusUpdate("Clicking started");
    }

    public void stopClicking() {
        if (clickRunnable != null) {
            handler.removeCallbacks(clickRunnable);
        }
        isClicking = false;
        sendStatusUpdate("Clicking stopped");
    }

    public boolean isClicking() {
        return isClicking;
    }

    private void performClick(int x, int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 1));

        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
            }
        }, null);
    }

    private void sendStatusUpdate(String message) {
        Intent intent = new Intent("AutoClickerStatus");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }
}