package com.example.swachhtasarthi.ui.pages;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.BuildConfig;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.google.android.material.button.MaterialButton;
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

import androidx.annotation.NonNull;

public class ProfileActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText etFirstName, etLastName, etEmail, etDobDay, etDobMonth, etDobYear, etMobile;
    private Spinner spGender;
    private MaterialButton btnSave, btnCancel;
    private ImageView ivBack, ivProfileImage;
    
    private Uri selectedImageUri;
    private String profileImageUrl;

    private static final String IMAGEKIT_UPLOAD_FOLDER = "/profiles/";
    private static final String IMAGEKIT_UPLOAD_API = "https://upload.imagekit.io/api/v1/files/upload";
    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivProfileImage.setImageURI(selectedImageUri);
                    uploadImageToImageKit(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeComponents();
        setupGenderSpinner();
        loadUserData();
        setupClickListeners();
    }

    private void initializeComponents() {
        ivBack = findViewById(R.id.ivBack);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etDobDay = findViewById(R.id.etDobDay);
        etDobMonth = findViewById(R.id.etDobMonth);
        etDobYear = findViewById(R.id.etDobYear);
        etMobile = findViewById(R.id.etMobile);
        spGender = findViewById(R.id.spGender);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupGenderSpinner() {
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);
    }

    private void loadUserData() {
        String uid = firebaseManagerAndAuth.getCurrentUserUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etFirstName.setText(documentSnapshot.getString("firstName"));
                etLastName.setText(documentSnapshot.getString("lastName"));
                etEmail.setText(documentSnapshot.getString("email"));
                etMobile.setText(documentSnapshot.getString("mobile"));
                
                profileImageUrl = documentSnapshot.getString("profileImageUrl");
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this).load(profileImageUrl).placeholder(R.drawable.profile_image).into(ivProfileImage);
                }

                String dob = documentSnapshot.getString("dob");
                if (dob != null && dob.contains("/")) {
                    String[] parts = dob.split("/");
                    if (parts.length == 3) {
                        etDobDay.setText(parts[0]);
                        etDobMonth.setText(parts[1]);
                        etDobYear.setText(parts[2]);
                    }
                }

                String gender = documentSnapshot.getString("gender");
                if (gender != null) {
                    for (int i = 0; i < spGender.getCount(); i++) {
                        if (spGender.getItemAtPosition(i).toString().equalsIgnoreCase(gender)) {
                            spGender.setSelection(i);
                            break;
                        }
                    }
                }
            }
        }).addOnFailureListener(e -> Log.e("ProfileActivity", "Error loading user data", e));
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> saveProfile());

        btnCancel.setOnClickListener(v -> finish());
    }

    private void uploadImageToImageKit(Uri imageUri) {
        // Direct upload fallback if auth endpoint is missing
        if (BuildConfig.IMAGEKIT_PRIVATE_KEY != null && !BuildConfig.IMAGEKIT_PRIVATE_KEY.trim().isEmpty()) {
            directUploadToImageKit(imageUri);
            return;
        }

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        
        String filename = "profile_" + firebaseManagerAndAuth.getCurrentUserUid();
        
        File uploadFile;
        try {
            uploadFile = createTempFileFromUri(imageUri, filename);
        } catch (IOException e) {
            Toast.makeText(this, "Unable to prepare image for upload", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchImageKitToken(new TokenCallback() {
            @Override
            public void onSuccess(String token) {
                ImageKit.Companion.getInstance().uploader().upload(
                        uploadFile,
                        filename,
                        token,
                        false,
                        null,
                        IMAGEKIT_UPLOAD_FOLDER,
                        false,
                        null,
                        null,
                        null,
                        null,
                        true,
                        true,
                        true,
                        true,
                        null,
                        null,
                        null,
                        new ImageKitCallback() {
                            @Override
                            public void onSuccess(@NonNull UploadResponse uploadResponse) {
                                safeDeleteTempFile(uploadFile);
                                profileImageUrl = uploadResponse.getUrl();
                                Toast.makeText(ProfileActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(@NonNull UploadError uploadError) {
                                safeDeleteTempFile(uploadFile);
                                Log.e("ProfileActivity", "Upload failed: " + uploadError.getMessage());
                                Toast.makeText(ProfileActivity.this, "Upload failed: " + uploadError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }

            @Override
            public void onError(String error) {
                safeDeleteTempFile(uploadFile);
                Toast.makeText(ProfileActivity.this, "Auth error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void directUploadToImageKit(Uri uri) {
        String filename = "profile_" + firebaseManagerAndAuth.getCurrentUserUid();
        File uploadFile;
        try {
            uploadFile = createTempFileFromUri(uri, filename);
        } catch (IOException e) {
            Toast.makeText(this, "Unable to prepare image", Toast.LENGTH_SHORT).show();
            return;
        }

        NETWORK_EXECUTOR.execute(() -> {
            try {
                String uploadedUrl = uploadFileToImageKitInternal(uploadFile, filename);
                runOnUiThread(() -> {
                    safeDeleteTempFile(uploadFile);
                    profileImageUrl = uploadedUrl;
                    Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    safeDeleteTempFile(uploadFile);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String uploadFileToImageKitInternal(File uploadFile, String filename) throws IOException {
        HttpURLConnection connection = null;
        try {
            String privateKey = BuildConfig.IMAGEKIT_PRIVATE_KEY;
            String authRaw = privateKey + ":";
            String authHeader = "Basic " + android.util.Base64.encodeToString(authRaw.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);

            byte[] imageBytes = readFileBytes(uploadFile);
            String mimeType = "image/jpeg";
            String base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
            String dataUri = "data:" + mimeType + ";base64," + base64;

            String body = "file=" + java.net.URLEncoder.encode(dataUri, "UTF-8")
                    + "&fileName=" + java.net.URLEncoder.encode(filename, "UTF-8")
                    + "&folder=" + java.net.URLEncoder.encode(IMAGEKIT_UPLOAD_FOLDER, "UTF-8")
                    + "&useUniqueFileName=true";

            URL url = new URL(IMAGEKIT_UPLOAD_API);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(40000);
            connection.setRequestProperty("Authorization", authHeader);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
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

            JSONObject json = new JSONObject(responseBody);
            return json.getString("url");
        } catch (Exception e) {
            throw new IOException(e.getMessage() == null ? "ImageKit upload error" : e.getMessage(), e);
        } finally {
            if (connection != null) connection.disconnect();
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

    private void fetchImageKitToken(TokenCallback callback) {
        final String endpoint = BuildConfig.IMAGEKIT_AUTH_ENDPOINT;
        if (endpoint == null || endpoint.trim().isEmpty()) {
            callback.onError("ImageKit auth endpoint not configured");
            return;
        }
        NETWORK_EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                InputStream responseStream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                String body = readStream(responseStream);
                JSONObject json = new JSONObject(body);
                String token = json.optString("token", "");
                if (token.isEmpty()) {
                    JSONObject data = json.optJSONObject("data");
                    if (data != null) token = data.optString("token", "");
                }
                String finalToken = token;
                runOnUiThread(() -> callback.onSuccess(finalToken));
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError("Auth error: " + e.getMessage()));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) builder.append(line);
        reader.close();
        return builder.toString();
    }

    private void safeDeleteTempFile(File file) {
        if (file != null && file.exists()) file.delete();
    }

    interface TokenCallback {
        void onSuccess(String token);
        void onError(String error);
    }

    private void saveProfile() {
        String uid = firebaseManagerAndAuth.getCurrentUserUid();
        if (uid == null) return;

        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String day = etDobDay.getText().toString().trim();
        String month = etDobMonth.getText().toString().trim();
        String year = etDobYear.getText().toString().trim();
        String gender = spGender.getSelectedItem().toString();

        if (firstName.isEmpty() || lastName.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("mobile", mobile);
        userData.put("dob", day + "/" + month + "/" + year);
        userData.put("gender", gender);
        if (profileImageUrl != null) {
            userData.put("profileImageUrl", profileImageUrl);
        }

        db.collection("users").document(uid).update(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Error updating profile", e);
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                });
    }
}
