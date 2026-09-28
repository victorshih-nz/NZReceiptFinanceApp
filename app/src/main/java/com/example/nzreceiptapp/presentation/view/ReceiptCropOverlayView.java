package com.example.nzreceiptapp.presentation.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.nzreceiptapp.domain.service.ReceiptCropProposal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MJ2.1 crop editor.
 *
 * Improvements over MJ2:
 * - four corner handles + four edge handles;
 * - edge handles move a whole side in parallel;
 * - larger touch targets than visible handles;
 * - dynamic Android gesture-exclusion bands while editing;
 * - normalized crop points remain free to move over the full image.
 */
public final class ReceiptCropOverlayView extends View {

    private static final int HANDLE_NONE = -1;
    private static final int HANDLE_TL = 0;
    private static final int HANDLE_TR = 1;
    private static final int HANDLE_BR = 2;
    private static final int HANDLE_BL = 3;
    private static final int HANDLE_TOP = 4;
    private static final int HANDLE_RIGHT = 5;
    private static final int HANDLE_BOTTOM = 6;
    private static final int HANDLE_LEFT = 7;

    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF imageRect = new RectF();
    private final List<ReceiptCropProposal.Point> corners = new ArrayList<>();

    private int imageWidth = 1;
    private int imageHeight = 1;

    private int activeHandle = HANDLE_NONE;
    private float lastTouchX;
    private float lastTouchY;

    public ReceiptCropOverlayView(Context context) {
        this(context, null);
    }

    public ReceiptCropOverlayView(
            Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);

        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(dp(2.5f));
        edgePaint.setARGB(255, 255, 255, 255);

        cornerPaint.setStyle(Paint.Style.FILL);
        cornerPaint.setARGB(255, 255, 255, 255);

        edgeHandlePaint.setStyle(Paint.Style.FILL);
        edgeHandlePaint.setARGB(220, 255, 255, 255);

        shadePaint.setStyle(Paint.Style.FILL);
        shadePaint.setARGB(105, 0, 0, 0);

        setClickable(true);
    }

    public void setImageSize(int width, int height) {
        imageWidth = Math.max(1, width);
        imageHeight = Math.max(1, height);
        invalidate();
    }

    public void setCorners(List<ReceiptCropProposal.Point> points) {
        corners.clear();

        if (points != null && points.size() == 4) {
            corners.addAll(points);
        } else {
            corners.add(new ReceiptCropProposal.Point(0, 0));
            corners.add(new ReceiptCropProposal.Point(1, 0));
            corners.add(new ReceiptCropProposal.Point(1, 1));
            corners.add(new ReceiptCropProposal.Point(0, 1));
        }

        invalidate();
        post(this::refreshGestureExclusion);
    }

    public List<ReceiptCropProposal.Point> getCorners() {
        return new ArrayList<>(corners);
    }

    @Override
    protected void onSizeChanged(
            int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        post(this::refreshGestureExclusion);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (corners.size() != 4) return;

        updateImageRect();

        float[][] p = screenPoints();

        Path polygon = new Path();
        polygon.moveTo(p[0][0], p[0][1]);
        polygon.lineTo(p[1][0], p[1][1]);
        polygon.lineTo(p[2][0], p[2][1]);
        polygon.lineTo(p[3][0], p[3][1]);
        polygon.close();

        int save = canvas.save();
        canvas.clipOutPath(polygon);
        canvas.drawRect(imageRect, shadePaint);
        canvas.restoreToCount(save);

        canvas.drawPath(polygon, edgePaint);

        float cornerRadius = dp(12f);
        for (int i = 0; i < 4; i++) {
            canvas.drawCircle(
                    p[i][0],
                    p[i][1],
                    cornerRadius,
                    cornerPaint);
        }

        float edgeRadius = dp(8f);

        float[] top = midpoint(p[0], p[1]);
        float[] right = midpoint(p[1], p[2]);
        float[] bottom = midpoint(p[2], p[3]);
        float[] left = midpoint(p[3], p[0]);

        canvas.drawCircle(top[0], top[1], edgeRadius, edgeHandlePaint);
        canvas.drawCircle(right[0], right[1], edgeRadius, edgeHandlePaint);
        canvas.drawCircle(bottom[0], bottom[1], edgeRadius, edgeHandlePaint);
        canvas.drawCircle(left[0], left[1], edgeRadius, edgeHandlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (corners.size() != 4) return false;

        updateImageRect();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeHandle =
                        findNearestHandle(
                                event.getX(),
                                event.getY());

                if (activeHandle == HANDLE_NONE) {
                    return false;
                }

                lastTouchX = event.getX();
                lastTouchY = event.getY();

                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (activeHandle == HANDLE_NONE) {
                    return false;
                }

                moveActiveHandle(
                        event.getX(),
                        event.getY());

                lastTouchX = event.getX();
                lastTouchY = event.getY();

                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeHandle = HANDLE_NONE;
                getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private int findNearestHandle(float x, float y) {
        float[][] p = screenPoints();

        float[][] handles = new float[][] {
                p[0],
                p[1],
                p[2],
                p[3],
                midpoint(p[0], p[1]),
                midpoint(p[1], p[2]),
                midpoint(p[2], p[3]),
                midpoint(p[3], p[0])
        };

        float cornerTouchRadius = dp(44f);
        float edgeTouchRadius = dp(34f);

        int bestHandle = HANDLE_NONE;
        float bestDistance = Float.MAX_VALUE;

        for (int i = 0; i < handles.length; i++) {
            float distance = (float) Math.hypot(
                    x - handles[i][0],
                    y - handles[i][1]);

            float limit = i < 4
                    ? cornerTouchRadius
                    : edgeTouchRadius;

            if (distance <= limit
                    && distance < bestDistance) {
                bestDistance = distance;
                bestHandle = i;
            }
        }

        return bestHandle;
    }

    private void moveActiveHandle(float x, float y) {
        if (activeHandle >= HANDLE_TL
                && activeHandle <= HANDLE_BL) {

            moveCorner(activeHandle, x, y);
            return;
        }

        double dx =
                (x - lastTouchX)
                        / Math.max(1f, imageRect.width());

        double dy =
                (y - lastTouchY)
                        / Math.max(1f, imageRect.height());

        switch (activeHandle) {
            case HANDLE_TOP:
                translateY(0, 1, dy);
                break;

            case HANDLE_RIGHT:
                translateX(1, 2, dx);
                break;

            case HANDLE_BOTTOM:
                translateY(2, 3, dy);
                break;

            case HANDLE_LEFT:
                translateX(3, 0, dx);
                break;

            default:
                break;
        }
    }

    private void moveCorner(
            int cornerIndex,
            float screenX,
            float screenY) {

        float x = clamp(
                screenX,
                imageRect.left,
                imageRect.right);

        float y = clamp(
                screenY,
                imageRect.top,
                imageRect.bottom);

        double nx =
                (x - imageRect.left)
                        / Math.max(1f, imageRect.width());

        double ny =
                (y - imageRect.top)
                        / Math.max(1f, imageRect.height());

        corners.set(
                cornerIndex,
                new ReceiptCropProposal.Point(nx, ny));
    }

    private void translateX(
            int first,
            int second,
            double delta) {

        double currentA = corners.get(first).getX();
        double currentB = corners.get(second).getX();

        double minDelta =
                Math.max(-currentA, -currentB);

        double maxDelta =
                Math.min(
                        1.0 - currentA,
                        1.0 - currentB);

        double applied =
                Math.max(
                        minDelta,
                        Math.min(maxDelta, delta));

        corners.set(
                first,
                new ReceiptCropProposal.Point(
                        currentA + applied,
                        corners.get(first).getY()));

        corners.set(
                second,
                new ReceiptCropProposal.Point(
                        currentB + applied,
                        corners.get(second).getY()));
    }

    private void translateY(
            int first,
            int second,
            double delta) {

        double currentA = corners.get(first).getY();
        double currentB = corners.get(second).getY();

        double minDelta =
                Math.max(-currentA, -currentB);

        double maxDelta =
                Math.min(
                        1.0 - currentA,
                        1.0 - currentB);

        double applied =
                Math.max(
                        minDelta,
                        Math.min(maxDelta, delta));

        corners.set(
                first,
                new ReceiptCropProposal.Point(
                        corners.get(first).getX(),
                        currentA + applied));

        corners.set(
                second,
                new ReceiptCropProposal.Point(
                        corners.get(second).getX(),
                        currentB + applied));
    }

    private float[][] screenPoints() {
        float[][] result = new float[4][2];

        for (int i = 0; i < 4; i++) {
            result[i][0] =
                    imageRect.left
                            + (float) corners.get(i).getX()
                            * imageRect.width();

            result[i][1] =
                    imageRect.top
                            + (float) corners.get(i).getY()
                            * imageRect.height();
        }

        return result;
    }

    private float[] midpoint(
            float[] a,
            float[] b) {
        return new float[] {
                (a[0] + b[0]) / 2f,
                (a[1] + b[1]) / 2f
        };
    }

    private void updateImageRect() {
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        if (viewWidth <= 0 || viewHeight <= 0) {
            imageRect.set(0, 0, 0, 0);
            return;
        }

        float imageAspect =
                imageWidth / (float) imageHeight;

        float viewAspect =
                viewWidth / viewHeight;

        if (imageAspect > viewAspect) {
            float displayedHeight =
                    viewWidth / imageAspect;

            float top =
                    (viewHeight - displayedHeight) / 2f;

            imageRect.set(
                    0,
                    top,
                    viewWidth,
                    top + displayedHeight);

        } else {
            float displayedWidth =
                    viewHeight * imageAspect;

            float left =
                    (viewWidth - displayedWidth) / 2f;

            imageRect.set(
                    left,
                    0,
                    left + displayedWidth,
                    viewHeight);
        }
    }

    /**
     * Keep Android's left/right system back gesture away from the crop editing
     * region. The exclusion width is derived from the current touch target size,
     * not from crop geometry.
     */
    private void refreshGestureExclusion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        int band =
                Math.max(
                        1,
                        Math.round(dp(48f)));

        int height = getHeight();
        int width = getWidth();

        if (width <= 0 || height <= 0) {
            setSystemGestureExclusionRects(
                    Collections.emptyList());
            return;
        }

        List<Rect> rects = new ArrayList<>();
        rects.add(
                new Rect(
                        0,
                        0,
                        Math.min(width, band),
                        height));

        rects.add(
                new Rect(
                        Math.max(0, width - band),
                        0,
                        width,
                        height));

        setSystemGestureExclusionRects(rects);
    }

    private float clamp(
            float value,
            float min,
            float max) {
        return Math.max(
                min,
                Math.min(max, value));
    }

    private float dp(float value) {
        return value
                * getResources()
                .getDisplayMetrics()
                .density;
    }
}
