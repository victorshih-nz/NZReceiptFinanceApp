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
import com.example.nzreceiptapp.presentation.viewmodel.ScannerViewModel;
import com.example.nzreceiptapp.presentation.viewmodel.ScannerUiState;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
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

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processImage(uri);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NzReceiptApplication app = (NzReceiptApplication) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        viewModel = new ViewModelProvider(requireActivity(), factory)
                .get(ScannerViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        binding.btnCapture.setOnClickListener(v -> takePhoto());
        binding.btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        binding.btnReview.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_scanner_to_receiptReview));

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            binding.txtStatus.setText("Status: " + state.getPhase().name());
            boolean loading = state.isLoading();
            boolean awaitingReview = state.canReview();
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnCapture.setEnabled(!loading && !awaitingReview);
            binding.btnGallery.setEnabled(!loading && !awaitingReview);
            binding.spinnerChain.setEnabled(!loading && !awaitingReview);
            binding.editBranch.setEnabled(!loading && !awaitingReview);
            binding.btnReview.setVisibility(awaitingReview ? View.VISIBLE : View.GONE);

            String error = state.getErrorMessage();
            if (error != null && !error.equals(lastShownError)) {
                lastShownError = error;
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(requireContext().getExternalCacheDir(),
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        processImage(Uri.fromFile(photoFile));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    }
                });
    }

    private void processImage(Uri uri) {
        String chainName = binding.spinnerChain.getSelectedItem().toString();
        String branchName = binding.editBranch.getText().toString();
        viewModel.processReceiptImage(uri.toString(), chainName, branchName);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
