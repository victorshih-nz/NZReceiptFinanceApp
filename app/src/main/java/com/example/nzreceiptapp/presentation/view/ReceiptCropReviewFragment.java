package com.example.nzreceiptapp.presentation.view;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.nzreceiptapp.NzReceiptApplication;
import com.example.nzreceiptapp.databinding.FragmentReceiptCropReviewBinding;
import com.example.nzreceiptapp.di.ViewModelFactory;
import com.example.nzreceiptapp.presentation.viewmodel.CropReviewState;
import com.example.nzreceiptapp.presentation.viewmodel.ScannerViewModel;

import java.io.IOException;

/** Manual boundary review shown before OCR. */
public final class ReceiptCropReviewFragment extends Fragment {

    private FragmentReceiptCropReviewBinding binding;
    private ScannerViewModel viewModel;
    private boolean leaving;

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        NzReceiptApplication app =
                (NzReceiptApplication) requireActivity().getApplication();

        viewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelFactory(app.getAppContainer()))
                .get(ScannerViewModel.class);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        cancelAndBack();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle state) {

        binding = FragmentReceiptCropReviewBinding.inflate(
                inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle state) {

        super.onViewCreated(view, state);

        binding.btnUseCrop.setOnClickListener(v -> {
            leaving = true;
            viewModel.confirmCrop(binding.cropOverlay.getCorners());
            Navigation.findNavController(v).popBackStack();
        });

        binding.btnUseOriginal.setOnClickListener(v -> {
            leaving = true;
            viewModel.confirmOriginalImage();
            Navigation.findNavController(v).popBackStack();
        });

        binding.btnResetCrop.setOnClickListener(v -> {
            CropReviewState current =
                    viewModel.getCropReviewState().getValue();

            if (current != null && current.isReady()) {
                binding.cropOverlay.setCorners(
                        current.getSuggestedCorners());
            }
        });

        binding.btnCancelCrop.setOnClickListener(v ->
                cancelAndBack());

        viewModel.getCropReviewState().observe(
                getViewLifecycleOwner(),
                this::render);
    }

    private void render(CropReviewState state) {
        if (state == null || binding == null) return;

        binding.progressCrop.setVisibility(
                state.getPhase() == CropReviewState.Phase.APPLYING
                        ? View.VISIBLE
                        : View.GONE);

        if (state.getPhase() == CropReviewState.Phase.READY) {
            Uri uri = Uri.parse(state.getOriginalImageUri());

            binding.imageReceipt.setImageURI(uri);

            int[] orientedSize = readOrientedImageSize(uri);
            binding.cropOverlay.setImageSize(
                    orientedSize[0],
                    orientedSize[1]);

            binding.cropOverlay.setCorners(
                    state.getSuggestedCorners());

            String label = state.isAutoDetected()
                    ? "Auto: " + state.getDetectionMode()
                    : "Manual review";

            binding.txtCropStatus.setText(
                    label + "\n" + state.getMessage());

        } else if (state.getPhase()
                == CropReviewState.Phase.ERROR) {

            Toast.makeText(
                    requireContext(),
                    state.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * ImageView displays the photo in its EXIF orientation. The crop overlay
     * must use the same oriented width/height, otherwise a portrait image whose
     * encoded bitmap is landscape gets a landscape crop rectangle centered
     * inside it (the MJ2/MJ2.1 top/bottom "locked" bug).
     */
    private int[] readOrientedImageSize(Uri uri) {
        if (!"file".equalsIgnoreCase(uri.getScheme())
                || uri.getPath() == null) {
            return new int[]{1, 1};
        }

        BitmapFactory.Options options =
                new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(
                uri.getPath(),
                options);

        int width = Math.max(1, options.outWidth);
        int height = Math.max(1, options.outHeight);

        try {
            ExifInterface exif =
                    new ExifInterface(uri.getPath());

            int orientation =
                    exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                    || orientation == ExifInterface.ORIENTATION_ROTATE_270
                    || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                    || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {

                int tmp = width;
                width = height;
                height = tmp;
            }
        } catch (IOException ignored) {
            // Bitmap bounds are still usable if EXIF cannot be read.
        }

        return new int[]{width, height};
    }

    private void cancelAndBack() {
        if (leaving) return;

        leaving = true;
        viewModel.cancelCropReview();

        Navigation.findNavController(
                requireView()).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
