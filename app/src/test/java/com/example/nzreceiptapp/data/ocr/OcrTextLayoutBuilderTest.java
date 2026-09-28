package com.example.nzreceiptapp.data.ocr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class OcrTextLayoutBuilderTest {

    private final OcrTextLayoutBuilder builder =
            new OcrTextLayoutBuilder();

    @Test
    public void build_realWoolworths_doesNotShiftPriceColumn() {
        List<OcrTextLayoutBuilder.Fragment> fragments =
                Arrays.asList(
                        f("WW Milk Standard 3L",93,369,249,392),
                        f("WW Chicken Wings Med",95,395,259,417),
                        f("Gopala Natural Yog Full Cream 750g",98,413,379,442),
                        f("Essentials Pasta Spirals 500g",99,440,337,466),
                        f("Essentials Macaroni 500g",99,465,295,490),
                        f("WH Spaghetti 500g",97,491,237,513),
                        f("^ Avalanche 992 SF Cappuccino 160g 10Pk",91,511,413,537),
                        f("#SALVATION ARMY DONATION",91,539,288,559),

                        f("7.33",523,359,557,377),
                        f("11.17",515,383,558,402),
                        f("4.75",524,408,559,427),
                        f("1.49",525,434,559,452),
                        f("1.49",526,459,560,477),
                        f("1.79",526,485,561,503),
                        f("3.79",526,510,561,528),
                        f("0.19",527,537,562,554)
                );

        assertEquals(
                "WW Milk Standard 3L 7.33\n"
                        + "WW Chicken Wings Med 11.17\n"
                        + "Gopala Natural Yog Full Cream 750g 4.75\n"
                        + "Essentials Pasta Spirals 500g 1.49\n"
                        + "Essentials Macaroni 500g 1.49\n"
                        + "WH Spaghetti 500g 1.79\n"
                        + "^ Avalanche 992 SF Cappuccino 160g 10Pk 3.79\n"
                        + "#SALVATION ARMY DONATION 0.19",
                builder.build(fragments));
    }

    @Test
    public void build_realPaknsaveRotatedReceipt_stillUsesSkewWhenItIsBetter() {
        List<OcrTextLayoutBuilder.Fragment> fragments =
                Arrays.asList(
                        f("COTTEE CHOC/WHIZ 220G",493,1292,945,1349),
                        f("CRAIGS JAM RED PLUM 3756",495,1350,1013,1413),
                        f("IDDY BIDDY FRUIT BITS DISNEY PRNCS 160G",499,1398,1336,1473),
                        f("M/MS MINI TUBES 356",500,1473,910,1534),
                        f("OFO0D SEAWEED SNACK GREEN 40G",503,1527,1127,1594),
                        f("2 @ $5.49",703,1592,1001,1650),
                        f("PAMS MILK POWDER WHOLE 400G",510,1650,1090,1721),
                        f("PAMS POPCORN SWEET/SALTY 856",514,1712,1115,1782),
                        f("RED SEAL H/C FRUIT TEA SUPER FRUITS 20EA",520,1754,1373,1845),
                        f("RED SEAL TEA BAGS RELAXING 25S",525,1824,1164,1897),
                        f("SKITTLES FRUITS TUBE 30G",532,1886,1041,1954),
                        f("NZ BEEF SCHNITZEL",538,1950,894,2010),

                        f("$4.69",1526,1269,1629,1317),
                        f("$3.29",1529,1330,1633,1377),
                        f("$2.99",1532,1387,1633,1436),
                        f("$0.89",1534,1443,1637,1494),
                        f("$10.98",1520,1566,1645,1617),
                        f("$6.99",1544,1622,1651,1679),
                        f("$1.19",1549,1693,1649,1740),
                        f("$3.49",1549,1750,1652,1796),
                        f("$3.49",1549,1803,1652,1850),
                        f("$0.99",1551,1862,1650,1907),
                        f("$8.74",1552,1918,1653,1964)
                );

        assertEquals(
                "COTTEE CHOC/WHIZ 220G $4.69\n"
                        + "CRAIGS JAM RED PLUM 3756 $3.29\n"
                        + "IDDY BIDDY FRUIT BITS DISNEY PRNCS 160G $2.99\n"
                        + "M/MS MINI TUBES 356 $0.89\n"
                        + "OFO0D SEAWEED SNACK GREEN 40G\n"
                        + "2 @ $5.49 $10.98\n"
                        + "PAMS MILK POWDER WHOLE 400G $6.99\n"
                        + "PAMS POPCORN SWEET/SALTY 856 $1.19\n"
                        + "RED SEAL H/C FRUIT TEA SUPER FRUITS 20EA $3.49\n"
                        + "RED SEAL TEA BAGS RELAXING 25S $3.49\n"
                        + "SKITTLES FRUITS TUBE 30G $0.99\n"
                        + "NZ BEEF SCHNITZEL $8.74",
                builder.build(fragments));
    }

    @Test
    public void scoring_prefersHealthyRowsOverShiftedRows() {
        int healthy =
                builder.scoreForTesting(
                        Arrays.asList(
                                "WW Milk Standard 3L 7.33",
                                "WW Chicken Wings Med 11.17",
                                "Gopala Natural Yog Full Cream 750g 4.75"));

        int shifted =
                builder.scoreForTesting(
                        Arrays.asList(
                                "7.33",
                                "WW Milk Standard 3L 11.17",
                                "WW Chicken Wings Med 4.75"));

        assertTrue(
                healthy > shifted);
    }

    private OcrTextLayoutBuilder.Fragment f(
            String s,
            int l,
            int t,
            int r,
            int b) {

        return new OcrTextLayoutBuilder.Fragment(
                s,l,t,r,b);
    }
}
