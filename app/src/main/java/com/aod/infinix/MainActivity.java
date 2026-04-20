package com.aod.infinix;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Switch aodSwitch;
    private TextView tvStatus, tvCurrentTheme, tvCurrentQuote;
    private SharedPreferences prefs;
    private static final int OVERLAY_REQ = 100;

    private final String[] THEME_NAMES = {
        "White Classic", "Blue Neon", "Orange Amber",
        "Matrix Green", "Hot Pink", "Soft Purple"
    };
    private final int[] THEME_COLORS = {
        0xFFFFFFFF, 0xFF00CFFF, 0xFFFF9800,
        0xFF00FF88, 0xFFFF4081, 0xFFCCBBFF
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE);

        aodSwitch       = findViewById(R.id.switch_aod);
        tvStatus        = findViewById(R.id.tv_status);
        tvCurrentTheme  = findViewById(R.id.tv_current_theme);
        tvCurrentQuote  = findViewById(R.id.tv_current_quote);

        // Restore saved state
        boolean enabled = prefs.getBoolean("aod_enabled", false);
        aodSwitch.setChecked(enabled);
        tvStatus.setText(enabled ? "AOD is ON ✓" : "AOD is OFF");
        updateThemeDisplay();
        updateQuoteDisplay();

        // Toggle AOD
        aodSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.canDrawOverlays(this)) {
                    Toast.makeText(this,
                        "Grant 'Display over other apps' permission first!",
                        Toast.LENGTH_LONG).show();
                    startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())), OVERLAY_REQ);
                    aodSwitch.setChecked(false);
                    return;
                }
                startAOD();
                tvStatus.setText("AOD is ON ✓");
                prefs.edit().putBoolean("aod_enabled", true).apply();
            } else {
                stopAOD();
                tvStatus.setText("AOD is OFF");
                prefs.edit().putBoolean("aod_enabled", false).apply();
            }
        });

        // Theme picker
        findViewById(R.id.btn_theme).setOnClickListener(v -> showThemePicker());

        // Quote editor
        findViewById(R.id.btn_quote).setOnClickListener(v -> showQuoteEditor());

        // Preview AOD button
        findViewById(R.id.btn_preview).setOnClickListener(v -> {
            Intent i = new Intent(this, AODActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
    }

    private void startAOD() {
        Intent i = new Intent(this, AODService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(i);
        else
            startService(i);
        Toast.makeText(this, "AOD started! Press power button to test.", Toast.LENGTH_SHORT).show();
    }

    private void stopAOD() {
        stopService(new Intent(this, AODService.class));
        Toast.makeText(this, "AOD stopped.", Toast.LENGTH_SHORT).show();
    }

    private void showThemePicker() {
        int currentIdx = prefs.getInt("theme_index", 0);

        // Build theme option views
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);

        for (int i = 0; i < THEME_NAMES.length; i++) {
            final int idx = i;
            TextView tv = new TextView(this);
            tv.setText((i == currentIdx ? "✓  " : "     ") + THEME_NAMES[i]);
            tv.setTextSize(16f);
            tv.setTextColor(THEME_COLORS[i]);
            tv.setPadding(16, 24, 16, 24);
            tv.setBackgroundColor(0xFF111111);
            tv.setOnClickListener(v -> {
                prefs.edit().putInt("theme_index", idx).apply();
                updateThemeDisplay();
                Toast.makeText(this, THEME_NAMES[idx] + " theme applied!", Toast.LENGTH_SHORT).show();
            });
            container.addView(tv);

            // Divider
            View div = new View(this);
            div.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
            div.setBackgroundColor(0xFF222222);
            container.addView(div);
        }

        new AlertDialog.Builder(this)
            .setTitle("Choose Theme")
            .setView(container)
            .setNegativeButton("Close", null)
            .show();
    }

    private void showQuoteEditor() {
        String current = prefs.getString("custom_quote", "Stay focused. Stay calm.");
        EditText et = new EditText(this);
        et.setText(current);
        et.setTextColor(0xFFFFFFFF);
        et.setBackgroundColor(0xFF111111);
        et.setPadding(30, 20, 30, 20);
        et.setHint("Enter your quote...");
        et.setHintTextColor(0xFF555555);
        et.setSingleLine(true);

        new AlertDialog.Builder(this)
            .setTitle("Custom Quote / Text")
            .setView(et)
            .setPositiveButton("Save", (d, w) -> {
                String q = et.getText().toString().trim();
                if (q.isEmpty()) q = "Stay focused. Stay calm.";
                prefs.edit().putString("custom_quote", q).apply();
                updateQuoteDisplay();
                Toast.makeText(this, "Quote saved!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateThemeDisplay() {
        int idx = prefs.getInt("theme_index", 0);
        tvCurrentTheme.setText("Theme: " + THEME_NAMES[idx]);
        tvCurrentTheme.setTextColor(THEME_COLORS[idx]);
    }

    private void updateQuoteDisplay() {
        String q = prefs.getString("custom_quote", "Stay focused. Stay calm.");
        tvCurrentQuote.setText("Quote: \"" + q + "\"");
    }
}
