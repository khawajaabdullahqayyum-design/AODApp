package com.aod.infinix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
                                 .getBoolean("aod_enabled", false);
        if (enabled) {
            Intent s = new Intent(context, AODService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(s);
            else
                context.startService(s);
        }
    }
}
