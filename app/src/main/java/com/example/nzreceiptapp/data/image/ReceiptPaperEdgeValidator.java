package com.example.nzreceiptapp.data.image;

import java.util.Locale;

/**
 * Validates that a candidate side edge behaves like a real paper/background
 * boundary rather than an internal receipt line.
 *
 * The validator samples luminance on both sides of each candidate edge. For a
 * genuine receipt boundary, the inward strip should consistently look more
 * paper-like than the outward strip. Internal text/crease lines usually do not
 * show that sustained two-sided contrast.
 */
public final class ReceiptPaperEdgeValidator {

    private static final int SAMPLE_ROWS = 21;
    private static final double TOP_BOTTOM_GUARD_RATIO = 0.06;

    private ReceiptPaperEdgeValidator() { }

    public interface LuminanceSampler {
        double sample(double x, double y);
    }

    public static final class Result {
        public final boolean accepted;
        public final double leftEvidence;
        public final double rightEvidence;
        public final double leftConsistency;
        public final double rightConsistency;
        public final double adaptiveContrastThreshold;
        public final double suggestedMarginRatio;
        public final String reason;

        Result(boolean accepted,
               double leftEvidence,
               double rightEvidence,
               double leftConsistency,
               double rightConsistency,
               double adaptiveContrastThreshold,
               double suggestedMarginRatio,
               String reason) {
            this.accepted = accepted;
            this.leftEvidence = leftEvidence;
            this.rightEvidence = rightEvidence;
            this.leftConsistency = leftConsistency;
            this.rightConsistency = rightConsistency;
            this.adaptiveContrastThreshold = adaptiveContrastThreshold;
            this.suggestedMarginRatio = suggestedMarginRatio;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.US,
                    "accepted=%s leftEvidence=%.2f rightEvidence=%.2f "
                            + "leftConsistency=%.2f rightConsistency=%.2f "
                            + "adaptiveThreshold=%.2f marginRatio=%.3f reason=%s",
                    accepted,
                    leftEvidence,
                    rightEvidence,
                    leftConsistency,
                    rightConsistency,
                    adaptiveContrastThreshold,
                    suggestedMarginRatio,
                    reason
            );
        }
    }

    /**
     * @param pair candidate paper side edges in analysis-image coordinates
     * @param imageWidth analysis image width
     * @param imageHeight analysis image height
     * @param globalStdDev image luminance standard deviation
     * @param sampler grayscale luminance sampler (0..255)
     */
    public static Result validate(
            ReceiptSideEdgeSelector.Pair pair,
            double imageWidth,
            double imageHeight,
            double globalStdDev,
            LuminanceSampler sampler) {

        if (pair == null || sampler == null || imageWidth <= 0 || imageHeight <= 0) {
            return rejected(0, 0, 0, 0, 0, "missing candidate/image");
        }

        double candidateWidth = pair.widthRatio * imageWidth;

        // Sampling distance is derived from the candidate itself, not from a
        // fixed pixel value. This keeps behavior scale-independent.
        double nearOffset = clamp(candidateWidth * 0.018, 4.0, imageWidth * 0.03);
        double farOffset = clamp(candidateWidth * 0.050, nearOffset + 2.0, imageWidth * 0.08);

        // Adaptive threshold follows the photo's own luminance variability.
        // A small floor prevents near-zero thresholds on very flat images.
        double adaptiveThreshold = Math.max(6.0, globalStdDev * 0.18);

        Evidence left = collectEvidence(
                pair.left,
                true,
                imageWidth,
                imageHeight,
                nearOffset,
                farOffset,
                adaptiveThreshold,
                sampler
        );

        Evidence right = collectEvidence(
                pair.right,
                false,
                imageWidth,
                imageHeight,
                nearOffset,
                farOffset,
                adaptiveThreshold,
                sampler
        );

        // Stronger evidence permits a smaller safety margin. Lower evidence
        // expands the crop outward. The final range is intentionally bounded.
        double weakestEvidence = Math.min(left.meanDelta, right.meanDelta);
        double normalized = clamp(
                weakestEvidence / Math.max(adaptiveThreshold * 3.0, 1.0),
                0.0,
                1.0
        );
        double marginRatio = 0.045 - 0.025 * normalized;

        boolean enoughContrast =
                left.meanDelta >= adaptiveThreshold
                        && right.meanDelta >= adaptiveThreshold;

        boolean enoughConsistency =
                left.consistency >= 0.62
                        && right.consistency >= 0.62;

        boolean accepted = enoughContrast && enoughConsistency;

        String reason;
        if (!enoughContrast) {
            reason = "insufficient paper/background contrast";
        } else if (!enoughConsistency) {
            reason = "edge contrast not vertically consistent";
        } else {
            reason = "paper-edge evidence accepted";
        }

        return new Result(
                accepted,
                left.meanDelta,
                right.meanDelta,
                left.consistency,
                right.consistency,
                adaptiveThreshold,
                marginRatio,
                reason
        );
    }

    private static Evidence collectEvidence(
            ReceiptSideEdgeSelector.Edge edge,
            boolean leftEdge,
            double imageWidth,
            double imageHeight,
            double nearOffset,
            double farOffset,
            double threshold,
            LuminanceSampler sampler) {

        int valid = 0;
        int supportive = 0;
        double deltaTotal = 0.0;

        double yStart = imageHeight * TOP_BOTTOM_GUARD_RATIO;
        double yEnd = imageHeight * (1.0 - TOP_BOTTOM_GUARD_RATIO);

        for (int i = 0; i < SAMPLE_ROWS; i++) {
            double t = SAMPLE_ROWS == 1 ? 0.5 : i / (double) (SAMPLE_ROWS - 1);
            double y = yStart + (yEnd - yStart) * t;
            double boundaryX = edge.xAt(y);

            double inwardSign = leftEdge ? 1.0 : -1.0;
            double outwardSign = -inwardSign;

            double insideNearX = boundaryX + inwardSign * nearOffset;
            double insideFarX = boundaryX + inwardSign * farOffset;
            double outsideNearX = boundaryX + outwardSign * nearOffset;
            double outsideFarX = boundaryX + outwardSign * farOffset;

            if (!inside(imageWidth, insideNearX)
                    || !inside(imageWidth, insideFarX)
                    || !inside(imageWidth, outsideNearX)
                    || !inside(imageWidth, outsideFarX)) {
                continue;
            }

            double insideLum = (
                    sampler.sample(insideNearX, y)
                            + sampler.sample(insideFarX, y)
            ) * 0.5;

            double outsideLum = (
                    sampler.sample(outsideNearX, y)
                            + sampler.sample(outsideFarX, y)
            ) * 0.5;

            // Receipts are normally lighter than their environment. More
            // importantly, an internal printed/crease line will generally have
            // similar paper luminance on both sides and therefore low delta.
            double delta = insideLum - outsideLum;

            valid++;
            deltaTotal += Math.max(0.0, delta);
            if (delta >= threshold) supportive++;
        }

        if (valid == 0) return new Evidence(0.0, 0.0);

        return new Evidence(
                deltaTotal / valid,
                supportive / (double) valid
        );
    }

    private static boolean inside(double width, double x) {
        return x >= 0.0 && x <= width - 1.0;
    }

    private static Result rejected(
            double leftEvidence,
            double rightEvidence,
            double leftConsistency,
            double rightConsistency,
            double threshold,
            String reason) {
        return new Result(
                false,
                leftEvidence,
                rightEvidence,
                leftConsistency,
                rightConsistency,
                threshold,
                0.045,
                reason
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Evidence {
        final double meanDelta;
        final double consistency;

        Evidence(double meanDelta, double consistency) {
            this.meanDelta = meanDelta;
            this.consistency = consistency;
        }
    }
}
