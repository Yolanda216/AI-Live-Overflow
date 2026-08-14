package com.example.deskpet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileWriter;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "pet_overlay_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int PET_SIZE_DP = 180;
    private static final int PET_HEIGHT_DP = 240;

    private WindowManager windowManager;
    private WebView overlayView;
    private WindowManager.LayoutParams params;

    private int initialX = 0;
    private int initialY = 0;
    private float initialTouchX = 0f;
    private float initialTouchY = 0f;
    private long lastTapTime = 0L;
    private long touchStartTime = 0L;
    private boolean hasMoved = false;

    private void log(String s) {
        try {
            File f = new File("/sdcard/Download/pet_error.txt");
            FileWriter fw = new FileWriter(f, true);
            fw.append("[SVC] ").append(s).append("\n");
            fw.close();
        } catch (Throwable t) {}
        Log.d("PetDebug", s);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("=== OverlayService onCreate ===");
        try {
            createNotificationChannel();
            log("channel created");
        } catch (Throwable t) {
            log("createNotificationChannel FAILED: " + t);
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification("澈在屏幕上啦"));
            log("startForeground ok");
        } catch (Throwable t) {
            log("startForeground FAILED: " + t);
        }
        try {
            setupOverlay();
            log("setupOverlay ok");
        } catch (Throwable t) {
            log("setupOverlay FAILED: " + t);
        }
    }

    private void setupOverlay() {
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams(
                dpToPx(PET_SIZE_DP),
                dpToPx(PET_HEIGHT_DP),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 300;
        log("params built, type=APPLICATION_OVERLAY");

        overlayView = new WebView(this);
        overlayView.setBackgroundColor(0x00000000);
        WebSettings settings = overlayView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        overlayView.setWebViewClient(new WebViewClient());
        overlayView.loadUrl("file:///android_asset/pet.html");
        log("webview loaded pet.html");
        overlayView.setOnTouchListener(createTouchListener());

        windowManager.addView(overlayView, params);
        log("addView ok");
    }

    private View.OnTouchListener createTouchListener() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        hasMoved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            hasMoved = true;
                        }
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        long elapsed = System.currentTimeMillis() - touchStartTime;
                        if (!hasMoved) {
                            if (System.currentTimeMillis() - lastTapTime < 300) {
                                onDoubleTap();
                            } else if (elapsed > 600) {
                                onLongPress();
                            } else {
                                onTap();
                            }
                        }
                        lastTapTime = System.currentTimeMillis();
                        return true;
                }
                return false;
            }
        };
    }

    private void onTap() {
        overlayView.evaluateJavascript("window.petEngine && window.petEngine.onTap()", null);
    }

    private void onDoubleTap() {
        overlayView.evaluateJavascript("window.petEngine && window.petEngine.onDoubleTap()", null);
    }

    private void onLongPress() {
        overlayView.evaluateJavascript("window.petEngine && window.petEngine.onLongPress()", null);
    }

    private Notification buildNotification(String text) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0,
                getPackageManager().getLaunchIntentForPackage(getPackageName()),
                PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("\uD83D\uDC3E")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Pet", NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        try {
            if (overlayView != null) {
                windowManager.removeView(overlayView);
                overlayView.destroy();
            }
        } catch (Throwable t) {}
        overlayView = null;
        super.onDestroy();
    }
}