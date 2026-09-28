package com.example.nzreceiptapp.data.image;

import java.util.Locale;

public final class ReceiptCropGeometryPolicy {
    private ReceiptCropGeometryPolicy() {}

    public enum Mode { SAFE_ROI, PERSPECTIVE_WARP }

    public static final class Point {
        public final double x, y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    public static final class Decision {
        public final Mode mode;
        public final double widthAsymmetry;
        public final double heightAsymmetry;
        public final double oppositeEdgeAngleDifferenceDegrees;
        public final double severity;

        Decision(Mode mode, double widthAsymmetry, double heightAsymmetry,
                 double oppositeEdgeAngleDifferenceDegrees, double severity) {
            this.mode = mode;
            this.widthAsymmetry = widthAsymmetry;
            this.heightAsymmetry = heightAsymmetry;
            this.oppositeEdgeAngleDifferenceDegrees = oppositeEdgeAngleDifferenceDegrees;
            this.severity = severity;
        }

        @Override public String toString() {
            return String.format(Locale.US,
                    "mode=%s widthAsym=%.3f heightAsym=%.3f oppositeAngleDiff=%.2f severity=%.3f",
                    mode, widthAsymmetry, heightAsymmetry,
                    oppositeEdgeAngleDifferenceDegrees, severity);
        }
    }

    public static Decision decide(Point[] c) {
        if (c == null || c.length != 4) {
            return new Decision(Mode.SAFE_ROI, 0, 0, 0, 0);
        }

        double top = distance(c[0], c[1]);
        double right = distance(c[1], c[2]);
        double bottom = distance(c[3], c[2]);
        double left = distance(c[0], c[3]);

        double widthAsym = relativeDifference(top, bottom);
        double heightAsym = relativeDifference(left, right);

        double horizontalDiff = parallelAngleDifference(
                lineAngleDegrees(c[0], c[1]),
                lineAngleDegrees(c[3], c[2]));
        double verticalDiff = parallelAngleDifference(
                lineAngleDegrees(c[0], c[3]),
                lineAngleDegrees(c[1], c[2]));

        double oppositeAngleDiff = Math.max(horizontalDiff, verticalDiff);
        double angleSignal = clamp(oppositeAngleDiff / 30.0, 0.0, 1.0);
        double severity = Math.max(Math.max(widthAsym, heightAsym), angleSignal);

        Mode mode = severity >= 0.24
                ? Mode.PERSPECTIVE_WARP
                : Mode.SAFE_ROI;

        return new Decision(mode, widthAsym, heightAsym, oppositeAngleDiff, severity);
    }

    private static double relativeDifference(double a, double b) {
        return Math.abs(a - b) / Math.max(1e-9, Math.max(a, b));
    }

    private static double distance(Point a, Point b) {
        return Math.hypot(b.x - a.x, b.y - a.y);
    }

    private static double lineAngleDegrees(Point a, Point b) {
        return Math.toDegrees(Math.atan2(b.y - a.y, b.x - a.x));
    }

    private static double parallelAngleDifference(double a, double b) {
        double diff = Math.abs(a - b) % 180.0;
        return diff > 90.0 ? 180.0 - diff : diff;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
