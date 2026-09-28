package com.example.nzreceiptapp.data.image;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReceiptBoundaryGeometryTest {
    private static final double EPS = 0.0001;

    @Test
    public void orderCorners_returnsTlTrBrBl() {
        ReceiptBoundaryGeometry.Point[] shuffled = {
                p(900, 1800), p(100, 200), p(120, 1820), p(880, 180)
        };
        ReceiptBoundaryGeometry.Point[] ordered =
                ReceiptBoundaryGeometry.orderCorners(shuffled);
        assertPoint(100, 200, ordered[0]);
        assertPoint(880, 180, ordered[1]);
        assertPoint(900, 1800, ordered[2]);
        assertPoint(120, 1820, ordered[3]);
    }

    @Test
    public void polygonArea_calculatesReceiptCoverage() {
        ReceiptBoundaryGeometry.Point[] rectangle = {
                p(100, 100), p(900, 100), p(900, 1900), p(100, 1900)
        };
        assertEquals(1_440_000.0,
                ReceiptBoundaryGeometry.polygonArea(rectangle), EPS);
    }

    @Test
    public void expandFromCenter_addsMarginButClampsToImage() {
        ReceiptBoundaryGeometry.Point[] rectangle = {
                p(10, 10), p(990, 10), p(990, 1990), p(10, 1990)
        };
        ReceiptBoundaryGeometry.Point[] expanded =
                ReceiptBoundaryGeometry.expandFromCenter(rectangle, 1.05, 999, 1999);
        assertEquals(0.0, expanded[0].x, EPS);
        assertEquals(0.0, expanded[0].y, EPS);
        assertEquals(999.0, expanded[2].x, EPS);
        assertEquals(1999.0, expanded[2].y, EPS);
    }

    @Test
    public void distance_supportsPerspectiveSizing() {
        assertEquals(5.0, ReceiptBoundaryGeometry.distance(p(0, 0), p(3, 4)), EPS);
    }

    private ReceiptBoundaryGeometry.Point p(double x, double y) {
        return new ReceiptBoundaryGeometry.Point(x, y);
    }

    private void assertPoint(double x, double y, ReceiptBoundaryGeometry.Point actual) {
        assertEquals(x, actual.x, EPS);
        assertEquals(y, actual.y, EPS);
    }
}
