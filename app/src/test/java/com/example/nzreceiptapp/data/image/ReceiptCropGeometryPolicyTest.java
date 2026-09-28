package com.example.nzreceiptapp.data.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReceiptCropGeometryPolicyTest {

    @Test public void rectangleUsesSafeRoi() {
        ReceiptCropGeometryPolicy.Decision d = ReceiptCropGeometryPolicy.decide(points(
                0,0, 100,0, 100,300, 0,300));
        assertEquals(ReceiptCropGeometryPolicy.Mode.SAFE_ROI, d.mode);
        assertTrue(d.severity < 0.24);
    }

    @Test public void mildSkewStillUsesSafeRoi() {
        ReceiptCropGeometryPolicy.Decision d = ReceiptCropGeometryPolicy.decide(points(
                10,0, 110,4, 104,304, 4,300));
        assertEquals(ReceiptCropGeometryPolicy.Mode.SAFE_ROI, d.mode);
    }

    @Test public void strongTrapezoidUsesPerspectiveWarp() {
        ReceiptCropGeometryPolicy.Decision d = ReceiptCropGeometryPolicy.decide(points(
                35,0, 85,0, 120,300, 0,300));
        assertEquals(ReceiptCropGeometryPolicy.Mode.PERSPECTIVE_WARP, d.mode);
        assertTrue(d.severity >= 0.24);
    }

    private static ReceiptCropGeometryPolicy.Point[] points(
            double x0,double y0,double x1,double y1,
            double x2,double y2,double x3,double y3) {
        return new ReceiptCropGeometryPolicy.Point[] {
                new ReceiptCropGeometryPolicy.Point(x0,y0),
                new ReceiptCropGeometryPolicy.Point(x1,y1),
                new ReceiptCropGeometryPolicy.Point(x2,y2),
                new ReceiptCropGeometryPolicy.Point(x3,y3)
        };
    }
}
