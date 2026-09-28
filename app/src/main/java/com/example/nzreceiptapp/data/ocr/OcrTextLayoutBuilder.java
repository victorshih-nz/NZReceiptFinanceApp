package com.example.nzreceiptapp.data.ocr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rebuilds receipt rows from positioned OCR fragments.
 *
 * MJ2.1 is adaptive:
 * 1) reconstruct with the proven MJ1 raw-Y grouping;
 * 2) reconstruct with skew correction when there is enough skew evidence;
 * 3) score both row sets for receipt-structure sanity;
 * 4) use skew only when it is materially better.
 *
 * This prevents a global skew estimate from shifting a correct Woolworths price
 * column while still fixing rotated PAK'nSAVE receipts.
 */
public final class OcrTextLayoutBuilder {

    private static final double MIN_VERTICAL_OVERLAP_RATIO = 0.25;
    private static final double MAX_CENTRE_DISTANCE_RATIO = 0.60;

    private static final double MIN_WIDE_PAIR_SPAN_RATIO = 0.32;
    private static final double MAX_PAIR_VERTICAL_DISTANCE_HEIGHTS = 1.35;
    private static final double MAX_ABS_SLOPE = 0.15;
    private static final double SLOPE_CLUSTER_RADIUS = 0.012;

    // Skew must beat the raw reconstruction by this margin.
    private static final int MIN_SKEW_SCORE_GAIN = 3;

    private static final Pattern MONEY_TOKEN =
            Pattern.compile(
                    "(?<![A-Za-z0-9])(?:NZ)?\\$?-?\\d+\\.\\d{2}(?!\\d)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern STARTS_LIKE_QUANTITY =
            Pattern.compile(
                    "^\\s*\\d+(?:\\.\\d+)?\\s*@\\s*",
                    Pattern.CASE_INSENSITIVE);

    public static final class Fragment {
        private final String text;
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        public Fragment(
                String text,
                int left,
                int top,
                int right,
                int bottom) {
            this.text = text;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        String getText() { return text; }
        int getLeft() { return left; }
        int getRight() { return right; }
        int getTop() { return top; }
        int getBottom() { return bottom; }

        int getHeight() {
            return Math.max(1, bottom - top);
        }

        double getCentreX() {
            return left
                    + Math.max(1, right - left) / 2.0;
        }

        double getCentreY() {
            return top
                    + getHeight() / 2.0;
        }

        boolean isUsable() {
            return text != null
                    && !text.trim().isEmpty()
                    && right > left
                    && bottom > top;
        }
    }

    public String build(
            List<Fragment> sourceFragments) {

        if (sourceFragments == null
                || sourceFragments.isEmpty()) {
            return "";
        }

        List<Fragment> fragments =
                usableFragments(
                        sourceFragments);

        if (fragments.isEmpty()) {
            return "";
        }

        List<Row> rawRows =
                reconstruct(
                        fragments,
                        0.0,
                        true);

        int rawScore =
                scoreRows(rawRows);

        double skewSlope =
                estimateDominantSkewSlope(
                        fragments);

        if (Math.abs(skewSlope) < 0.006) {
            return buildText(rawRows);
        }

        List<Row> skewRows =
                reconstruct(
                        fragments,
                        skewSlope,
                        false);

        int skewScore =
                scoreRows(skewRows);

        if (skewScore
                >= rawScore
                + MIN_SKEW_SCORE_GAIN) {

            return buildText(skewRows);
        }

        // Conservative default: retain MJ1 behaviour unless skew is clearly
        // structurally better.
        return buildText(rawRows);
    }

    private List<Fragment> usableFragments(
            List<Fragment> sourceFragments) {

        List<Fragment> fragments =
                new ArrayList<>();

        for (Fragment fragment :
                sourceFragments) {

            if (fragment != null
                    && fragment.isUsable()) {
                fragments.add(fragment);
            }
        }

        return fragments;
    }

    private List<Row> reconstruct(
            List<Fragment> sourceFragments,
            double skewSlope,
            boolean allowOverlapMatch) {

        List<Fragment> fragments =
                new ArrayList<>(
                        sourceFragments);

        final double referenceX =
                minimumLeft(
                        fragments);

        fragments.sort(
                Comparator.<Fragment>comparingDouble(
                                f -> adjustedTop(
                                        f,
                                        skewSlope,
                                        referenceX))
                        .thenComparingInt(
                                Fragment::getLeft));

        List<Row> rows =
                new ArrayList<>();

        for (Fragment fragment :
                fragments) {

            Row bestRow = null;
            double bestDistance =
                    Double.MAX_VALUE;

            for (Row row : rows) {
                if (!row.isSameVisualRow(
                        fragment,
                        allowOverlapMatch)) {
                    continue;
                }

                double distance =
                        Math.abs(
                                row.getRepresentativeCentreY()
                                        - adjustedCentreY(
                                                fragment,
                                                skewSlope,
                                                referenceX));

                if (distance
                        < bestDistance) {
                    bestDistance =
                            distance;

                    bestRow =
                            row;
                }
            }

            if (bestRow == null) {
                rows.add(
                        new Row(
                                fragment,
                                skewSlope,
                                referenceX));
            } else {
                bestRow.add(
                        fragment);
            }
        }

        rows.sort(
                Comparator.comparingDouble(
                        Row::getRepresentativeCentreY));

        return rows;
    }

    /**
     * Receipt-structure sanity score used only to choose between two layout
     * interpretations of the same OCR fragments.
     */
    private int scoreRows(
            List<Row> rows) {

        int score = 0;

        for (Row row : rows) {
            String text =
                    row.buildText();

            if (text.isEmpty()) {
                continue;
            }

            int moneyCount =
                    countMoneyTokens(
                            text);

            boolean hasLetters =
                    containsLetter(
                            text);

            boolean hasAt =
                    text.contains("@");

            boolean startsLikeQuantity =
                    STARTS_LIKE_QUANTITY
                            .matcher(text)
                            .find();

            // A bare price row inside receipt content usually means the amount
            // lost ownership of its item row.
            if (!hasLetters
                    && moneyCount == 1) {
                score -= 6;
            }

            // Two prices on a normal product row is the exact PAK'nSAVE failure
            // seen with consecutive equal amounts.
            if (moneyCount >= 2
                    && !hasAt) {
                score -=
                        5 * (moneyCount - 1);
            }

            // Quantity rows are expected to start with "N @ ...". If a product
            // name and quantity syntax were merged, row ownership is suspect.
            if (hasAt
                    && !startsLikeQuantity
                    && hasLetters) {
                score -= 4;
            }

            // Normal product/payment rows with text + one amount are healthy.
            if (hasLetters
                    && moneyCount == 1) {
                score += 1;
            }
        }

        return score;
    }

    int scoreForTesting(
            List<String> rowTexts) {

        List<Row> synthetic =
                new ArrayList<>();

        int y = 0;

        for (String text : rowTexts) {
            Fragment f =
                    new Fragment(
                            text,
                            0,
                            y,
                            100,
                            y + 10);

            synthetic.add(
                    new Row(
                            f,
                            0.0,
                            0.0));

            y += 20;
        }

        return scoreRows(
                synthetic);
    }

    private int countMoneyTokens(
            String text) {

        Matcher matcher =
                MONEY_TOKEN.matcher(
                        text);

        int count = 0;

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private boolean containsLetter(
            String text) {

        for (int i = 0;
             i < text.length();
             i++) {

            if (Character.isLetter(
                    text.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    double estimateDominantSkewSlope(
            List<Fragment> fragments) {

        if (fragments == null
                || fragments.size() < 4) {
            return 0.0;
        }

        int minLeft =
                Integer.MAX_VALUE;

        int maxRight =
                Integer.MIN_VALUE;

        List<Integer> heights =
                new ArrayList<>();

        for (Fragment fragment :
                fragments) {

            minLeft =
                    Math.min(
                            minLeft,
                            fragment.getLeft());

            maxRight =
                    Math.max(
                            maxRight,
                            fragment.getRight());

            heights.add(
                    fragment.getHeight());
        }

        int horizontalSpan =
                Math.max(
                        1,
                        maxRight - minLeft);

        double minPairDx =
                horizontalSpan
                        * MIN_WIDE_PAIR_SPAN_RATIO;

        double medianHeight =
                medianInt(
                        heights);

        double maxPairDy =
                Math.max(
                        1.0,
                        medianHeight
                                * MAX_PAIR_VERTICAL_DISTANCE_HEIGHTS);

        List<Double> candidateSlopes =
                new ArrayList<>();

        for (int i = 0;
             i < fragments.size();
             i++) {

            Fragment a =
                    fragments.get(i);

            for (int j = i + 1;
                 j < fragments.size();
                 j++) {

                Fragment b =
                        fragments.get(j);

                double dx =
                        b.getCentreX()
                                - a.getCentreX();

                if (Math.abs(dx)
                        < minPairDx) {
                    continue;
                }

                double dy =
                        b.getCentreY()
                                - a.getCentreY();

                if (Math.abs(dy)
                        > maxPairDy) {
                    continue;
                }

                double slope =
                        dy / dx;

                if (Math.abs(slope)
                        <= MAX_ABS_SLOPE) {

                    candidateSlopes.add(
                            slope);
                }
            }
        }

        if (candidateSlopes.size()
                < 3) {
            return 0.0;
        }

        double bestSlope = 0.0;
        int bestSupport = -1;

        for (double candidate :
                candidateSlopes) {

            int support = 0;

            for (double other :
                    candidateSlopes) {

                if (Math.abs(
                        other - candidate)
                        <= SLOPE_CLUSTER_RADIUS) {

                    support++;
                }
            }

            if (support > bestSupport
                    || (support == bestSupport
                    && Math.abs(candidate)
                    < Math.abs(bestSlope))) {

                bestSupport =
                        support;

                bestSlope =
                        candidate;
            }
        }

        int minimumSupport =
                Math.max(
                        3,
                        (int) Math.ceil(
                                candidateSlopes.size()
                                        * 0.18));

        if (bestSupport
                < minimumSupport) {
            return 0.0;
        }

        List<Double> refined =
                new ArrayList<>();

        for (double slope :
                candidateSlopes) {

            if (Math.abs(
                    slope - bestSlope)
                    <= SLOPE_CLUSTER_RADIUS) {

                refined.add(
                        slope);
            }
        }

        if (refined.size() < 3) {
            return 0.0;
        }

        Collections.sort(
                refined);

        double result =
                medianDouble(
                        refined);

        return Math.abs(result)
                < 0.006
                ? 0.0
                : result;
    }

    private static String buildText(
            List<Row> rows) {

        StringBuilder result =
                new StringBuilder();

        for (Row row : rows) {
            String text =
                    row.buildText();

            if (text.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {
                result.append('\n');
            }

            result.append(
                    text);
        }

        return result.toString();
    }

    private static int minimumLeft(
            List<Fragment> fragments) {

        int result =
                Integer.MAX_VALUE;

        for (Fragment fragment :
                fragments) {

            result =
                    Math.min(
                            result,
                            fragment.getLeft());
        }

        return result
                == Integer.MAX_VALUE
                ? 0
                : result;
    }

    private static double adjustedCentreY(
            Fragment fragment,
            double slope,
            double referenceX) {

        return fragment.getCentreY()
                - slope
                * (fragment.getCentreX()
                - referenceX);
    }

    private static double adjustedTop(
            Fragment fragment,
            double slope,
            double referenceX) {

        return fragment.getTop()
                - slope
                * (fragment.getCentreX()
                - referenceX);
    }

    private static double adjustedBottom(
            Fragment fragment,
            double slope,
            double referenceX) {

        return fragment.getBottom()
                - slope
                * (fragment.getCentreX()
                - referenceX);
    }

    private static double medianInt(
            List<Integer> values) {

        if (values == null
                || values.isEmpty()) {
            return 1.0;
        }

        List<Integer> sorted =
                new ArrayList<>(
                        values);

        Collections.sort(
                sorted);

        int middle =
                sorted.size() / 2;

        if ((sorted.size() & 1)
                == 1) {
            return sorted.get(
                    middle);
        }

        return (sorted.get(
                        middle - 1)
                + sorted.get(
                        middle))
                / 2.0;
    }

    private static double medianDouble(
            List<Double> sortedValues) {

        if (sortedValues == null
                || sortedValues.isEmpty()) {
            return 0.0;
        }

        int middle =
                sortedValues.size() / 2;

        if ((sortedValues.size() & 1)
                == 1) {
            return sortedValues.get(
                    middle);
        }

        return (sortedValues.get(
                        middle - 1)
                + sortedValues.get(
                        middle))
                / 2.0;
    }

    private static final class Row {
        private final List<Fragment> fragments =
                new ArrayList<>();

        private final double skewSlope;
        private final double referenceX;

        Row(
                Fragment first,
                double skewSlope,
                double referenceX) {

            this.skewSlope =
                    skewSlope;

            this.referenceX =
                    referenceX;

            add(first);
        }

        void add(
                Fragment fragment) {

            fragments.add(
                    fragment);
        }

        double getRepresentativeCentreY() {
            List<Double> centres =
                    new ArrayList<>();

            for (Fragment fragment :
                    fragments) {

                centres.add(
                        adjustedCentreY(
                                fragment,
                                skewSlope,
                                referenceX));
            }

            Collections.sort(
                    centres);

            return medianDouble(
                    centres);
        }

        int getRepresentativeHeight() {
            List<Integer> heights =
                    new ArrayList<>();

            for (Fragment fragment :
                    fragments) {

                heights.add(
                        fragment.getHeight());
            }

            Collections.sort(
                    heights);

            return Math.max(
                    1,
                    (int) Math.round(
                            medianInt(
                                    heights)));
        }

        boolean isSameVisualRow(
                Fragment candidate,
                boolean allowOverlapMatch) {

            double rowCentre =
                    getRepresentativeCentreY();

            int rowHeight =
                    getRepresentativeHeight();

            double candidateCentre =
                    adjustedCentreY(
                            candidate,
                            skewSlope,
                            referenceX);

            double centreDistance =
                    Math.abs(
                            rowCentre
                                    - candidateCentre);

            double allowedDistance =
                    Math.max(
                                    rowHeight,
                                    candidate.getHeight())
                            * MAX_CENTRE_DISTANCE_RATIO;

            if (centreDistance
                    <= allowedDistance) {
                return true;
            }

            if (!allowOverlapMatch) {
                return false;
            }

            // MJ1 fallback. This is useful when raw bounding boxes differ in
            // height but still overlap the same printed row.
            double rowTop =
                    rowCentre
                            - rowHeight / 2.0;

            double rowBottom =
                    rowCentre
                            + rowHeight / 2.0;

            double candidateTop =
                    adjustedTop(
                            candidate,
                            skewSlope,
                            referenceX);

            double candidateBottom =
                    adjustedBottom(
                            candidate,
                            skewSlope,
                            referenceX);

            double overlap =
                    Math.min(
                                    rowBottom,
                                    candidateBottom)
                            - Math.max(
                                    rowTop,
                                    candidateTop);

            int smallerHeight =
                    Math.min(
                            rowHeight,
                            candidate.getHeight());

            return overlap > 0
                    && overlap
                    >= smallerHeight
                    * MIN_VERTICAL_OVERLAP_RATIO;
        }

        String buildText() {
            fragments.sort(
                    Comparator.comparingInt(
                            Fragment::getLeft));

            StringBuilder result =
                    new StringBuilder();

            for (Fragment fragment :
                    fragments) {

                String cleanText =
                        fragment.getText()
                                .trim();

                if (cleanText.isEmpty()) {
                    continue;
                }

                if (result.length() > 0) {
                    result.append(' ');
                }

                result.append(
                        cleanText);
            }

            return result.toString();
        }
    }
}
