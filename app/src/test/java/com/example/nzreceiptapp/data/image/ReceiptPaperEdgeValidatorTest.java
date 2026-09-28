package com.example.nzreceiptapp.data.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class ReceiptPaperEdgeValidatorTest {

    @Test
    public void acceptsWhitePaperAgainstDarkBackground() {
        ReceiptSideEdgeSelector.Edge left =
                new ReceiptSideEdgeSelector.Edge(300, 0, 320, 999);
        ReceiptSideEdgeSelector.Edge right =
                new ReceiptSideEdgeSelector.Edge(700, 0, 720, 999);

        ReceiptSideEdgeSelector.Pair pair =
                ReceiptSideEdgeSelector.select(
                        Arrays.asList(left, right),
                        1000,
                        1000
                );

        ReceiptPaperEdgeValidator.Result result =
                ReceiptPaperEdgeValidator.validate(
                        pair,
                        1000,
                        1000,
                        70,
                        (x, y) -> x >= 310 && x <= 710 ? 235 : 45
                );

        assertTrue(result.accepted);
        assertTrue(result.leftEvidence > result.adaptiveContrastThreshold);
        assertTrue(result.rightEvidence > result.adaptiveContrastThreshold);
    }

    @Test
    public void rejectsInternalLinesOnSamePaper() {
        ReceiptSideEdgeSelector.Edge left =
                new ReceiptSideEdgeSelector.Edge(350, 0, 350, 999);
        ReceiptSideEdgeSelector.Edge right =
                new ReceiptSideEdgeSelector.Edge(650, 0, 650, 999);

        ReceiptSideEdgeSelector.Pair pair =
                ReceiptSideEdgeSelector.select(
                        Arrays.asList(left, right),
                        1000,
                        1000
                );

        ReceiptPaperEdgeValidator.Result result =
                ReceiptPaperEdgeValidator.validate(
                        pair,
                        1000,
                        1000,
                        20,
                        (x, y) -> 230
                );

        assertFalse(result.accepted);
    }

    @Test
    public void rejectsOneGoodAndOneFakeEdge() {
        ReceiptSideEdgeSelector.Edge left =
                new ReceiptSideEdgeSelector.Edge(250, 0, 250, 999);
        ReceiptSideEdgeSelector.Edge right =
                new ReceiptSideEdgeSelector.Edge(700, 0, 700, 999);

        ReceiptSideEdgeSelector.Pair pair =
                ReceiptSideEdgeSelector.select(
                        Arrays.asList(left, right),
                        1000,
                        1000
                );

        ReceiptPaperEdgeValidator.Result result =
                ReceiptPaperEdgeValidator.validate(
                        pair,
                        1000,
                        1000,
                        50,
                        (x, y) -> {
                            // Real left paper/background boundary.
                            if (x < 250) return 50;
                            // Entire right side remains paper-like, so the right
                            // candidate is an internal/fake boundary.
                            return 230;
                        }
                );

        assertFalse(result.accepted);
    }

    @Test
    public void adaptiveMarginIsNotAFixedCropWidth() {
        ReceiptSideEdgeSelector.Edge left =
                new ReceiptSideEdgeSelector.Edge(250, 0, 250, 999);
        ReceiptSideEdgeSelector.Edge right =
                new ReceiptSideEdgeSelector.Edge(750, 0, 750, 999);

        ReceiptSideEdgeSelector.Pair pair =
                ReceiptSideEdgeSelector.select(
                        Arrays.asList(left, right),
                        1000,
                        1000
                );

        ReceiptPaperEdgeValidator.Result strong =
                ReceiptPaperEdgeValidator.validate(
                        pair,
                        1000,
                        1000,
                        30,
                        (x, y) -> x >= 250 && x <= 750 ? 245 : 20
                );

        ReceiptPaperEdgeValidator.Result weaker =
                ReceiptPaperEdgeValidator.validate(
                        pair,
                        1000,
                        1000,
                        30,
                        (x, y) -> x >= 250 && x <= 750 ? 210 : 150
                );

        assertTrue(strong.suggestedMarginRatio <= weaker.suggestedMarginRatio);
    }
}
