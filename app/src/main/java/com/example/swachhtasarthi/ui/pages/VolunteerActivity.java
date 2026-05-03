package com.example.swachhtasarthi.ui.pages;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.swachhtasarthi.BuildConfig;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.AppNotificationService;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.imagekit.android.ImageKit;
import com.imagekit.android.ImageKitCallback;
import com.imagekit.android.entity.UploadError;
import com.imagekit.android.entity.UploadResponse;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VolunteerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1003;
    private static final String IMAGEKIT_UPLOAD_FOLDER = "/resolved_reports/";
    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();

    private String reportId, reportOwnerId, city, address, description;
    private final FirebaseManagerAndAuth auth = new FirebaseManagerAndAuth();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AppNotificationService notificationService = new AppNotificationService();

    private MaterialCardView btnCaptureProof;
    private LinearLayout layoutCapturePlaceholder;
    private ImageView ivProofPreview;
    private ProgressBar progressBar;
    private MaterialButton btnContributeCommunity, btnContributeIndividual;

    private Uri proofUri;
    private Uri pendingCameraImageUri;
    private String proofUrl = "";

    private final ActivityResultLauncher<Uri> cameraCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && pendingCameraImageUri != null) {
                    proofUri = pendingCameraImageUri;
                    ivProofPreview.setImageURI(pendingCameraImageUri);
                    ivProofPreview.setVisibility(View.VISIBLE);
                    layoutCapturePlaceholder.setVisibility(View.GONE);
                }
                pendingCameraImageUri = null;
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer);

        reportId = getIntent().getStringExtra("reportId");
        reportOwnerId = getIntent().getStringExtra("reportOwnerId");
        city = getIntent().getStringExtra("city");
        address = getIntent().getStringExtra("address");
        description = getIntent().getStringExtra("description");

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvAddress = findViewById(R.id.tvAddress);
        btnContributeCommunity = findViewById(R.id.btnContributeCommunity);
        btnContributeIndividual = findViewById(R.id.btnContributeIndividual);

        btnCaptureProof = findViewById(R.id.btnCaptureProof);
        layoutCapturePlaceholder = findViewById(R.id.layoutCapturePlaceholder);
        ivProofPreview = findViewById(R.id.ivProofPreview);
        progressBar = findViewById(R.id.progressBar);

        tvTitle.setText(city != null ? city : "Report Details");
        tvDescription.setText(description != null ? description : "No description provided");
        tvAddress.setText(address != null ? address : "No address provided");
    }

    private void setupClickListeners() {
        btnCaptureProof.setOnClickListener(v -> checkCameraPermissionAndCapture());
        btnContributeCommunity.setOnClickListener(v -> startResolutionProcess(true));
        btnContributeIndividual.setOnClickListener(v -> startResolutionProcess(false));
    }

    private void checkCameraPermissionAndCapture() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            launchCameraCapture();
        }
    }

    private void launchCameraCapture() {
        pendingCameraImageUri = createImageUri();
        if (pendingCameraImageUri != null) {
            cameraCaptureLauncher.launch(pendingCameraImageUri);
        }
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "proof_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SwachhtaSarthi");
        }
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void startResolutionProcess(boolean contributeInCommunity) {
        if (proofUri == null) {
            Toast.makeText(this, "Please capture a photo of the cleaned area as proof", Toast.LENGTH_SHORT).show();
            return;
        }

        btnContributeCommunity.setEnabled(false);
        btnContributeIndividual.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        uploadToImageKit(proofUri, "proof_" + reportId + "_" + auth.getCurrentUserUid(), new ImageKitUploadCallback() {
            @Override
            public void onSuccess(String url) {
                proofUrl = url;
                markReportResolved(contributeInCommunity);
            }

            @Override
            public void onError(String error) {
                handleError(error, contributeInCommunity);
            }
        });
    }

    private void uploadToImageKit(Uri uri, String filename, ImageKitUploadCallback callback) {
        // No-backend direct upload mode using private key from BuildConfig.
        if (BuildConfig.IMAGEKIT_PRIVATE_KEY == null || BuildConfig.IMAGEKIT_PRIVATE_KEY.trim().isEmpty()) {
            callback.onError("ImageKit private key not configured");
            return;
        }

        File uploadFile;
        try {
            uploadFile = createTempFileFromUri(uri, filename);
        } catch (IOException e) {
            callback.onError("Unable to prepare image for upload");
            return;
        }

        NETWORK_EXECUTOR.execute(() -> {
            try {
                String uploadedUrl = uploadFileToImageKit(uploadFile, filename);
                runOnUiThread(() -> {
                    safeDeleteTempFile(uploadFile);
                    callback.onSuccess(uploadedUrl);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    safeDeleteTempFile(uploadFile);
                    callback.onError(e.getMessage() == null ? "Upload failed" : e.getMessage());
                });
            }
        });
    }

    private String uploadFileToImageKit(File uploadFile, String filename) throws IOException {
        HttpURLConnection connection = null;
        try {
            String privateKey = BuildConfig.IMAGEKIT_PRIVATE_KEY;
            String authRaw = privateKey + ":";
            String authHeader = "Basic " + android.util.Base64.encodeToString(authRaw.getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);

            byte[] imageBytes = readFileBytes(uploadFile);
            String mimeType = "image/jpeg";
            String base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
            String dataUri = "data:" + mimeType + ";base64," + base64;

            String body = "file=" + java.net.URLEncoder.encode(dataUri, "UTF-8")
                    + "&fileName=" + java.net.URLEncoder.encode(filename, "UTF-8")
                    + "&folder=" + java.net.URLEncoder.encode(IMAGEKIT_UPLOAD_FOLDER, "UTF-8")
                    + "&useUniqueFileName=true";

            URL url = new URL("https://upload.imagekit.io/api/v1/files/upload");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(40000);
            connection.setRequestProperty("Authorization", authHeader);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            connection.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            connection.getOutputStream().flush();
            connection.getOutputStream().close();

            int code = connection.getResponseCode();
            InputStream responseStream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String responseBody = readStream(responseStream);
            if (code < 200 || code >= 300) {
                throw new IOException("ImageKit upload failed: " + responseBody);
            }

            org.json.JSONObject json = new org.json.JSONObject(responseBody);
            String urlText = json.optString("url", "");
            if (urlText == null || urlText.trim().isEmpty()) {
                throw new IOException("ImageKit response missing URL");
            }
            return urlText;
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e.getMessage() == null ? "ImageKit upload error" : e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readFileBytes(File file) throws IOException {
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        try {
            byte[] buffer = new byte[8 * 1024];
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            int read;
            while ((read = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        } finally {
            fis.close();
        }
    }

    private void markReportResolved(boolean contributeInCommunity) {
        String volunteerUid = auth.getCurrentUserUid();
        if (volunteerUid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "RESOLVED");
        updates.put("resolvedBy", volunteerUid);
        updates.put("resolvedAt", FieldValue.serverTimestamp());
        updates.put("proofImageUrl", proofUrl);

        db.collection("reports").document(reportId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (contributeInCommunity) {
                        resolveUsersCommunityId(volunteerUid, communityId -> {
                            createVolunteerAction(volunteerUid, "community", communityId);
                            notifyCompletion(volunteerUid);
                            completeActivity();
                        });
                    } else {
                        createVolunteerAction(volunteerUid, "individual", "");
                        notifyCompletion(volunteerUid);
                        completeActivity();
                    }
                })
                .addOnFailureListener(e -> handleError(e.getMessage(), contributeInCommunity));
    }

    private void completeActivity() {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "Great job! Issue resolved successfully.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void handleError(String error, boolean contributeInCommunity) {
        progressBar.setVisibility(View.GONE);
        btnContributeCommunity.setEnabled(true);
        btnContributeIndividual.setEnabled(true);
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
    }

    private void createVolunteerAction(String volunteerUid, String contributionType, String communityId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", volunteerUid);
        data.put("reportId", reportId);
        data.put("reportOwnerId", reportOwnerId == null ? "" : reportOwnerId);
        data.put("city", city == null ? "" : city);
        data.put("address", address == null ? "" : address);
        data.put("contributionType", contributionType);
        data.put("proofImageUrl", proofUrl);
        if (communityId != null && !communityId.trim().isEmpty()) {
            data.put("communityId", communityId.trim());
        }
        data.put("createdAt", FieldValue.serverTimestamp());
        db.collection("volunteerActions").add(data);
    }

    private void resolveUsersCommunityId(String uid, OnCommunityResolved cb) {
        db.collection("community").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                cb.onResolved(uid);
            } else {
                db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
                    String joinedCommId = userDoc.getString("joinedCommunityId");
                    cb.onResolved(joinedCommId != null ? joinedCommId : "");
                }).addOnFailureListener(e -> cb.onResolved(""));
            }
        }).addOnFailureListener(e -> cb.onResolved(""));
    }

    private void notifyCompletion(String volunteerUid) {
        Map<String, Object> common = new HashMap<>();
        common.put("reportId", reportId == null ? "" : reportId);
        common.put("city", city == null ? "" : city);

        if (reportOwnerId != null && !reportOwnerId.equals(volunteerUid)) {
            notificationService.sendToUser(
                    reportOwnerId,
                    "report_completed_by_other",
                    "Issue Resolved!",
                    "A volunteer has cleaned and resolved your reported issue in " + city + ".",
                    "Verified with proof of work.",
                    common
            );
        }

        notificationService.sendToUser(
                volunteerUid,
                "report_completed",
                "Work Submitted",
                "Your proof of work has been uploaded and the report is marked as resolved.",
                "Thank you for your contribution!",
                common
        );
    }

    private interface OnCommunityResolved {
        void onResolved(String communityId);
    }

    private File createTempFileFromUri(Uri uri, String baseName) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = new File(getCacheDir(), baseName + "_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();
        outputStream.close();
        return tempFile;
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) builder.append(line);
        reader.close();
        return builder.toString();
    }

    private void safeDeleteTempFile(File file) {
        if (file != null && file.exists()) file.delete();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCameraCapture();
        }
    }

    interface ImageKitUploadCallback {
        void onSuccess(String url);
        void onError(String error);
    }
}
