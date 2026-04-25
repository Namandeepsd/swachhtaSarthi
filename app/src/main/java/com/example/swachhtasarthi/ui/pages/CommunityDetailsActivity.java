package com.example.swachhtasarthi.ui.pages;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.swachhtasarthi.BuildConfig;
import com.example.swachhtasarthi.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityDetailsActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final String IMAGEKIT_UPLOAD_FOLDER = "/community/";
    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();

    private EditText etCommName, etCommEmail, etCommDesc;
    private ImageView ivProfilePreview, ivBannerPreview;
    private MaterialCardView btnPickProfile, btnPickBanner;
    private MaterialButton btnSaveCommunity;
    private ProgressBar progressBar;

    private Uri profileUri, bannerUri;
    private Uri pendingCameraImageUri;
    private boolean isPickingProfile = true;
    
    private String profileUrl = "", bannerUrl = "";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid;

    private final ActivityResultLauncher<String> galleryPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    if (isPickingProfile) {
                        profileUri = uri;
                        ivProfilePreview.setImageURI(uri);
                    } else {
                        bannerUri = uri;
                        ivBannerPreview.setImageURI(uri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && pendingCameraImageUri != null) {
                    if (isPickingProfile) {
                        profileUri = pendingCameraImageUri;
                        ivProfilePreview.setImageURI(pendingCameraImageUri);
                    } else {
                        bannerUri = pendingCameraImageUri;
                        ivBannerPreview.setImageURI(pendingCameraImageUri);
                    }
                }
                pendingCameraImageUri = null;
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_details);

        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etCommName = findViewById(R.id.etCommName);
        etCommEmail = findViewById(R.id.etCommEmail);
        etCommDesc = findViewById(R.id.etCommDesc);
        ivProfilePreview = findViewById(R.id.ivProfilePreview);
        ivBannerPreview = findViewById(R.id.ivBannerPreview);
        btnPickProfile = findViewById(R.id.btnPickProfile);
        btnPickBanner = findViewById(R.id.btnPickBanner);
        btnSaveCommunity = findViewById(R.id.btnSaveCommunity);
        progressBar = findViewById(R.id.progressBar);

        btnPickProfile.setOnClickListener(v -> {
            isPickingProfile = true;
            showImageSourceChooser();
        });
        
        btnPickBanner.setOnClickListener(v -> {
            isPickingProfile = false;
            showImageSourceChooser();
        });

        btnSaveCommunity.setOnClickListener(v -> startUploadProcess());
    }

    private void showImageSourceChooser() {
        String[] options = {"Take Photo", "Upload from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndCapture();
                    } else {
                        galleryPickerLauncher.launch("image/*");
                    }
                })
                .show();
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
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "community_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SwachhtaSarthi");
        }
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void startUploadProcess() {
        String name = etCommName.getText().toString().trim();
        String email = etCommEmail.getText().toString().trim();
        String desc = etCommDesc.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (profileUri == null || bannerUri == null) {
            Toast.makeText(this, "Please select both profile and banner images", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveCommunity.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Upload Profile Image First
        uploadToImageKit(profileUri, "profile_" + uid, new ImageKitUploadCallback() {
            @Override
            public void onSuccess(String url) {
                profileUrl = url;
                // Then Upload Banner Image
                uploadToImageKit(bannerUri, "banner_" + uid, new ImageKitUploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        bannerUrl = url;
                        saveToFirestore(name, email, desc);
                    }

                    @Override
                    public void onError(String error) {
                        handleError(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                handleError(error);
            }
        });
    }

    private void uploadToImageKit(Uri uri, String filename, ImageKitUploadCallback callback) {
        // Fallback mode: allow app flow to work even if backend token endpoint is not configured.
        // In this mode we save local content Uri instead of uploading to ImageKit.
        if (BuildConfig.IMAGEKIT_AUTH_ENDPOINT == null || BuildConfig.IMAGEKIT_AUTH_ENDPOINT.trim().isEmpty()) {
            callback.onSuccess(uri.toString());
            return;
        }

        File uploadFile;
        try {
            uploadFile = createTempFileFromUri(uri, filename);
        } catch (IOException e) {
            callback.onError("Unable to prepare image for upload");
            return;
        }

        fetchImageKitToken(new TokenCallback() {
            @Override
            public void onSuccess(String token) {
                ImageKit.Companion.getInstance().uploader().upload(
                        uploadFile,
                        filename,
                        token,
                        false, // useUniqueFilename
                        null, // tags
                        IMAGEKIT_UPLOAD_FOLDER,
                        false, // isPrivateFile
                        null, // customCoordinates
                        null, // responseFields
                        null, // extensions
                        null, // webhookUrl
                        true, // overwriteFile
                        true, // overwriteAITags
                        true, // overwriteTags
                        true, // overwriteCustomMetadata
                        null, // customMetadata
                        null, // policy
                        null, // preprocessor
                        new ImageKitCallback() {
                            @Override
                            public void onSuccess(@NonNull UploadResponse uploadResponse) {
                                safeDeleteTempFile(uploadFile);
                                callback.onSuccess(uploadResponse.getUrl());
                            }

                            @Override
                            public void onError(@NonNull UploadError uploadError) {
                                safeDeleteTempFile(uploadFile);
                                callback.onError(uploadError.getMessage());
                            }
                        }
                );
            }

            @Override
            public void onError(String error) {
                safeDeleteTempFile(uploadFile);
                callback.onError(error);
            }
        });
    }

    private void fetchImageKitToken(TokenCallback callback) {
        final String endpoint = BuildConfig.IMAGEKIT_AUTH_ENDPOINT;
        if (endpoint == null || endpoint.trim().isEmpty()) {
            callback.onError("ImageKit auth endpoint not configured");
            return;
        }
        if (endpoint.contains("ik.imagekit.io")) {
            callback.onError("This is ImageKit URL endpoint, not auth endpoint. Use your backend /imagekit/auth API.");
            return;
        }

        NETWORK_EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int code = connection.getResponseCode();
                InputStream responseStream = code >= 200 && code < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String body = readStream(responseStream);
                if (code < 200 || code >= 300) {
                    runOnUiThread(() -> callback.onError("Auth failed: " + body));
                    return;
                }

                JSONObject json = new JSONObject(body);
                String token = json.optString("token", "");
                if (token.isEmpty()) {
                    JSONObject data = json.optJSONObject("data");
                    if (data != null) {
                        token = data.optString("token", "");
                    }
                }

                if (token.isEmpty()) {
                    runOnUiThread(() -> callback.onError("Auth token missing in response"));
                    return;
                }

                String finalToken = token;
                runOnUiThread(() -> callback.onSuccess(finalToken));
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError("Auth error: " + e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private File createTempFileFromUri(Uri uri, String baseName) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Cannot open input stream");
        }

        File tempFile = new File(getCacheDir(), baseName + "_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream outputStream = new FileOutputStream(tempFile);

        try {
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
            try {
                outputStream.close();
            } catch (IOException ignored) {
            }
        }

        return tempFile;
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }
        return builder.toString();
    }

    private void safeDeleteTempFile(File file) {
        if (file != null && file.exists()) {
            // no-op if deletion fails
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private void saveToFirestore(String name, String email, String desc) {
        Map<String, Object> community = new HashMap<>();
        community.put("name", name);
        community.put("email", email);
        community.put("description", desc);
        community.put("profileImage", profileUrl);
        community.put("channelImage", bannerUrl);
        community.put("createdBy", uid);

        db.collection("community")
                .document(uid)
                .set(community)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Community Created Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void handleError(String error) {
        progressBar.setVisibility(View.GONE);
        btnSaveCommunity.setEnabled(true);
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraCapture();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    interface ImageKitUploadCallback {
        void onSuccess(String url);
        void onError(String error);
    }

    interface TokenCallback {
        void onSuccess(String token);
        void onError(String error);
    }
}
