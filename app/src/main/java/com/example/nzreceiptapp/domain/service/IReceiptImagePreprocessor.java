package com.example.nzreceiptapp.domain.service;

import java.util.List;

/**
 * Analyze first, then apply the user-reviewed crop.
 * The original image is never overwritten.
 */
public interface IReceiptImagePreprocessor {
    ReceiptCropProposal analyze(String originalStoredUri);

    Result crop(String originalStoredUri,
                List<ReceiptCropProposal.Point> corners);

    void cleanup(Result result);

    final class Result {
        private final String originalImageUri;
        private final String ocrImageUri;
        private final boolean temporaryOcrImage;

        public Result(String originalImageUri,
                      String ocrImageUri,
                      boolean temporaryOcrImage) {
            this.originalImageUri = originalImageUri;
            this.ocrImageUri = ocrImageUri;
            this.temporaryOcrImage = temporaryOcrImage;
        }

        public static Result passThrough(String uri) {
            return new Result(uri, uri, false);
        }

        public String getOriginalImageUri() { return originalImageUri; }
        public String getOcrImageUri() { return ocrImageUri; }
        public boolean isTemporaryOcrImage() { return temporaryOcrImage; }
    }
}
