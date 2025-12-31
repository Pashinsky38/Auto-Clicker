package com.example.autoclicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnEnableAccessibility;
    private Button btnEnableOverlay;
    private Button btnSetPosition;
    private Button btnStartStop;
    private EditText etInterval;
    private EditText etRepeatCount;
    private TextView tvPosition;

    private int clickX = -1;
    private int clickY = -1;

    private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message != null) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                updateStartStopButton();
            }
        }
    };

    private BroadcastReceiver positionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clickX = intent.getIntExtra("x", -1);
            clickY = intent.getIntExtra("y", -1);
            updatePositionText();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter statusFilter = new IntentFilter("AutoClickerStatus");
        registerReceiver(statusReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED);

        IntentFilter positionFilter = new IntentFilter("PositionSelected");
        registerReceiver(positionReceiver, positionFilter, Context.RECEIVER_NOT_EXPORTED);

        updateStartStopButton();
        checkPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(statusReceiver);
        unregisterReceiver(positionReceiver);
    }

    private void initViews() {
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility);
        btnEnableOverlay = findViewById(R.id.btnEnableOverlay);
        btnSetPosition = findViewById(R.id.btnSetPosition);
        btnStartStop = findViewById(R.id.btnStartStop);
        etInterval = findViewById(R.id.etInterval);
        etRepeatCount = findViewById(R.id.etRepeatCount);
        tvPosition = findViewById(R.id.tvPosition);
    }

    private void setupListeners() {
        btnEnableAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this,
                        "Please enable 'Auto Clicker' in Accessibility Settings",
                        Toast.LENGTH_LONG).show();
            }
        });

        btnEnableOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        Toast.makeText(MainActivity.this,
                                "Please grant overlay permission",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Overlay permission already granted",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnSetPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkOverlayPermission()) {
                    Toast.makeText(MainActivity.this,
                            "Please enable overlay permission first",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, OverlayService.class);
                intent.setAction("START_POSITION_SELECTION");
                startService(intent);

                Toast.makeText(MainActivity.this,
                        "Tap anywhere on screen to set position",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AutoClickerService service = AutoClickerService.getInstance();
                if (service == null) {
                    Toast.makeText(MainActivity.this,
                            "Please enable Accessibility Service first",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (clickX < 0 || clickY < 0) {
                    Toast.makeText(MainActivity.this,
                            "Please set a click position first",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (service.isClicking()) {
                    service.stopClicking();
                } else {
                    try {
                        long interval = Long.parseLong(etInterval.getText().toString());
                        int repeatCount = Integer.parseInt(etRepeatCount.getText().toString());

                        if (interval < 100) {
                            Toast.makeText(MainActivity.this,
                                    "Interval must be at least 100ms",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        service.setClickPosition(clickX, clickY);
                        service.setClickInterval(interval);
                        service.setRepeatCount(repeatCount);
                        service.startClicking();
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this,
                                "Please enter valid numbers",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void checkPermissions() {
        boolean accessibilityEnabled = AutoClickerService.isServiceEnabled();
        boolean overlayEnabled = checkOverlayPermission();

        btnEnableAccessibility.setEnabled(!accessibilityEnabled);
        btnEnableAccessibility.setText(accessibilityEnabled ?
                "Accessibility Enabled ✓" : "Enable Accessibility");

        btnEnableOverlay.setEnabled(!overlayEnabled);
        btnEnableOverlay.setText(overlayEnabled ?
                "Overlay Enabled ✓" : "Enable Overlay");

        if (overlayEnabled) {
            Intent intent = new Intent(this, OverlayService.class);
            intent.setAction("SHOW_FLOATING_BUTTON");
            startService(intent);
        }
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void updatePositionText() {
        if (clickX >= 0 && clickY >= 0) {
            tvPosition.setText(String.format(
                    getString(R.string.position_set), clickX, clickY));
        } else {
            tvPosition.setText("Not Set");
        }
    }

    private void updateStartStopButton() {
        AutoClickerService service = AutoClickerService.getInstance();
        if (service != null && service.isClicking()) {
            btnStartStop.setText(R.string.stop_clicking);
        } else {
            btnStartStop.setText(R.string.start_clicking);
        }
    }
}