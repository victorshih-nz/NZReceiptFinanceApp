package com.example.nzreceiptapp.data.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.example.nzreceiptapp.domain.service.ReceiptCropProposal;
import org.junit.Test;

public class ReceiptCropProposalTest {
    @Test public void pointClampsToNormalizedRange() {
        ReceiptCropProposal.Point p = new ReceiptCropProposal.Point(-1, 2);
        assertEquals(0.0, p.getX(), 0.0001);
        assertEquals(1.0, p.getY(), 0.0001);
    }

    @Test public void fullImageHasFourClockwiseCorners() {
        ReceiptCropProposal p = ReceiptCropProposal.fullImage("file:///x.jpg", "");
        assertFalse(p.isAutoDetected());
        assertEquals(4, p.getCorners().size());
        assertEquals(0.0, p.getCorners().get(0).getX(), 0.0001);
        assertEquals(1.0, p.getCorners().get(1).getX(), 0.0001);
        assertEquals(1.0, p.getCorners().get(2).getY(), 0.0001);
        assertEquals(0.0, p.getCorners().get(3).getX(), 0.0001);
    }
}
