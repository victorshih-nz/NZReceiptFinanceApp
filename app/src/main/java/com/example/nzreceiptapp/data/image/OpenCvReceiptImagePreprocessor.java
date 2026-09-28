package com.example.nzreceiptapp.data.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.example.nzreceiptapp.domain.service.IReceiptImagePreprocessor;
import com.example.nzreceiptapp.domain.service.ReceiptCropProposal;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * MJ2: OpenCV proposes a crop; UI confirms/edits it before OCR.
 */
public final class OpenCvReceiptImagePreprocessor
        implements IReceiptImagePreprocessor {

    private static final String TAG = "ReceiptCrop";
    private static final int MAX_WORKING_LONG_EDGE = 3200;
    private static final int ANALYSIS_LONG_EDGE = 1400;
    private static final double MIN_RECEIPT_AREA_RATIO = 0.20;
    private static final double APPROX_EPSILON_RATIO = 0.025;
    private static final int MIN_OUTPUT_EDGE = 220;
    private static final int HOUGH_THRESHOLD = 70;
    private static final double HOUGH_MIN_LINE_LENGTH_RATIO = 0.28;
    private static final double HOUGH_MAX_LINE_GAP_RATIO = 0.08;

    private final Context context;
    private final boolean openCvReady;

    public OpenCvReceiptImagePreprocessor(Context context) {
        this.context = context.getApplicationContext();
        openCvReady = OpenCVLoader.initLocal();
        Log.i(TAG, openCvReady ? "OpenCV initialized" : "OpenCV unavailable");
    }

    @Override
    public ReceiptCropProposal analyze(String originalStoredUri) {
        if (!openCvReady) {
            return ReceiptCropProposal.fullImage(
                    originalStoredUri, "Auto boundary unavailable; adjust manually.");
        }

        File file = resolveFile(originalStoredUri);
        if (file == null || !file.exists()) {
            return ReceiptCropProposal.fullImage(
                    originalStoredUri, "Original image unavailable.");
        }

        Bitmap bitmap = null;
        Mat full = null;
        Mat analysis = null;

        try {
            bitmap = decodeWorkingBitmap(file);
            if (bitmap == null) {
                return ReceiptCropProposal.fullImage(
                        originalStoredUri, "Image decode failed.");
            }

            full = new Mat();
            Utils.bitmapToMat(bitmap, full);

            double scale = Math.min(
                    1.0,
                    ANALYSIS_LONG_EDGE /
                            (double) Math.max(full.cols(), full.rows()));

            analysis = new Mat();
            if (scale < 1.0) {
                Imgproc.resize(full, analysis,
                        new Size(Math.round(full.cols() * scale),
                                Math.round(full.rows() * scale)),
                        0, 0, Imgproc.INTER_AREA);
            } else {
                full.copyTo(analysis);
            }

            Point[] quad = detectReceiptQuad(analysis);
            if (quad != null) {
                Log.i(TAG, "proposal=QUAD");
                return new ReceiptCropProposal(
                        originalStoredUri,
                        normalizeQuad(quad, analysis.cols(), analysis.rows()),
                        true,
                        "QUAD",
                        "Boundary detected. Check and adjust corners.");
            }

            Point[] paperRegion = detectPaperRegionQuad(analysis);
            if (paperRegion != null) {
                Log.i(TAG, "proposal=PAPER_REGION");

                return new ReceiptCropProposal(
                        originalStoredUri,
                        normalizeQuad(
                                paperRegion,
                                analysis.cols(),
                                analysis.rows()),
                        true,
                        "PAPER_REGION",
                        "Approximate receipt boundary detected. Fine-tune the handles.");
            }

            ReceiptSideEdgeSelector.Pair pair = detectSideEdgePair(analysis);
            if (pair != null) {
                ReceiptPaperEdgeValidator.Result evidence =
                        validatePaperEdges(analysis, pair);
                Log.i(TAG, "paper-edge " + evidence);

                if (evidence.accepted) {
                    Log.i(TAG, "proposal=SIDE_EDGES");
                    return new ReceiptCropProposal(
                            originalStoredUri,
                            sidePairCorners(pair,
                                    analysis.cols(),
                                    analysis.rows(),
                                    evidence.suggestedMarginRatio),
                            true,
                            "SIDE_EDGES",
                            "Receipt sides detected. Check and adjust.");
                }
            }

            Log.i(TAG, "proposal=FULL_IMAGE");
            return ReceiptCropProposal.fullImage(
                    originalStoredUri,
                    "Boundary uncertain. Adjust manually or use original.");

        } catch (Exception e) {
            Log.w(TAG, "Boundary analysis failed", e);
            return ReceiptCropProposal.fullImage(
                    originalStoredUri,
                    "Auto boundary failed. Adjust manually or use original.");
        } finally {
            if (analysis != null) analysis.release();
            if (full != null) full.release();
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
    }

    @Override
    public Result crop(String originalStoredUri,
                       List<ReceiptCropProposal.Point> corners) {
        if (!openCvReady || corners == null || corners.size() != 4) {
            return Result.passThrough(originalStoredUri);
        }

        File file = resolveFile(originalStoredUri);
        if (file == null || !file.exists()) {
            return Result.passThrough(originalStoredUri);
        }

        Bitmap bitmap = null;
        Mat full = null;
        Mat masked = null;

        try {
            bitmap = decodeWorkingBitmap(file);
            if (bitmap == null) {
                return Result.passThrough(originalStoredUri);
            }

            full = new Mat();
            Utils.bitmapToMat(bitmap, full);

            ReceiptBoundaryGeometry.Point[] absolute =
                    normalizedToAbsolute(
                            corners,
                            full.cols(),
                            full.rows());

            if (!isValidSelection(
                    absolute,
                    full.cols(),
                    full.rows())) {

                Log.i(TAG,
                        "selection-mask rejected; use original");

                return Result.passThrough(
                        originalStoredUri);
            }

            /*
             * Final OCR-preserving strategy:
             *
             * Keep the original oriented bitmap dimensions and all pixels
             * inside the selected receipt polygon unchanged. Only replace the
             * outside background with white.
             *
             * No crop, resize, interpolation, perspective transform, or JPEG
             * recompression is performed.
             */
            masked =
                    maskOutsideSelection(
                            full,
                            absolute);

            if (masked == null || masked.empty()) {
                Log.i(TAG,
                        "selection-mask failed; use original");

                return Result.passThrough(
                        originalStoredUri);
            }

            String ocrUri =
                    saveWorkingImageLossless(
                            masked);

            Log.i(
                    TAG,
                    "crop applied mode=MASKED_ORIGINAL "
                            + full.cols()
                            + "x"
                            + full.rows()
                            + " -> "
                            + masked.cols()
                            + "x"
                            + masked.rows());

            return new Result(
                    originalStoredUri,
                    ocrUri,
                    true);

        } catch (Exception e) {
            Log.w(
                    TAG,
                    "Selection masking failed; use original",
                    e);

            return Result.passThrough(
                    originalStoredUri);

        } finally {
            if (masked != null) {
                masked.release();
            }

            if (full != null) {
                full.release();
            }

            if (bitmap != null
                    && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    @Override
    public void cleanup(Result result) {
        if (result == null || !result.isTemporaryOcrImage()) return;
        File file = resolveFile(result.getOcrImageUri());
        File dir = new File(context.getFilesDir(), "receipt_ocr_work");
        if (file != null && dir.equals(file.getParentFile()) && !file.delete()) {
            Log.w(TAG, "Unable to delete temp OCR image");
        }
    }

    private Point[] detectReceiptQuad(Mat rgba) {
        Mat gray = new Mat(), blur = new Mat(), edges = new Mat(),
                closed = new Mat(), hierarchy = new Mat();
        Mat kernel = null;
        try {
            toGray(rgba, gray);
            Imgproc.GaussianBlur(gray, blur, new Size(5, 5), 0);
            Imgproc.Canny(blur, edges, 60, 180);
            kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT, new Size(5, 5));
            Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE, kernel);
            Imgproc.dilate(closed, closed, kernel, new Point(-1, -1), 1);

            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(closed, contours, hierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            contours.sort(Comparator.comparingDouble(
                    (MatOfPoint c) -> Imgproc.contourArea(c)).reversed());

            double imageArea = rgba.cols() * (double) rgba.rows();

            for (MatOfPoint contour : contours) {
                if (Imgproc.contourArea(contour) / imageArea
                        < MIN_RECEIPT_AREA_RATIO) continue;

                MatOfPoint2f c2 = new MatOfPoint2f(contour.toArray());
                MatOfPoint2f approx = new MatOfPoint2f();
                try {
                    double perimeter = Imgproc.arcLength(c2, true);
                    Imgproc.approxPolyDP(c2, approx,
                            APPROX_EPSILON_RATIO * perimeter, true);
                    Point[] p = approx.toArray();
                    if (p.length != 4) continue;

                    MatOfPoint polygon = new MatOfPoint(p);
                    boolean convex;
                    try { convex = Imgproc.isContourConvex(polygon); }
                    finally { polygon.release(); }
                    if (!convex) continue;

                    ReceiptBoundaryGeometry.Point[] plain =
                            new ReceiptBoundaryGeometry.Point[4];
                    for (int i = 0; i < 4; i++) {
                        plain[i] = new ReceiptBoundaryGeometry.Point(p[i].x, p[i].y);
                    }
                    ReceiptBoundaryGeometry.Point[] ordered =
                            ReceiptBoundaryGeometry.orderCorners(plain);

                    Point[] result = new Point[4];
                    for (int i = 0; i < 4; i++) {
                        result[i] = new Point(ordered[i].x, ordered[i].y);
                    }
                    return result;
                } finally {
                    c2.release();
                    approx.release();
                }
            }
            return null;
        } finally {
            gray.release(); blur.release(); edges.release();
            closed.release(); hierarchy.release();
            if (kernel != null) kernel.release();
        }
    }

    /**
     * Coarse auto-proposal fallback.
     *
     * It intentionally aims for a useful approximate paper region rather than
     * a final crop. The user will review/adjust it before OCR.
     *
     * Otsu thresholding keeps this scale-independent. We then look for a large,
     * fairly rectangular bright region and convert its minimum-area rectangle
     * into four editable corners.
     */
    private Point[] detectPaperRegionQuad(Mat rgba) {
        Mat gray = new Mat();
        Mat blurred = new Mat();
        Mat binary = new Mat();
        Mat cleaned = new Mat();
        Mat hierarchy = new Mat();
        Mat closeKernel = null;
        Mat openKernel = null;

        try {
            toGray(rgba, gray);

            Imgproc.GaussianBlur(
                    gray,
                    blurred,
                    new Size(7, 7),
                    0);

            Imgproc.threshold(
                    blurred,
                    binary,
                    0,
                    255,
                    Imgproc.THRESH_BINARY
                            | Imgproc.THRESH_OTSU);

            int shortEdge =
                    Math.max(
                            1,
                            Math.min(
                                    rgba.cols(),
                                    rgba.rows()));

            int closeSize =
                    Math.max(
                            5,
                            (int) Math.round(
                                    shortEdge * 0.012));

            if ((closeSize & 1) == 0) {
                closeSize++;
            }

            int openSize =
                    Math.max(
                            3,
                            (int) Math.round(
                                    shortEdge * 0.004));

            if ((openSize & 1) == 0) {
                openSize++;
            }

            closeKernel =
                    Imgproc.getStructuringElement(
                            Imgproc.MORPH_RECT,
                            new Size(
                                    closeSize,
                                    closeSize));

            openKernel =
                    Imgproc.getStructuringElement(
                            Imgproc.MORPH_RECT,
                            new Size(
                                    openSize,
                                    openSize));

            Imgproc.morphologyEx(
                    binary,
                    cleaned,
                    Imgproc.MORPH_CLOSE,
                    closeKernel);

            Imgproc.morphologyEx(
                    cleaned,
                    cleaned,
                    Imgproc.MORPH_OPEN,
                    openKernel);

            List<MatOfPoint> contours =
                    new ArrayList<>();

            Imgproc.findContours(
                    cleaned,
                    contours,
                    hierarchy,
                    Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE);

            double imageArea =
                    rgba.cols()
                            * (double) rgba.rows();

            Point[] best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (MatOfPoint contour : contours) {
                double contourArea =
                        Imgproc.contourArea(contour);

                double areaRatio =
                        contourArea / imageArea;

                // Dynamic proposal only: exclude tiny noise and almost-whole
                // frame backgrounds.
                if (areaRatio < 0.10
                        || areaRatio > 0.90) {
                    continue;
                }

                MatOfPoint2f points =
                        new MatOfPoint2f(
                                contour.toArray());

                try {
                    RotatedRect rect =
                            Imgproc.minAreaRect(points);

                    double rectArea =
                            rect.size.width
                                    * rect.size.height;

                    if (rectArea <= 0) {
                        continue;
                    }

                    double rectangularity =
                            contourArea / rectArea;

                    if (rectangularity < 0.48) {
                        continue;
                    }

                    Point[] box =
                            new Point[4];

                    rect.points(box);

                    ReceiptBoundaryGeometry.Point[] plain =
                            new ReceiptBoundaryGeometry.Point[4];

                    for (int i = 0; i < 4; i++) {
                        plain[i] =
                                new ReceiptBoundaryGeometry.Point(
                                        box[i].x,
                                        box[i].y);
                    }

                    ReceiptBoundaryGeometry.Point[] ordered =
                            ReceiptBoundaryGeometry.orderCorners(
                                    plain);

                    double centerX =
                            rect.center.x
                                    / Math.max(
                                            1.0,
                                            rgba.cols());

                    double centerY =
                            rect.center.y
                                    / Math.max(
                                            1.0,
                                            rgba.rows());

                    double centerDistance =
                            Math.hypot(
                                    centerX - 0.5,
                                    centerY - 0.5);

                    double centerScore =
                            Math.max(
                                    0.0,
                                    1.0
                                            - centerDistance
                                            / 0.71);

                    double longSide =
                            Math.max(
                                    rect.size.width,
                                    rect.size.height);

                    double shortSide =
                            Math.max(
                                    1.0,
                                    Math.min(
                                            rect.size.width,
                                            rect.size.height));

                    double elongation =
                            longSide / shortSide;

                    // Receipts are commonly elongated, but don't hard-require
                    // one aspect ratio because partial receipts vary.
                    double elongationScore =
                            Math.min(
                                    1.0,
                                    elongation / 2.5);

                    double score =
                            0.42 * rectangularity
                                    + 0.28 * centerScore
                                    + 0.20 * Math.min(
                                            1.0,
                                            areaRatio / 0.45)
                                    + 0.10 * elongationScore;

                    if (score <= bestScore) {
                        continue;
                    }

                    Point[] orderedOpenCv =
                            new Point[4];

                    for (int i = 0; i < 4; i++) {
                        orderedOpenCv[i] =
                                new Point(
                                        ordered[i].x,
                                        ordered[i].y);
                    }

                    best = orderedOpenCv;
                    bestScore = score;

                } finally {
                    points.release();
                }
            }

            if (best != null) {
                Log.i(
                        TAG,
                        String.format(
                                Locale.US,
                                "paper-region accepted score=%.3f",
                                bestScore));
            } else {
                Log.i(
                        TAG,
                        "paper-region accepted=none");
            }

            return best;

        } finally {
            gray.release();
            blurred.release();
            binary.release();
            cleaned.release();
            hierarchy.release();

            if (closeKernel != null) {
                closeKernel.release();
            }

            if (openKernel != null) {
                openKernel.release();
            }
        }
    }

    private ReceiptSideEdgeSelector.Pair detectSideEdgePair(Mat rgba) {
        Mat gray = new Mat(), blur = new Mat(), edges = new Mat(),
                strengthened = new Mat(), lines = new Mat();
        Mat kernel = null;
        try {
            toGray(rgba, gray);
            Imgproc.GaussianBlur(gray, blur, new Size(5, 5), 0);
            Imgproc.Canny(blur, edges, 45, 150);

            int kh = Math.max(9, rgba.rows() / 35);
            if ((kh & 1) == 0) kh++;
            kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT, new Size(3, kh));
            Imgproc.morphologyEx(edges, strengthened,
                    Imgproc.MORPH_CLOSE, kernel);

            Imgproc.HoughLinesP(strengthened, lines, 1,
                    Math.PI / 180.0,
                    HOUGH_THRESHOLD,
                    Math.max(60, rgba.rows() * HOUGH_MIN_LINE_LENGTH_RATIO),
                    Math.max(20, rgba.rows() * HOUGH_MAX_LINE_GAP_RATIO));

            List<ReceiptSideEdgeSelector.Edge> candidates = new ArrayList<>();
            for (int r = 0; r < lines.rows(); r++) {
                double[] v = lines.get(r, 0);
                if (v == null || v.length < 4) continue;
                ReceiptSideEdgeSelector.Edge e =
                        new ReceiptSideEdgeSelector.Edge(v[0], v[1], v[2], v[3]);
                if (e.angleFromVerticalDegrees() <= 28) candidates.add(e);
            }
            return ReceiptSideEdgeSelector.select(
                    candidates, rgba.cols(), rgba.rows());
        } finally {
            gray.release(); blur.release(); edges.release();
            strengthened.release(); lines.release();
            if (kernel != null) kernel.release();
        }
    }

    private ReceiptPaperEdgeValidator.Result validatePaperEdges(
            Mat analysis, ReceiptSideEdgeSelector.Pair pair) {
        Mat gray = new Mat();
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        try {
            toGray(analysis, gray);
            Core.meanStdDev(gray, mean, std);
            double[] s = std.toArray();
            double globalStd = s.length == 0 ? 0 : s[0];
            return ReceiptPaperEdgeValidator.validate(
                    pair, gray.cols(), gray.rows(), globalStd,
                    (x, y) -> sample(gray, x, y));
        } finally {
            gray.release(); mean.release(); std.release();
        }
    }

    private List<ReceiptCropProposal.Point> normalizeQuad(
            Point[] quad, double w, double h) {
        List<ReceiptCropProposal.Point> result = new ArrayList<>();
        for (Point p : quad) {
            result.add(new ReceiptCropProposal.Point(
                    p.x / Math.max(1, w - 1),
                    p.y / Math.max(1, h - 1)));
        }
        return result;
    }

    private List<ReceiptCropProposal.Point> sidePairCorners(
            ReceiptSideEdgeSelector.Pair p,
            double w, double h, double marginRatio) {
        double lt = p.left.xAt(0), lb = p.left.xAt(h - 1);
        double rt = p.right.xAt(0), rb = p.right.xAt(h - 1);
        double width = Math.max(rt - lt, rb - lb);
        double m = Math.max(4, width * marginRatio);

        List<ReceiptCropProposal.Point> c = new ArrayList<>();
        c.add(new ReceiptCropProposal.Point((lt - m) / w, 0));
        c.add(new ReceiptCropProposal.Point((rt + m) / w, 0));
        c.add(new ReceiptCropProposal.Point((rb + m) / w, 1));
        c.add(new ReceiptCropProposal.Point((lb - m) / w, 1));
        return c;
    }

    private ReceiptBoundaryGeometry.Point[] normalizedToAbsolute(
            List<ReceiptCropProposal.Point> c, double w, double h) {
        ReceiptBoundaryGeometry.Point[] out =
                new ReceiptBoundaryGeometry.Point[4];
        for (int i = 0; i < 4; i++) {
            out[i] = new ReceiptBoundaryGeometry.Point(
                    c.get(i).getX() * (w - 1),
                    c.get(i).getY() * (h - 1));
        }
        return out;
    }

    /**
     * Generic safety validation for the user-confirmed quadrilateral.
     *
     * The threshold is an image-relative validity guard, not a fixed receipt
     * width. It only prevents accidental tiny/self-crossing selections from
     * replacing the OCR input.
     */
    private boolean isValidSelection(
            ReceiptBoundaryGeometry.Point[] c,
            int imageWidth,
            int imageHeight) {

        if (c == null || c.length != 4) {
            return false;
        }

        double area =
                polygonArea(c);

        double imageArea =
                Math.max(
                        1.0,
                        imageWidth
                                * (double) imageHeight);

        double areaRatio =
                area / imageArea;

        MatOfPoint polygon =
                new MatOfPoint(
                        new Point(c[0].x, c[0].y),
                        new Point(c[1].x, c[1].y),
                        new Point(c[2].x, c[2].y),
                        new Point(c[3].x, c[3].y));

        try {
            boolean convex =
                    Imgproc.isContourConvex(
                            polygon);

            if (!convex) {
                Log.i(
                        TAG,
                        "selection-mask invalid: non-convex");
                return false;
            }

        } finally {
            polygon.release();
        }

        /*
         * This guard is intentionally permissive. It rejects only obviously
         * accidental tiny selections; it does not prescribe receipt width.
         */
        if (areaRatio < 0.06) {
            Log.i(
                    TAG,
                    String.format(
                            Locale.US,
                            "selection-mask invalid: areaRatio=%.3f",
                            areaRatio));
            return false;
        }

        Log.i(
                TAG,
                String.format(
                        Locale.US,
                        "selection-mask valid areaRatio=%.3f",
                        areaRatio));

        return true;
    }

    private double polygonArea(
            ReceiptBoundaryGeometry.Point[] c) {

        double sum = 0.0;

        for (int i = 0; i < c.length; i++) {
            ReceiptBoundaryGeometry.Point a =
                    c[i];

            ReceiptBoundaryGeometry.Point b =
                    c[(i + 1) % c.length];

            sum +=
                    a.x * b.y
                            - b.x * a.y;
        }

        return Math.abs(sum) * 0.5;
    }

    private Mat maskOutsideSelection(
            Mat source,
            ReceiptBoundaryGeometry.Point[] c) {

        Mat mask =
                Mat.zeros(
                        source.rows(),
                        source.cols(),
                        CvType.CV_8UC1);

        Mat output =
                new Mat(
                        source.rows(),
                        source.cols(),
                        source.type(),
                        Scalar.all(255));

        MatOfPoint polygon =
                new MatOfPoint(
                        new Point(c[0].x, c[0].y),
                        new Point(c[1].x, c[1].y),
                        new Point(c[2].x, c[2].y),
                        new Point(c[3].x, c[3].y));

        try {
            Imgproc.fillConvexPoly(
                    mask,
                    polygon,
                    Scalar.all(255));

            source.copyTo(
                    output,
                    mask);

            return output;

        } catch (Exception e) {
            output.release();
            throw e;

        } finally {
            polygon.release();
            mask.release();
        }
    }

    private ReceiptCropGeometryPolicy.Decision decideCropPolicy(
            ReceiptBoundaryGeometry.Point[] c) {
        ReceiptCropGeometryPolicy.Point[] points =
                new ReceiptCropGeometryPolicy.Point[4];
        for (int i = 0; i < 4; i++) {
            points[i] = new ReceiptCropGeometryPolicy.Point(c[i].x, c[i].y);
        }
        return ReceiptCropGeometryPolicy.decide(points);
    }

    /**
     * Pixel-preserving crop. Uses the bounding ROI around the confirmed
     * quadrilateral and does not warp, rotate, stretch, or interpolate.
     */
    private Mat safeRoiCrop(Mat source,
                            ReceiptBoundaryGeometry.Point[] c) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (ReceiptBoundaryGeometry.Point p : c) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        int left = clampInt((int) Math.floor(minX), 0, source.cols() - 1);
        int top = clampInt((int) Math.floor(minY), 0, source.rows() - 1);
        int right = clampInt((int) Math.ceil(maxX) + 1, left + 1, source.cols());
        int bottom = clampInt((int) Math.ceil(maxY) + 1, top + 1, source.rows());

        Rect roi = new Rect(left, top, right - left, bottom - top);
        Mat view = new Mat(source, roi);
        try {
            return view.clone();
        } finally {
            view.release();
        }
    }

    private Mat warpReceipt(Mat source,
                            ReceiptBoundaryGeometry.Point[] c) {
        ReceiptBoundaryGeometry.Point tl = c[0], tr = c[1],
                br = c[2], bl = c[3];

        int targetW = (int) Math.round(Math.max(
                ReceiptBoundaryGeometry.distance(tl, tr),
                ReceiptBoundaryGeometry.distance(bl, br)));
        int targetH = (int) Math.round(Math.max(
                ReceiptBoundaryGeometry.distance(tl, bl),
                ReceiptBoundaryGeometry.distance(tr, br)));

        if (targetW < MIN_OUTPUT_EDGE || targetH < MIN_OUTPUT_EDGE) return null;

        MatOfPoint2f src = new MatOfPoint2f(
                new Point(tl.x, tl.y), new Point(tr.x, tr.y),
                new Point(br.x, br.y), new Point(bl.x, bl.y));
        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0, 0), new Point(targetW - 1, 0),
                new Point(targetW - 1, targetH - 1),
                new Point(0, targetH - 1));
        Mat transform = Imgproc.getPerspectiveTransform(src, dst);
        Mat output = new Mat(targetH, targetW, source.type());

        try {
            Imgproc.warpPerspective(source, output, transform,
                    new Size(targetW, targetH),
                    Imgproc.INTER_CUBIC,
                    Core.BORDER_REPLICATE,
                    Scalar.all(255));
            return output;
        } finally {
            src.release(); dst.release(); transform.release();
        }
    }

    private double sample(Mat gray, double x, double y) {
        int cx = (int) Math.round(x), cy = (int) Math.round(y);
        double total = 0; int count = 0;
        for (int yy = Math.max(0, cy - 2);
             yy <= Math.min(gray.rows() - 1, cy + 2); yy++) {
            for (int xx = Math.max(0, cx - 2);
                 xx <= Math.min(gray.cols() - 1, cx + 2); xx++) {
                double[] v = gray.get(yy, xx);
                if (v != null && v.length > 0) {
                    total += v[0]; count++;
                }
            }
        }
        return count == 0 ? 0 : total / count;
    }

    private boolean usable(Mat m) {
        return m != null
                && m.cols() >= MIN_OUTPUT_EDGE
                && m.rows() >= MIN_OUTPUT_EDGE;
    }

    private void toGray(Mat src, Mat gray) {
        if (src.channels() == 4) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);
        } else if (src.channels() == 3) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        } else {
            src.copyTo(gray);
        }
    }

    private File resolveFile(String uriText) {
        if (uriText == null) return null;
        Uri uri = Uri.parse(uriText);
        return "file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null
                ? new File(uri.getPath()) : null;
    }

    private Bitmap decodeWorkingBitmap(File file) throws IOException {
        BitmapFactory.Options b = new BitmapFactory.Options();
        b.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), b);
        if (b.outWidth <= 0 || b.outHeight <= 0) return null;

        int sampleSize = 1;
        int longest = Math.max(b.outWidth, b.outHeight);
        while (longest / sampleSize > MAX_WORKING_LONG_EDGE * 2) {
            sampleSize *= 2;
        }

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = sampleSize;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded = BitmapFactory.decodeFile(file.getAbsolutePath(), o);
        if (decoded == null) return null;

        Bitmap oriented = applyExifOrientation(file, decoded);
        if (oriented != decoded) decoded.recycle();

        int currentLongest = Math.max(oriented.getWidth(), oriented.getHeight());
        if (currentLongest <= MAX_WORKING_LONG_EDGE) return oriented;

        double scale = MAX_WORKING_LONG_EDGE / (double) currentLongest;
        Bitmap scaled = Bitmap.createScaledBitmap(
                oriented,
                Math.max(1, (int) Math.round(oriented.getWidth() * scale)),
                Math.max(1, (int) Math.round(oriented.getHeight() * scale)),
                true);
        if (scaled != oriented) oriented.recycle();
        return scaled;
    }

    private Bitmap applyExifOrientation(File file, Bitmap source)
            throws IOException {
        ExifInterface exif = new ExifInterface(file);
        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        Matrix m = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                m.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                m.setRotate(180); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                m.setScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                m.setRotate(90); m.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                m.setRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                m.setRotate(-90); m.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                m.setRotate(-90); break;
            default:
                return source;
        }

        return Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), m, true);
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Temporary OCR derivatives are lossless PNG to avoid adding JPEG
     * compression artifacts around small digits, decimals and currency signs.
     */
    private String saveWorkingImageLossless(Mat rgba) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(
                rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);

        try {
            Utils.matToBitmap(rgba, bitmap);

            File dir = new File(context.getFilesDir(), "receipt_ocr_work");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Cannot create OCR work directory");
            }

            File output = new File(dir, UUID.randomUUID() + ".png");

            try (FileOutputStream stream = new FileOutputStream(output)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw new IOException("Cannot encode lossless OCR image");
                }
            }

            return Uri.fromFile(output).toString();
        } finally {
            bitmap.recycle();
        }
    }
}
