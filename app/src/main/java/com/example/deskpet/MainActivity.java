package com.example.deskpet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;

public class MainActivity extends Activity {

    private void log(String s) {
        try {
            File f = new File("/sdcard/Download/pet_error.txt");
            FileWriter fw = new FileWriter(f, true);
            fw.append(s).append("\n");
            fw.close();
        } catch (Throwable t) {}
        Log.d("PetDebug", s);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("=== MainActivity onCreate ===");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean ok = Settings.canDrawOverlays(this);
            log("canDrawOverlays = " + ok);
            if (!ok) {
                log("no overlay permission, jump to settings");
                try {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                    );
                    startActivity(intent);
                } catch (Throwable t) {
                    log("jump settings failed: " + t);
                }
                finish();
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean notifOk = getApplicationContext()
                    .getSystemService(android.app.NotificationManager.class)
                    .areNotificationsEnabled();
            log("notifications enabled = " + notifOk);
        }

        log("starting OverlayService...");
        try {
            Intent svc = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
            log("service start ok");
        } catch (Throwable t) {
            log("service start FAILED: " + t);
        }
        finish();
    }
}