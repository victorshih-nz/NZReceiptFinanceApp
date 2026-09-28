package com.example.nzreceiptapp.data.image;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java scoring/selection for partial-receipt side edges.
 *
 * A receipt photo may contain only the middle section of a long receipt, so
 * top/bottom corners are not required. We instead look for a plausible pair of
 * long, near-parallel side edges that encloses the main receipt region.
 */
public final class ReceiptSideEdgeSelector {

    private static final double MAX_VERTICAL_DEVIATION_DEGREES = 22.0;
    private static final double MAX_PAIR_ANGLE_DIFFERENCE_DEGREES = 10.0;
    private static final double MIN_EDGE_LENGTH_RATIO = 0.30;
    private static final double MIN_WIDTH_RATIO = 0.25;
    private static final double MAX_WIDTH_RATIO = 0.90;
    private static final double MIN_PAIR_SCORE = 0.48;

    private ReceiptSideEdgeSelector() { }

    public static final class Edge {
        public final double x1;
        public final double y1;
        public final double x2;
        public final double y2;

        public Edge(double x1, double y1, double x2, double y2) {
            if (y1 <= y2) {
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
            } else {
                this.x1 = x2;
                this.y1 = y2;
                this.x2 = x1;
                this.y2 = y1;
            }
        }

        public double length() {
            return Math.hypot(x2 - x1, y2 - y1);
        }

        public double angleFromVerticalDegrees() {
            double dx = x2 - x1;
            double dy = y2 - y1;
            if (Math.abs(dy) < 1e-9) return 90.0;
            return Math.toDegrees(Math.atan2(Math.abs(dx), Math.abs(dy)));
        }

        public double signedSlopeXPerY() {
            double dy = y2 - y1;
            if (Math.abs(dy) < 1e-9) return 0.0;
            return (x2 - x1) / dy;
        }

        public double xAt(double y) {
            double dy = y2 - y1;
            if (Math.abs(dy) < 1e-9) return (x1 + x2) * 0.5;
            return x1 + (y - y1) * (x2 - x1) / dy;
        }

        public double midX(double imageHeight) {
            return xAt((imageHeight - 1.0) * 0.5);
        }
    }

    public static final class Pair {
        public final Edge left;
        public final Edge right;
        public final double score;
        public final double widthRatio;
        public final double centerOffsetRatio;
        public final double angleDifferenceDegrees;

        Pair(Edge left,
             Edge right,
             double score,
             double widthRatio,
             double centerOffsetRatio,
             double angleDifferenceDegrees) {
            this.left = left;
            this.right = right;
            this.score = score;
            this.widthRatio = widthRatio;
            this.centerOffsetRatio = centerOffsetRatio;
            this.angleDifferenceDegrees = angleDifferenceDegrees;
        }
    }

    public static Pair select(
            List<Edge> rawEdges, double imageWidth, double imageHeight) {
        if (rawEdges == null || rawEdges.size() < 2
                || imageWidth <= 0 || imageHeight <= 0) {
            return null;
        }

        List<Edge> edges = new ArrayList<>();
        for (Edge edge : rawEdges) {
            if (edge == null) continue;
            if (edge.length() < imageHeight * MIN_EDGE_LENGTH_RATIO) continue;
            if (edge.angleFromVerticalDegrees() > MAX_VERTICAL_DEVIATION_DEGREES) continue;
            edges.add(edge);
        }

        Pair best = null;
        double imageCenter = (imageWidth - 1.0) * 0.5;

        for (int i = 0; i < edges.size(); i++) {
            for (int j = i + 1; j < edges.size(); j++) {
                Edge a = edges.get(i);
                Edge b = edges.get(j);

                double aMid = a.midX(imageHeight);
                double bMid = b.midX(imageHeight);
                Edge left = aMid <= bMid ? a : b;
                Edge right = aMid <= bMid ? b : a;

                double leftTop = left.xAt(0);
                double rightTop = right.xAt(0);
                double leftBottom = left.xAt(imageHeight - 1.0);
                double rightBottom = right.xAt(imageHeight - 1.0);

                double widthTop = rightTop - leftTop;
                double widthBottom = rightBottom - leftBottom;
                if (widthTop <= 0 || widthBottom <= 0) continue;

                double width = (widthTop + widthBottom) * 0.5;
                double widthRatio = width / imageWidth;
                if (widthRatio < MIN_WIDTH_RATIO || widthRatio > MAX_WIDTH_RATIO) continue;

                double angleDiff = Math.abs(
                        Math.toDegrees(Math.atan(left.signedSlopeXPerY()))
                                - Math.toDegrees(Math.atan(right.signedSlopeXPerY()))
                );
                if (angleDiff > MAX_PAIR_ANGLE_DIFFERENCE_DEGREES) continue;

                double center = (left.midX(imageHeight) + right.midX(imageHeight)) * 0.5;
                double centerOffsetRatio = Math.abs(center - imageCenter) / imageWidth;

                double leftLengthScore = clamp(left.length() / imageHeight, 0.0, 1.0);
                double rightLengthScore = clamp(right.length() / imageHeight, 0.0, 1.0);
                double lengthScore = (leftLengthScore + rightLengthScore) * 0.5;

                double parallelScore = 1.0
                        - clamp(angleDiff / MAX_PAIR_ANGLE_DIFFERENCE_DEGREES, 0.0, 1.0);

                // Prefer a broad central strip. This intentionally penalizes narrow
                // keyboard/object edge pairs near one side of the image.
                double widthScore = 1.0
                        - Math.abs(widthRatio - 0.58) / 0.58;
                widthScore = clamp(widthScore, 0.0, 1.0);

                double centerScore = 1.0
                        - clamp(centerOffsetRatio / 0.30, 0.0, 1.0);

                double score =
                        0.35 * lengthScore
                                + 0.25 * parallelScore
                                + 0.25 * centerScore
                                + 0.15 * widthScore;

                if (score < MIN_PAIR_SCORE) continue;

                if (best == null || score > best.score) {
                    best = new Pair(
                            left,
                            right,
                            score,
                            widthRatio,
                            centerOffsetRatio,
                            angleDiff
                    );
                }
            }
        }

        return best;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
