package com.example.nzreceiptapp.domain.logic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReceiptBranchDetectorTest {

    @Test
    public void detectsPaknSaveRoyalOak() {
        String text =
                "PAK'nSAVE\n"
                        + "PAK nSAVE ROYAL OAK ****\n"
                        + "691 MANUKAU RD. ROYAL 0AK *****";

        assertEquals(
                "ROYAL OAK",
                ReceiptBranchDetector.detect(
                        text,
                        "PAK'nSAVE"));
    }

    @Test
    public void detectsPaknSaveWithApostropheSpacing() {
        String text =
                "PAK' nSAVE ROYAL OAK ****\n";

        assertEquals(
                "ROYAL OAK",
                ReceiptBranchDetector.detect(
                        text,
                        "PAK'nSAVE"));
    }

    @Test
    public void detectsPaknSaveWithLeadingStars() {
        String text =
                "**** PAK'nSAVE ROYAL OAK ****\n";

        assertEquals(
                "ROYAL OAK",
                ReceiptBranchDetector.detect(
                        text,
                        "PAK'nSAVE"));
    }

    @Test
    public void standaloneLogoLineDoesNotConsumeNextLine() {
        String text =
                "PAK'nSAVE\n"
                        + "UNRELATED TEXT\n";

        assertEquals(
                "",
                ReceiptBranchDetector.detect(
                        text,
                        "PAK'nSAVE"));
    }

    @Test
    public void doesNotOverrideFromAddressOnly() {
        String text =
                "691 MANUKAU RD, ROYAL OAK\n"
                        + "HUTCHINSON BROS LTD\n";

        assertEquals(
                "",
                ReceiptBranchDetector.detect(
                        text,
                        "PAK'nSAVE"));
    }
}
