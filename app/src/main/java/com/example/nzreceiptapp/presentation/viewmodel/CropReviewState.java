package com.example.nzreceiptapp.presentation.viewmodel;

import com.example.nzreceiptapp.domain.service.ReceiptCropProposal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CropReviewState {
    public enum Phase { IDLE, PREPARING, READY, APPLYING, ERROR }

    private final Phase phase;
    private final String originalImageUri;
    private final List<ReceiptCropProposal.Point> suggestedCorners;
    private final boolean autoDetected;
    private final String detectionMode;
    private final String message;

    private CropReviewState(Phase phase,
                            String originalImageUri,
                            List<ReceiptCropProposal.Point> suggestedCorners,
                            boolean autoDetected,
                            String detectionMode,
                            String message) {
        this.phase = phase;
        this.originalImageUri = originalImageUri;
        this.suggestedCorners = suggestedCorners == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(suggestedCorners));
        this.autoDetected = autoDetected;
        this.detectionMode = detectionMode == null ? "" : detectionMode;
        this.message = message == null ? "" : message;
    }

    public static CropReviewState idle() {
        return new CropReviewState(Phase.IDLE, null, null, false, "", "");
    }

    public static CropReviewState preparing() {
        return new CropReviewState(Phase.PREPARING, null, null, false, "", "");
    }

    public static CropReviewState ready(ReceiptCropProposal p) {
        return new CropReviewState(Phase.READY, p.getOriginalImageUri(),
                p.getCorners(), p.isAutoDetected(),
                p.getDetectionMode(), p.getMessage());
    }

    public static CropReviewState applying(String uri,
                                           List<ReceiptCropProposal.Point> c) {
        return new CropReviewState(Phase.APPLYING, uri, c, false, "", "");
    }

    public static CropReviewState error(String uri, String message) {
        return new CropReviewState(Phase.ERROR, uri, null, false, "", message);
    }

    public Phase getPhase() { return phase; }
    public String getOriginalImageUri() { return originalImageUri; }
    public List<ReceiptCropProposal.Point> getSuggestedCorners() { return suggestedCorners; }
    public boolean isAutoDetected() { return autoDetected; }
    public String getDetectionMode() { return detectionMode; }
    public String getMessage() { return message; }
    public boolean isReady() { return phase == Phase.READY; }
}
