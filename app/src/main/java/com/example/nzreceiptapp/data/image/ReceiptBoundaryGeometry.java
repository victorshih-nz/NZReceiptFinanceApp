package com.example.nzreceiptapp.data.image;

import java.util.Arrays;
import java.util.Comparator;

/** Pure-Java geometry helpers used by receipt boundary preprocessing. */
public final class ReceiptBoundaryGeometry {
    private ReceiptBoundaryGeometry() { }

    public static final class Point {
        public final double x;
        public final double y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    /** Orders four corners as top-left, top-right, bottom-right, bottom-left. */
    public static Point[] orderCorners(Point[] input) {
        if (input == null || input.length != 4) {
            throw new IllegalArgumentException("Exactly four corners are required");
        }
        Point[] points = Arrays.copyOf(input, 4);
        double cx = 0.0, cy = 0.0;
        for (Point p : points) { cx += p.x; cy += p.y; }
        cx /= 4.0; cy /= 4.0;
        final double centerX = cx, centerY = cy;
        Arrays.sort(points, Comparator.comparingDouble(
                p -> Math.atan2(p.y - centerY, p.x - centerX)));

        int topLeft = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double score = points[i].x + points[i].y;
            if (score < best) { best = score; topLeft = i; }
        }

        Point[] ordered = new Point[4];
        for (int i = 0; i < 4; i++) ordered[i] = points[(topLeft + i) % 4];
        if (ordered[1].x < ordered[3].x) {
            Point tmp = ordered[1]; ordered[1] = ordered[3]; ordered[3] = tmp;
        }
        return ordered;
    }

    public static double polygonArea(Point[] ordered) {
        if (ordered == null || ordered.length != 4) return 0.0;
        double sum = 0.0;
        for (int i = 0; i < 4; i++) {
            Point a = ordered[i], b = ordered[(i + 1) % 4];
            sum += a.x * b.y - b.x * a.y;
        }
        return Math.abs(sum) * 0.5;
    }

    public static Point[] expandFromCenter(Point[] ordered, double factor,
                                            double maxX, double maxY) {
        if (ordered == null || ordered.length != 4) {
            throw new IllegalArgumentException("Exactly four corners are required");
        }
        if (factor < 1.0) throw new IllegalArgumentException("factor must be >= 1");
        double cx = 0.0, cy = 0.0;
        for (Point p : ordered) { cx += p.x; cy += p.y; }
        cx /= 4.0; cy /= 4.0;
        Point[] out = new Point[4];
        for (int i = 0; i < 4; i++) {
            double x = cx + (ordered[i].x - cx) * factor;
            double y = cy + (ordered[i].y - cy) * factor;
            out[i] = new Point(clamp(x, 0, maxX), clamp(y, 0, maxY));
        }
        return out;
    }

    public static double distance(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
