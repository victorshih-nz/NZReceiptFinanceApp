package com.example.nzreceiptapp.data.ocr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rebuilds reading rows from OCR fragments while preserving their image positions.
 *
 * <p>ML Kit may return the item-name column and the price column as separate text
 * blocks. Flattening those blocks with {@code Text#getText()} loses the fact that
 * two fragments were printed on the same receipt row. This class groups fragments
 * with matching vertical positions and then joins each row from left to right.</p>
 *
 * <p>This class deliberately has no Android or ML Kit dependency, so the layout
 * algorithm can be covered by ordinary JVM unit tests.</p>
 */
public final class OcrTextLayoutBuilder {

    private static final double MIN_VERTICAL_OVERLAP_RATIO = 0.25;
    private static final double MAX_CENTRE_DISTANCE_RATIO = 0.60;

    /** A recognised piece of text and its bounding rectangle in image pixels. */
    public static final class Fragment {
        private final String text;
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        public Fragment(String text, int left, int top, int right, int bottom) {
            this.text = text;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        String getText() {
            return text;
        }

        int getLeft() {
            return left;
        }

        int getTop() {
            return top;
        }

        int getBottom() {
            return bottom;
        }

        int getHeight() {
            return Math.max(1, bottom - top);
        }

        double getCentreY() {
            return top + getHeight() / 2.0;
        }

        boolean isUsable() {
            return text != null && !text.trim().isEmpty()
                    && right > left && bottom > top;
        }
    }

    /** Returns receipt text ordered by row and then from left to right. */
    public String build(List<Fragment> sourceFragments) {
        if (sourceFragments == null || sourceFragments.isEmpty()) return "";

        List<Fragment> fragments = new ArrayList<>();
        for (Fragment fragment : sourceFragments) {
            if (fragment != null && fragment.isUsable()) fragments.add(fragment);
        }
        fragments.sort(Comparator
                .comparingInt(Fragment::getTop)
                .thenComparingInt(Fragment::getLeft));

        List<Row> rows = new ArrayList<>();
        for (Fragment fragment : fragments) {
            Row bestRow = null;
            double bestDistance = Double.MAX_VALUE;
            for (Row row : rows) {
                if (!row.isSameVisualRow(fragment)) continue;
                double distance = Math.abs(row.getCentreY() - fragment.getCentreY());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestRow = row;
                }
            }

            if (bestRow == null) {
                rows.add(new Row(fragment));
            } else {
                bestRow.add(fragment);
            }
        }

        rows.sort(Comparator.comparingDouble(Row::getCentreY));
        StringBuilder result = new StringBuilder();
        for (Row row : rows) {
            String rowText = row.buildText();
            if (rowText.isEmpty()) continue;
            if (result.length() > 0) result.append('\n');
            result.append(rowText);
        }
        return result.toString();
    }

    private static final class Row {
        private final List<Fragment> fragments = new ArrayList<>();
        private int top;
        private int bottom;
        private double centreYTotal;

        Row(Fragment first) {
            top = first.getTop();
            bottom = first.getBottom();
            add(first);
        }

        void add(Fragment fragment) {
            fragments.add(fragment);
            top = Math.min(top, fragment.getTop());
            bottom = Math.max(bottom, fragment.getBottom());
            centreYTotal += fragment.getCentreY();
        }

        double getCentreY() {
            return centreYTotal / fragments.size();
        }

        int getHeight() {
            return Math.max(1, bottom - top);
        }

        boolean isSameVisualRow(Fragment fragment) {
            int overlap = Math.min(bottom, fragment.getBottom())
                    - Math.max(top, fragment.getTop());
            int smallerHeight = Math.min(getHeight(), fragment.getHeight());
            boolean enoughOverlap = overlap > 0
                    && overlap >= smallerHeight * MIN_VERTICAL_OVERLAP_RATIO;

            double centreDistance = Math.abs(getCentreY() - fragment.getCentreY());
            double allowedDistance = Math.max(getHeight(), fragment.getHeight())
                    * MAX_CENTRE_DISTANCE_RATIO;
            return enoughOverlap || centreDistance <= allowedDistance;
        }

        String buildText() {
            fragments.sort(Comparator.comparingInt(Fragment::getLeft));
            StringBuilder result = new StringBuilder();
            for (Fragment fragment : fragments) {
                String cleanText = fragment.getText().trim();
                if (cleanText.isEmpty()) continue;
                if (result.length() > 0) result.append(' ');
                result.append(cleanText);
            }
            return result.toString();
        }
    }
}
