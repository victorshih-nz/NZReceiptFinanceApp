package com.example.nzreceiptapp.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cross-platform crop proposal contract.
 * Corners are normalized 0..1 and ordered TL, TR, BR, BL.
 */
public final class ReceiptCropProposal {
    public static final class Point {
        private final double x;
        private final double y;

        public Point(double x, double y) {
            this.x = clamp(x);
            this.y = clamp(y);
        }

        public double getX() { return x; }
        public double getY() { return y; }

        private static double clamp(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }

    private final String originalImageUri;
    private final List<Point> corners;
    private final boolean autoDetected;
    private final String detectionMode;
    private final String message;

    public ReceiptCropProposal(String originalImageUri,
                               List<Point> corners,
                               boolean autoDetected,
                               String detectionMode,
                               String message) {
        this.originalImageUri = originalImageUri;
        this.corners = Collections.unmodifiableList(new ArrayList<>(corners));
        this.autoDetected = autoDetected;
        this.detectionMode = detectionMode == null ? "" : detectionMode;
        this.message = message == null ? "" : message;
    }

    public static ReceiptCropProposal fullImage(String uri, String message) {
        List<Point> c = new ArrayList<>();
        c.add(new Point(0, 0));
        c.add(new Point(1, 0));
        c.add(new Point(1, 1));
        c.add(new Point(0, 1));
        return new ReceiptCropProposal(uri, c, false, "FULL_IMAGE", message);
    }

    public String getOriginalImageUri() { return originalImageUri; }
    public List<Point> getCorners() { return corners; }
    public boolean isAutoDetected() { return autoDetected; }
    public String getDetectionMode() { return detectionMode; }
    public String getMessage() { return message; }
}
