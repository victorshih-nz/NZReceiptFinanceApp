package com.example.nzreceiptapp.presentation.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.nzreceiptapp.R;
import com.example.nzreceiptapp.NzReceiptApplication;
import com.example.nzreceiptapp.databinding.FragmentScannerBinding;
import com.example.nzreceiptapp.di.ViewModelFactory;
import com.example.nzreceiptapp.presentation.viewmodel.CropReviewState;
import com.example.nzreceiptapp.presentation.viewmodel.ScannerViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ScannerFragment extends Fragment {
    private static final String TAG = "ScannerFragment";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    private FragmentScannerBinding binding;
    private ScannerViewModel viewModel;
    private ImageCapture imageCapture;
    private String lastShownError;
    private boolean receiptReviewOpened;
    private boolean cropReviewOpened;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) startCamera();
                        else Toast.makeText(getContext(),
                                "Camera permission denied", Toast.LENGTH_SHORT).show();
                    });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> { if (uri != null) processImage(uri); });

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        NzReceiptApplication app =
                (NzReceiptApplication) requireActivity().getApplication();
        viewModel = new ViewModelProvider(requireActivity(),
                new ViewModelFactory(app.getAppContainer()))
                .get(ScannerViewModel.class);
    }

    @Nullable @Override public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle state) {
        binding = FragmentScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(
            @NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);

        if (allPermissionsGranted()) startCamera();
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA);

        binding.btnCapture.setOnClickListener(v -> takePhoto());
        binding.btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        binding.btnTestSample.setOnClickListener(v -> loadSampleFromAssets());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getCropReviewState().observe(
                getViewLifecycleOwner(), crop -> {
                    if (crop == null) return;
                    if (crop.isReady() && !cropReviewOpened) {
                        cropReviewOpened = true;
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_scanner_to_cropReview);
                    } else if (crop.getPhase() == CropReviewState.Phase.IDLE) {
                        cropReviewOpened = false;
                    }
                    updateControls();
                });

        viewModel.getUiState().observe(
                getViewLifecycleOwner(), state -> {
                    if (state == null) return;
                    binding.txtStatus.setText("Status: " + state.getPhase().name());

                    if (state.canReview() && !receiptReviewOpened) {
                        receiptReviewOpened = true;
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_scanner_to_receiptReview);
                    } else if (!state.canReview()) {
                        receiptReviewOpened = false;
                    }

                    String error = state.getErrorMessage();
                    if (error != null && !error.equals(lastShownError)) {
                        lastShownError = error;
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    }
                    updateControls();
                });
    }

    private void updateControls() {
        if (binding == null) return;
        boolean loading = viewModel.getUiState().getValue() != null
                && viewModel.getUiState().getValue().isLoading();
        boolean receiptReview = viewModel.getUiState().getValue() != null
                && viewModel.getUiState().getValue().canReview();
        CropReviewState crop = viewModel.getCropReviewState().getValue();
        boolean cropBusy = crop != null && crop.getPhase() != CropReviewState.Phase.IDLE;

        boolean enabled = !loading && !receiptReview && !cropBusy;
        binding.btnCapture.setEnabled(enabled);
        binding.btnGallery.setEnabled(enabled);
        binding.btnTestSample.setEnabled(enabled);
        binding.spinnerChain.setEnabled(enabled);
        binding.editBranch.setEnabled(enabled);
        binding.btnReview.setVisibility(receiptReview ? View.VISIBLE : View.GONE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();

                provider.unbindAll();
                provider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File file = new File(requireContext().getExternalCacheDir(),
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                        .format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(options,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults results) {
                        processImage(Uri.fromFile(file));
                    }

                    @Override public void onError(
                            @NonNull ImageCaptureException e) {
                        Log.e(TAG, "Photo capture failed: " + e.getMessage(), e);
                    }
                });
    }

    private void processImage(Uri uri) {
        viewModel.processReceiptImage(
                uri.toString(),
                binding.spinnerChain.getSelectedItem().toString(),
                binding.editBranch.getText().toString());
    }

    private void loadSampleFromAssets() {
        try {
            InputStream is = requireContext().getAssets()
                    .open("sample_woolworths.jpg");
            File file = new File(requireContext().getExternalCacheDir(),
                    "sample_woolworths.jpg");
            FileOutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
            os.close();
            is.close();
            processImage(Uri.fromFile(file));
        } catch (Exception e) {
            Log.e(TAG, "Failed to load sample", e);
            Toast.makeText(getContext(),
                    "Failed to load sample: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
