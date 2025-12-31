package com.example.autoclicker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private View targetView;
    private boolean isSelectingPosition = false;
    private static final String CHANNEL_ID = "AutoClickerChannel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_POSITION_SELECTION".equals(action)) {
                startPositionSelection();
            } else if ("STOP_POSITION_SELECTION".equals(action)) {
                stopPositionSelection();
            } else if ("SHOW_FLOATING_BUTTON".equals(action)) {
                showFloatingButton();
            } else if ("HIDE_FLOATING_BUTTON".equals(action)) {
                hideFloatingButton();
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Auto Clicker Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto Clicker")
                .setContentText("Service is running")
                .setSmallIcon(android.R.drawable.ic_menu_preferences)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void startPositionSelection() {
        if (targetView != null) {
            return;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.argb(128, 0, 0, 0));

        TextView textView = new TextView(this);
        textView.setText("Tap anywhere to set click position");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(20);
        textView.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        layout.addView(textView, textParams);

        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getRawX();
                    int y = (int) event.getRawY();

                    AutoClickerService service = AutoClickerService.getInstance();
                    if (service != null) {
                        service.setClickPosition(x, y);
                    }

                    Intent broadcastIntent = new Intent("PositionSelected");
                    broadcastIntent.putExtra("x", x);
                    broadcastIntent.putExtra("y", y);
                    sendBroadcast(broadcastIntent);

                    stopPositionSelection();
                    return true;
                }
                return false;
            }
        });

        targetView = layout;
        windowManager.addView(targetView, params);
        isSelectingPosition = true;
    }

    private void stopPositionSelection() {
        if (targetView != null) {
            windowManager.removeView(targetView);
            targetView = null;
            isSelectingPosition = false;
        }
    }

    private void showFloatingButton() {
        if (overlayView != null) {
            return;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        FrameLayout layout = new FrameLayout(this);
        TextView button = new TextView(this);
        button.setText("AC");
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.parseColor("#FF6200EE"));
        button.setPadding(40, 40, 40, 40);
        button.setTextSize(18);

        layout.addView(button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OverlayService.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        layout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

        overlayView = layout;
        windowManager.addView(overlayView, params);
    }

    private void hideFloatingButton() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPositionSelection();
        hideFloatingButton();
    }
}