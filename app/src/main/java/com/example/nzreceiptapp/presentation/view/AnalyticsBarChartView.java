package com.example.nzreceiptapp.presentation.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.nzreceiptapp.presentation.viewmodel.AnalyticsUiState;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lightweight cents-based bar chart. Rendering and gestures only; selection lives in the ViewModel. */
public final class AnalyticsBarChartView extends View {
    public interface Listener {
        void onSelect(int key);
        void onDrillDown(int key);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GestureDetector gestures;
    private final List<Integer> keys = new ArrayList<>();
    private final List<Long> values = new ArrayList<>();
    private final RectF rectangle = new RectF();
    private Listener listener;
    private AnalyticsUiState.Level level = AnalyticsUiState.Level.MONTH;
    private Integer selectedKey;
    private int highlightedColor;
    private int barColor;
    private int textColor;
    private int dividerColor;
    private float cellWidth;
    private final float density;

    public AnalyticsBarChartView(Context context) { this(context, null); }

    public AnalyticsBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        cellWidth = 44 * density;
        highlightedColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary);
        barColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary);
        textColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        dividerColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline);
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                Integer key = keyAt(e.getX(), e.getY());
                if (key != null && listener != null) listener.onSelect(key);
                return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e) {
                Integer key = keyAt(e.getX(), e.getY());
                if (key != null && listener != null) listener.onDrillDown(key);
                return true;
            }
        });
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setListener(Listener listener) { this.listener = listener; }

    public void setData(Map<Integer, Long> bars, AnalyticsUiState.Level level, Integer selected) {
        this.level = level;
        this.selectedKey = selected;
        keys.clear();
        values.clear();
        for (Map.Entry<Integer, Long> entry : bars.entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }
        cellWidth = (level == AnalyticsUiState.Level.YEAR ? 62 : 44) * density;
        int minimum = (int) (keys.size() * cellWidth + 24 * density);
        getLayoutParams().width = Math.max(minimum, getResources().getDisplayMetrics().widthPixels - (int) (40 * density));
        requestLayout();
        invalidate();
        setContentDescription(buildDescription());
    }

    /** Chart-local centre coordinate for a bar, or -1 when not present. */
    public int getCentreXForKey(int key) {
        int index = keys.indexOf(key);
        return index < 0 ? -1 : Math.round(12 * density + (index + 0.5f) * cellWidth);
    }

    private String buildDescription() {
        StringBuilder description = new StringBuilder("Spending chart. ");
        for (int i = 0; i < keys.size(); i++) {
            description.append(keys.get(i)).append(": ")
                    .append(String.format(Locale.US, "$%.2f", values.get(i) / 100.0))
                    .append(". ");
        }
        return description.toString();
    }

    private Integer keyAt(float x, float y) {
        if (keys.isEmpty() || y < 0 || y > getHeight()) return null;
        int index = (int) ((x - 12 * density) / cellWidth);
        if (x < 12 * density || index < 0 || index >= keys.size()) return null;
        return keys.get(index);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        return gestures.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float top = 30 * density;
        final float bottom = getHeight() - 40 * density;
        final float availableHeight = Math.max(1, bottom - top);
        long maximum = 0;
        for (long amount : values) maximum = Math.max(maximum, amount);
        maximum = Math.max(1, maximum);

        paint.setColor(dividerColor);
        paint.setStrokeWidth(density);
        canvas.drawLine(12 * density, bottom, getWidth() - 8 * density, bottom, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11 * density);

        for (int i = 0; i < keys.size(); i++) {
            float centre = 12 * density + (i + 0.5f) * cellWidth;
            float width = Math.min(32 * density, cellWidth * 0.68f);
            float height = values.get(i) <= 0 ? 0 : availableHeight * ((float) values.get(i) / maximum);
            paint.setColor(keys.get(i).equals(selectedKey) ? highlightedColor : barColor);
            rectangle.set(centre - width / 2, bottom - height, centre + width / 2, bottom);
            canvas.drawRoundRect(rectangle, 4 * density, 4 * density, paint);
            paint.setColor(textColor);
            String label = level == AnalyticsUiState.Level.YEAR
                    ? String.valueOf(keys.get(i)) : String.valueOf(keys.get(i));
            canvas.drawText(label, centre, bottom + 17 * density, paint);
            if (keys.get(i).equals(selectedKey)) {
                paint.setTextSize(10 * density);
                canvas.drawText(String.format(Locale.US, "$%.2f", values.get(i) / 100.0),
                        centre, Math.max(12 * density, bottom - height - 6 * density), paint);
                paint.setTextSize(11 * density);
            }
        }
    }
}
