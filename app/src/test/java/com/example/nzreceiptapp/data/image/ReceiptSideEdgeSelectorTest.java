package com.example.nzreceiptapp.data.image;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ReceiptSideEdgeSelectorTest {

    @Test
    public void select_acceptsLongParallelReceiptSidesWithoutCorners() {
        ReceiptSideEdgeSelector.Edge left =
                edge(780, 0, 850, 1399);
        ReceiptSideEdgeSelector.Edge right =
                edge(2250, 0, 2310, 1399);

        ReceiptSideEdgeSelector.Pair pair =
                ReceiptSideEdgeSelector.select(
                        Arrays.asList(left, right),
                        3000,
                        1400
                );

        assertNotNull(pair);
        assertSame(left, pair.left);
        assertSame(right, pair.right);
        assertTrue(pair.widthRatio > 0.45);
        assertTrue(pair.score >= 0.48);
    }

    @Test
    public void select_rejectsShortKeyboardLikeEdges() {
        List<ReceiptSideEdgeSelector.Edge> edges = Arrays.asList(
                edge(220, 500, 230, 700),
                edge(500, 520, 510, 730),
                edge(800, 480, 810, 690)
        );

        assertNull(ReceiptSideEdgeSelector.select(
                edges,
                3000,
                1400
        ));
    }

    @Test
    public void select_prefersCentralReceiptPairOverLeftNoisePair() {
        ReceiptSideEdgeSelector.Edge noiseLeft =
                edge(150, 0, 165, 1399);
        ReceiptSideEdgeSelector.Edge noiseRight =
                edge(700, 0, 710, 1399);

        ReceiptSideEdgeSelector.Edge receiptLeft =
                edge(820, 0, 870, 1399);
        ReceiptSideEdgeSelector.Edge receiptRight =
                edge(2300, 0, 2350, 1399);

        ReceiptSideEdgeSelector.Pair pair =
                ReceiptSideEdgeSelector.select(
                        Arrays.asList(
                                noiseLeft,
                                noiseRight,
                                receiptLeft,
                                receiptRight
                        ),
                        3000,
                        1400
                );

        assertNotNull(pair);
        assertSame(receiptLeft, pair.left);
        assertSame(receiptRight, pair.right);
    }

    @Test
    public void select_rejectsStronglyDivergingPair() {
        ReceiptSideEdgeSelector.Edge left =
                edge(800, 0, 1200, 1399);
        ReceiptSideEdgeSelector.Edge right =
                edge(2200, 0, 1700, 1399);

        assertNull(ReceiptSideEdgeSelector.select(
                Arrays.asList(left, right),
                3000,
                1400
        ));
    }

    private ReceiptSideEdgeSelector.Edge edge(
            double x1, double y1, double x2, double y2) {
        return new ReceiptSideEdgeSelector.Edge(
                x1, y1, x2, y2);
    }
}
