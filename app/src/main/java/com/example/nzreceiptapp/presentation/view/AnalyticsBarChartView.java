package com.example.nzreceiptapp.presentation.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

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
    private float edgeInset;
    private int desiredContentWidth;
    private float downX, downY, lastX;
    private boolean horizontalDrag;
    private final int touchSlop;
    private final float density;

    public AnalyticsBarChartView(Context context) { this(context, null); }

    public AnalyticsBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        touchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
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

    /**
     * Monthly chart fits 12 bars. Day/year charts have an approximately 8/7-bar viewport.
     * Use the actual scroll viewport width, not the device screen width.
     */
    public void setData(Map<Integer, Long> bars, AnalyticsUiState.Level level,
                        Integer selected, int viewportWidth) {
        this.level = level;
        this.selectedKey = selected;
        keys.clear();
        values.clear();
        // Do not depend on repository Map iteration order for chronological bars.
        for (Integer key : new java.util.TreeSet<>(bars.keySet())) {
            keys.add(key);
            values.add(bars.get(key));
        }
        int viewport = Math.max(1, viewportWidth);
        int visibleBars = level == AnalyticsUiState.Level.MONTH ? 12
                : level == AnalyticsUiState.Level.DAY ? 8 : 7;
        // The wide edge gutter keeps a selected value visible at either end.
        edgeInset = (level == AnalyticsUiState.Level.MONTH ? 12f : 56f) * density;
        cellWidth = Math.max(1f, (viewport - 24f * density) / visibleBars);
        desiredContentWidth = Math.max(viewport,
                (int) Math.ceil(2 * edgeInset + keys.size() * cellWidth));
        // Width is enforced by onMeasure, independent of fillViewport/layout params.
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params != null && params.width != desiredContentWidth) {
            params.width = desiredContentWidth;
            setLayoutParams(params);
        }
        requestLayout();
        invalidate();
        setContentDescription(buildDescription());
    }

    /** Chart-local centre coordinate for a bar, or -1 when not present. */
    public int getCentreXForKey(int key) {
        int index = keys.indexOf(key);
        return index < 0 ? -1 : Math.round(edgeInset + (index + 0.5f) * cellWidth);
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
        int index = (int) ((x - edgeInset) / cellWidth);
        if (x < edgeInset || index < 0 || index >= keys.size()) return null;
        return keys.get(index);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // A HorizontalScrollView may give its child an AT_MOST/UNSPECIFIED width.
        // Force the full width for all 28–31 daily bars, not only the viewport.
        int requested = desiredContentWidth > 0 ? desiredContentWidth
                : MeasureSpec.getSize(widthMeasureSpec);
        int exactWidth = MeasureSpec.makeMeasureSpec(requested, MeasureSpec.EXACTLY);
        super.onMeasure(exactWidth, heightMeasureSpec);
    }

    private HorizontalScrollView scrollParent() {
        android.view.ViewParent p = getParent();
        while (p != null) {
            if (p instanceof HorizontalScrollView) return (HorizontalScrollView) p;
            p = p.getParent();
        }
        return null;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = event.getX();
                downY = event.getY();
                horizontalDrag = false;
                gestures.onTouchEvent(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (!horizontalDrag && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    horizontalDrag = true;
                    // Prevent the outer SwipeRefreshLayout from taking a horizontal swipe.
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (horizontalDrag) {
                    HorizontalScrollView parent = scrollParent();
                    if (parent != null) parent.scrollBy(Math.round(lastX - event.getX()), 0);
                    lastX = event.getX();
                    return true;
                }
                lastX = event.getX();
                return gestures.onTouchEvent(event) || true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (horizontalDrag) {
                    horizontalDrag = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                return gestures.onTouchEvent(event) || true;
            default:
                return gestures.onTouchEvent(event) || super.onTouchEvent(event);
        }
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
        canvas.drawLine(0, bottom, getWidth(), bottom, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11 * density);

        int firstPositiveIndex = -1;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) > 0) { firstPositiveIndex = i; break; }
        }
        for (int i = 0; i < keys.size(); i++) {
            float centre = edgeInset + (i + 0.5f) * cellWidth;
            float width = Math.min(32 * density, cellWidth * 0.68f);
            float height = values.get(i) <= 0 ? 0 : availableHeight * ((float) values.get(i) / maximum);
            paint.setColor(keys.get(i).equals(selectedKey) ? highlightedColor : barColor);
            rectangle.set(centre - width / 2, bottom - height, centre + width / 2, bottom);
            canvas.drawRoundRect(rectangle, 4 * density, 4 * density, paint);
            paint.setColor(textColor);
            String label = level == AnalyticsUiState.Level.YEAR
                    ? String.valueOf(keys.get(i)) : String.valueOf(keys.get(i));
            canvas.drawText(label, centre, bottom + 17 * density, paint);
            // On first drill-down nothing is selected. Show the first nonzero bar's
            // amount so the auto-positioned day has a readable value immediately.
            if (keys.get(i).equals(selectedKey)
                    || (selectedKey == null && i == firstPositiveIndex)) {
                paint.setTextSize(10 * density);
                String amount = String.format(Locale.US, "$%.2f", values.get(i) / 100.0);
                float halfWidth = paint.measureText(amount) / 2f;
                HorizontalScrollView scroll = scrollParent();
                float visibleLeft = scroll == null ? 0 : scroll.getScrollX();
                float visibleRight = scroll == null ? getWidth() : visibleLeft + scroll.getWidth();
                float labelX = Math.max(visibleLeft + halfWidth + 2 * density,
                        Math.min(visibleRight - halfWidth - 2 * density, centre));
                if (centre >= visibleLeft && centre <= visibleRight) {
                    canvas.drawText(amount, labelX,
                            Math.max(12 * density, bottom - height - 6 * density), paint);
                }
                paint.setTextSize(11 * density);
            }
        }
    }
}
