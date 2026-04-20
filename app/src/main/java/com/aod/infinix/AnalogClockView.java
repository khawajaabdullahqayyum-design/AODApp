package com.aod.infinix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.Calendar;

public class AnalogClockView extends View {

    private Paint paintCircle, paintHour, paintMinute, paintSecond,
                  paintTick, paintCenter, paintNumbers;
    private int primaryColor   = Color.WHITE;
    private int secondaryColor = Color.GRAY;

    public AnalogClockView(Context context) { super(context); init(); }
    public AnalogClockView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        paintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCircle.setStyle(Paint.Style.STROKE);
        paintCircle.setStrokeWidth(3f);

        paintHour = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHour.setStyle(Paint.Style.STROKE);
        paintHour.setStrokeCap(Paint.Cap.ROUND);
        paintHour.setStrokeWidth(10f);

        paintMinute = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMinute.setStyle(Paint.Style.STROKE);
        paintMinute.setStrokeCap(Paint.Cap.ROUND);
        paintMinute.setStrokeWidth(6f);

        paintSecond = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSecond.setStyle(Paint.Style.STROKE);
        paintSecond.setStrokeCap(Paint.Cap.ROUND);
        paintSecond.setStrokeWidth(2.5f);

        paintTick = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTick.setStyle(Paint.Style.STROKE);

        paintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenter.setStyle(Paint.Style.FILL);

        paintNumbers = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNumbers.setTextAlign(Paint.Align.CENTER);

        applyColors();
    }

    public void setThemeColor(int primary, int secondary) {
        primaryColor   = primary;
        secondaryColor = secondary;
        applyColors();
        invalidate();
    }

    private void applyColors() {
        paintCircle.setColor(secondaryColor);
        paintHour.setColor(primaryColor);
        paintMinute.setColor(primaryColor);
        paintSecond.setColor(0xFFFF4444); // red seconds
        paintTick.setColor(secondaryColor);
        paintCenter.setColor(primaryColor);
        paintNumbers.setColor(secondaryColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float radius = Math.min(cx, cy) * 0.88f;

        // Outer circle
        canvas.drawCircle(cx, cy, radius, paintCircle);

        // Hour tick marks + numbers
        paintNumbers.setTextSize(radius * 0.12f);
        for (int i = 1; i <= 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            float tickOuter = radius * 0.92f;
            float tickInner = (i % 3 == 0) ? radius * 0.76f : radius * 0.84f;
            paintTick.setStrokeWidth((i % 3 == 0) ? 4f : 2f);

            float x1 = cx + (float)(tickOuter * Math.cos(angle));
            float y1 = cy + (float)(tickOuter * Math.sin(angle));
            float x2 = cx + (float)(tickInner * Math.cos(angle));
            float y2 = cy + (float)(tickInner * Math.sin(angle));
            canvas.drawLine(x1, y1, x2, y2, paintTick);

            // Numbers at 12,3,6,9
            if (i % 3 == 0) {
                float nx = cx + (float)((radius * 0.65f) * Math.cos(angle));
                float ny = cy + (float)((radius * 0.65f) * Math.sin(angle))
                             + (paintNumbers.getTextSize() / 3);
                canvas.drawText(String.valueOf(i), nx, ny, paintNumbers);
            }
        }

        // Get current time
        Calendar cal = Calendar.getInstance();
        int hour   = cal.get(Calendar.HOUR);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        // Smooth angles
        float hourAngle   = (float)((hour * 30 + minute * 0.5) - 90);
        float minuteAngle = (float)((minute * 6 + second * 0.1) - 90);
        float secondAngle = (float)(second * 6 - 90);

        // Hour hand
        drawHand(canvas, cx, cy, radius * 0.50f, hourAngle, paintHour);
        // Minute hand
        drawHand(canvas, cx, cy, radius * 0.70f, minuteAngle, paintMinute);
        // Second hand (with tail)
        drawHandWithTail(canvas, cx, cy, radius * 0.80f, radius * 0.20f, secondAngle, paintSecond);

        // Center dot
        canvas.drawCircle(cx, cy, 10f, paintCenter);
        // Inner center dot (red)
        Paint redCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
        redCenter.setColor(0xFFFF4444);
        redCenter.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 5f, redCenter);
    }

    private void drawHand(Canvas canvas, float cx, float cy,
                          float length, float angleDeg, Paint paint) {
        double rad = Math.toRadians(angleDeg);
        float ex = cx + (float)(length * Math.cos(rad));
        float ey = cy + (float)(length * Math.sin(rad));
        canvas.drawLine(cx, cy, ex, ey, paint);
    }

    private void drawHandWithTail(Canvas canvas, float cx, float cy,
                                  float length, float tailLength,
                                  float angleDeg, Paint paint) {
        double rad    = Math.toRadians(angleDeg);
        double radOpp = Math.toRadians(angleDeg + 180);
        float ex   = cx + (float)(length     * Math.cos(rad));
        float ey   = cy + (float)(length     * Math.sin(rad));
        float tx   = cx + (float)(tailLength * Math.cos(radOpp));
        float ty   = cy + (float)(tailLength * Math.sin(radOpp));
        canvas.drawLine(tx, ty, ex, ey, paint);
    }
}
