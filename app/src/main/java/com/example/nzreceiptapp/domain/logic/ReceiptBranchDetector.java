package com.example.nzreceiptapp.domain.logic;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative receipt-header branch extraction.
 */
public final class ReceiptBranchDetector {

    /*
     * Important: use horizontal whitespace only.
     *
     * Java \s also matches line breaks. The previous pattern could therefore
     * start matching the standalone "PAK'nSAVE" logo line and continue onto
     * the following header line, causing the captured branch to include
     * "PAK nSAVE ..." itself.
     */
    private static final Pattern PAKNSAVE_BRANCH_PATTERN =
            Pattern.compile(
                    "(?im)^[ \\t]*[*%]*[ \\t]*"
                            + "PAK[ \\t]*['’]?[ \\t]*N?SAVE[ \\t]+"
                            + "([A-Z][A-Z0-9 '&.-]{1,40}?)"
                            + "(?:[ \\t]*\\*+)?[ \\t]*$",
                    Pattern.CASE_INSENSITIVE);

    private ReceiptBranchDetector() { }

    public static String detect(
            String rawText,
            String chainName) {

        if (rawText == null
                || rawText.trim().isEmpty()
                || chainName == null) {
            return "";
        }

        String normalizedChain =
                chainName
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", "");

        if ("paknsave".equals(normalizedChain)) {
            Matcher matcher =
                    PAKNSAVE_BRANCH_PATTERN.matcher(rawText);

            if (matcher.find()) {
                return clean(matcher.group(1));
            }
        }

        return "";
    }

    private static String clean(String value) {
        if (value == null) return "";

        return value
                .replaceAll("\\*+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
