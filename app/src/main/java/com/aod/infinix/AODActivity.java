package com.aod.infinix;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.Calendar;
import android.app.KeyguardManager;
import android.os.Build;

public class AODActivity extends Activity {

    private AnalogClockView analogClockView;
    private FrameLayout digitalClockContainer;
    private TextView tvTime, tvDate, tvBattery, tvQuote;
    private Handler handler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver batteryReceiver;
    private BroadcastReceiver screenReceiver;
    private GestureDetector gestureDetector;
    private SharedPreferences prefs;
    private boolean showingAnalog = false;

    private Runnable clockUpdater = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            if (analogClockView != null) analogClockView.invalidate();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE);

        // ── SCREEN FIX: Keep screen on permanently at low brightness ──
        Window window = getWindow();
window.addFlags(
    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON     |
    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON     |
    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
);

// Android 8.1+ ka naya tarika
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
    setShowWhenLocked(true);
    setTurnScreenOn(true);
    KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
    if (km != null) km.requestDismissKeyguard(this, null);
} else {
    window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
    );
}
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = 0.04f; // 4% brightness — saves battery
        window.setAttributes(lp);

        // Fullscreen
        window.getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN         |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION    |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY   |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        setContentView(R.layout.activity_aod);

        analogClockView       = findViewById(R.id.analog_clock);
        digitalClockContainer = findViewById(R.id.digital_clock_container);
        tvTime                = findViewById(R.id.tv_time);
        tvDate                = findViewById(R.id.tv_date);
        tvBattery             = findViewById(R.id.tv_battery);
        tvQuote               = findViewById(R.id.tv_quote);

        showingAnalog = prefs.getBoolean("show_analog", false);
        applyTheme();
        applyQuote();
        updateClockVisibility();
        updateDateTime();

        // Swipe left/right → switch clock | Double tap → dismiss
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null) return false;
                float dX = e2.getX() - e1.getX();
                if (Math.abs(dX) > 120) {
                    showingAnalog = !showingAnalog;
                    prefs.edit().putBoolean("show_analog", showingAnalog).apply();
                    updateClockVisibility();
                    return true;
                }
                return false;
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                finish();
                return true;
            }
        });

        findViewById(R.id.root_layout).setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        setupBatteryReceiver();
        setupScreenReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(clockUpdater);
        updateBattery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(clockUpdater);
        try { unregisterReceiver(batteryReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(screenReceiver);  } catch (Exception ignored) {}
    }

    private void updateClockVisibility() {
        analogClockView.setVisibility(showingAnalog ? View.VISIBLE : View.GONE);
        digitalClockContainer.setVisibility(showingAnalog ? View.GONE : View.VISIBLE);
    }

    private void updateDateTime() {
        Calendar cal = Calendar.getInstance();
        String[] days   = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        String[] months = {"January","February","March","April","May","June",
                           "July","August","September","October","November","December"};
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        tvTime.setText(String.format("%02d:%02d", h, m));
        tvDate.setText(days[cal.get(Calendar.DAY_OF_WEEK)-1] + ", " +
                       cal.get(Calendar.DAY_OF_MONTH) + " " +
                       months[cal.get(Calendar.MONTH)] + " " +
                       cal.get(Calendar.YEAR));
    }

    public void applyTheme() {
        int idx = prefs.getInt("theme_index", 0);
        int[][] themes = {
            {0xFFFFFFFF, 0xFF999999}, // White Classic
            {0xFF00CFFF, 0xFF0077AA}, // Blue Neon
            {0xFFFF9800, 0xFFCC6600}, // Orange Amber
            {0xFF00FF88, 0xFF007744}, // Matrix Green
            {0xFFFF4081, 0xFFAA0044}, // Hot Pink
            {0xFFCCBBFF, 0xFF8866CC}, // Soft Purple
        };
        int primary   = themes[idx][0];
        int secondary = themes[idx][1];

        if (tvTime    != null) tvTime.setTextColor(primary);
        if (tvDate    != null) tvDate.setTextColor(secondary);
        if (tvBattery != null) tvBattery.setTextColor(secondary);
        if (tvQuote   != null) tvQuote.setTextColor(secondary);
        if (analogClockView != null) analogClockView.setThemeColor(primary, secondary);
    }

    public void applyQuote() {
        String q = prefs.getString("custom_quote", "Stay focused. Stay calm.");
        if (tvQuote != null) tvQuote.setText(q);
    }

    private void setupBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level  = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale  = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                                || status == BatteryManager.BATTERY_STATUS_FULL;
                int pct = (int)((level / (float) scale) * 100);
                tvBattery.setText((charging ? "⚡ " : "🔋 ") + pct + "%");
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void updateBattery() {
        Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (bat == null) return;
        int level  = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale  = bat.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL;
        int pct = (int)((level / (float) scale) * 100);
        tvBattery.setText((charging ? "⚡ " : "🔋 ") + pct + "%");
    }

    private void setupScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) finish();
            }
        };
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }
}
