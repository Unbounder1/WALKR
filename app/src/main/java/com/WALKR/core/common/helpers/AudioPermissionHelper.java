package com.WALKR.core.common.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class AudioPermissionHelper {
    private static final int AUDIO_PERMISSION_CODE = 2;

    public static boolean hasAudioPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
    }

    public static void requestAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_CODE);
    }

    public static boolean hasAudioPermissionsResponseInResult(String[] permissions) {
        for (String permission : permissions) {
            if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO);
    }

    public static void launchPermissionSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }
}

