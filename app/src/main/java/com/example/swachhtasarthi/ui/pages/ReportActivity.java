package com.example.swachhtasarthi.ui.pages;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.swachhtasarthi.BuildConfig;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.imagekit.android.ImageKit;
import com.imagekit.android.ImageKitCallback;
import com.imagekit.android.entity.UploadError;
import com.imagekit.android.entity.UploadResponse;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final int MAX_IMAGES = 3;
    private static final String IMAGEKIT_UPLOAD_FOLDER = "/reports/";
    private static final String IMAGEKIT_UPLOAD_API = "https://upload.imagekit.io/api/v1/files/upload";
    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();

    private EditText etLatitude, etLongitude, etAddress, etCity, etPinCode, etDateOfIssue, etTimeOfIssue, etDescription;
    private MaterialButton btnFetchGPS, btnAutoFillAddress;
    private MaterialButton btnPostCommunity, btnPostIndividual;
    private ImageView btnBack;
    
    private MaterialCardView btnUploadInitial, btnAddMore;
    private LinearLayout layoutThumbnails;
    private FrameLayout[] frames = new FrameLayout[MAX_IMAGES];
    private ImageView[] ivPhotos = new ImageView[MAX_IMAGES];
    private ImageView[] btnRemoves = new ImageView[MAX_IMAGES];
    
    private List<Uri> selectedImages = new ArrayList<>();
    private Uri pendingCameraImageUri;
    
    private FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();
    private FusedLocationProviderClient fusedLocationClient;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final ActivityResultLauncher<String> galleryPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            imageUri -> {
                if (imageUri != null) {
                    addImage(imageUri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && pendingCameraImageUri != null) {
                    addImage(pendingCameraImageUri);
                } else if (pendingCameraImageUri != null) {
                    getContentResolver().delete(pendingCameraImageUri, null, null);
                }
                pendingCameraImageUri = null;
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!firebaseManagerAndAuth.isUserLoggedIn()) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_post);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setCurrentDateTime();
        setupListeners();
        updateImageUI();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnFetchGPS = findViewById(R.id.btnFetchGPS);
        btnAutoFillAddress = findViewById(R.id.btnAutoFillAddress);
        btnPostCommunity = findViewById(R.id.btnPostCommunity);
        btnPostIndividual = findViewById(R.id.btnPostIndividual);

        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        etAddress = findViewById(R.id.etAddress);
        etCity = findViewById(R.id.etCity);
        etPinCode = findViewById(R.id.etPinCode);
        etDateOfIssue = findViewById(R.id.etDateOfIssue);
        etTimeOfIssue = findViewById(R.id.etTimeOfIssue);
        etDescription = findViewById(R.id.etDescription);

        btnUploadInitial = findViewById(R.id.btnUploadInitial);
        btnAddMore = findViewById(R.id.btnAddMore);
        layoutThumbnails = findViewById(R.id.layoutThumbnails);

        frames[0] = findViewById(R.id.framePhoto1);
        frames[1] = findViewById(R.id.framePhoto2);
        frames[2] = findViewById(R.id.framePhoto3);

        ivPhotos[0] = findViewById(R.id.ivPhoto1);
        ivPhotos[1] = findViewById(R.id.ivPhoto2);
        ivPhotos[2] = findViewById(R.id.ivPhoto3);

        btnRemoves[0] = findViewById(R.id.btnRemove1);
        btnRemoves[1] = findViewById(R.id.btnRemove2);
        btnRemoves[2] = findViewById(R.id.btnRemove3);
    }

    private void setCurrentDateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDateOfIssue.setText(dateFormat.format(calendar.getTime()));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        etTimeOfIssue.setText(timeFormat.format(calendar.getTime()));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        View.OnClickListener pickImageClick = v -> {
            if (selectedImages.size() < MAX_IMAGES) {
                showImageSourceChooser();
            } else {
                Toast.makeText(this, "Maximum 3 images allowed", Toast.LENGTH_SHORT).show();
            }
        };

        btnUploadInitial.setOnClickListener(pickImageClick);
        btnAddMore.setOnClickListener(pickImageClick);

        for (int i = 0; i < MAX_IMAGES; i++) {
            int index = i;
            btnRemoves[i].setOnClickListener(v -> removeImage(index));
        }

        btnFetchGPS.setOnClickListener(v -> checkLocationPermissionAndFetch());

        btnAutoFillAddress.setOnClickListener(v -> {
            String latStr = etLatitude.getText().toString();
            String lonStr = etLongitude.getText().toString();
            if (!latStr.isEmpty() && !lonStr.isEmpty()) {
                fetchAddressFromCoords(Double.parseDouble(latStr), Double.parseDouble(lonStr));
            } else {
                Toast.makeText(this, "Get GPS coordinates first", Toast.LENGTH_SHORT).show();
            }
        });

        etDateOfIssue.setOnClickListener(v -> showDatePicker());
        etTimeOfIssue.setOnClickListener(v -> showTimePicker());

        if (btnPostCommunity != null) {
            btnPostCommunity.setOnClickListener(v -> submitReport(true));
        }
        if (btnPostIndividual != null) {
            btnPostIndividual.setOnClickListener(v -> submitReport(false));
        }
    }

    private void submitReport(boolean asCommunity) {
        String uid = firebaseManagerAndAuth.getCurrentUserUid();
        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        String latitude = etLatitude.getText().toString().trim();
        String longitude = etLongitude.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String pinCode = etPinCode.getText().toString().trim();
        String dateOfIssue = etDateOfIssue.getText().toString().trim();
        String timeOfIssue = etTimeOfIssue.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (description.isEmpty() || address.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> imageUris = new ArrayList<>();
        for (Uri uri : selectedImages) {
            if (uri != null) {
                imageUris.add(uri.toString());
            }
        }

        setPostingEnabled(false);

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("postedBy", uid);
        reportData.put("reportByType", asCommunity ? "community" : "individual");
        reportData.put("latitude", latitude);
        reportData.put("longitude", longitude);
        reportData.put("address", address);
        reportData.put("city", city);
        reportData.put("pinCode", pinCode);
        reportData.put("dateOfIssue", dateOfIssue);
        reportData.put("timeOfIssue", timeOfIssue);
        reportData.put("description", description);
        reportData.put("status", "PENDING");
        reportData.put("likes", 0);
        reportData.put("upvoteCount", 0);
        reportData.put("commentCount", 0);
        reportData.put("createdAt", System.currentTimeMillis());

        if (!asCommunity) {
            reportData.put("userId", uid);
            uploadImagesAndSubmitReport(uid, imageUris, reportData);
            return;
        }

        resolveUsersCommunityId(uid, communityId -> {
            if (communityId == null || communityId.trim().isEmpty()) {
                // If user isn't a member/owner of a community, fall back to individual.
                reportData.put("reportByType", "individual");
                reportData.put("userId", uid);
                uploadImagesAndSubmitReport(uid, imageUris, reportData);
                return;
            }

            reportData.put("communityId", communityId);
            // For community reports, store under community owner id for easy aggregation.
            reportData.put("userId", communityId);
            uploadImagesAndSubmitReport(uid, imageUris, reportData);
        });
    }

    private void setPostingEnabled(boolean enabled) {
        if (btnPostCommunity != null) btnPostCommunity.setEnabled(enabled);
        if (btnPostIndividual != null) btnPostIndividual.setEnabled(enabled);
    }

    private interface OnCommunityResolved {
        void onResolved(String communityId);
    }

    private void resolveUsersCommunityId(String uid, OnCommunityResolved cb) {
        // Own community doc takes precedence
        db.collection("community")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        cb.onResolved(uid);
                        return;
                    }

                    db.collectionGroup("members")
                            .whereEqualTo(FieldPath.documentId(), uid)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snaps -> {
                                if (!snaps.isEmpty()) {
                                    String communityId = snaps.getDocuments().get(0).getReference()
                                            .getParent()
                                            .getParent()
                                            .getId();
                                    cb.onResolved(communityId);
                                } else {
                                    cb.onResolved("");
                                }
                            })
                            .addOnFailureListener(e -> cb.onResolved(""));
                })
                .addOnFailureListener(e -> cb.onResolved(""));
    }

    private void uploadImagesAndSubmitReport(String uid, List<String> localImageUris, Map<String, Object> reportData) {
        if (localImageUris == null || localImageUris.isEmpty()) {
            reportData.put("imageUris", new ArrayList<String>());
            submitPreparedReport(reportData);
            return;
        }

        List<String> nonEmptyUris = new ArrayList<>();
        for (String uri : localImageUris) {
            if (uri != null && !uri.trim().isEmpty()) {
                nonEmptyUris.add(uri);
            }
        }

        if (nonEmptyUris.isEmpty()) {
            reportData.put("imageUris", new ArrayList<String>());
            submitPreparedReport(reportData);
            return;
        }

        uploadReportImageAtIndex(uid, nonEmptyUris, 0, new ArrayList<>(), reportData);
    }

    private void uploadReportImageAtIndex(
            String uid,
            List<String> localImageUris,
            int index,
            List<String> uploadedUrls,
            Map<String, Object> reportData
    ) {
        if (index >= localImageUris.size()) {
            reportData.put("imageUris", uploadedUrls);
            if (!uploadedUrls.isEmpty()) {
                reportData.put("imageUrl", uploadedUrls.get(0));
            }
            submitPreparedReport(reportData);
            return;
        }

        Uri imageUri = Uri.parse(localImageUris.get(index));
        String filename = "report_" + uid + "_" + System.currentTimeMillis() + "_" + index;

        uploadToImageKit(imageUri, filename, new ImageKitUploadCallback() {
            @Override
            public void onSuccess(String url) {
                uploadedUrls.add(url);
                uploadReportImageAtIndex(uid, localImageUris, index + 1, uploadedUrls, reportData);
            }

            @Override
            public void onError(String error) {
                setPostingEnabled(true);
                Toast.makeText(ReportActivity.this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
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
            String authHeader = "Basic " + Base64.encodeToString(authRaw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            byte[] imageBytes = readFileBytes(uploadFile);
            String mimeType = "image/jpeg";
            String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            String dataUri = "data:" + mimeType + ";base64," + base64;

            String body = "file=" + URLEncoder.encode(dataUri, "UTF-8")
                    + "&fileName=" + URLEncoder.encode(filename, "UTF-8")
                    + "&folder=" + URLEncoder.encode(IMAGEKIT_UPLOAD_FOLDER, "UTF-8")
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
        FileInputStream fis = new FileInputStream(file);
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

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    private void safeDeleteTempFile(File file) {
        if (file == null) return;
        try {
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private void submitPreparedReport(Map<String, Object> reportData) {
        firebaseManagerAndAuth.submitReport(reportData, task -> {
            setPostingEnabled(true);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Issue Reported Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String message = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Failed to post report: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addImage(Uri uri) {
        if (selectedImages.size() < MAX_IMAGES) {
            selectedImages.add(uri);
            updateImageUI();
        }
    }

    private void showImageSourceChooser() {
        String[] options = {"Take Photo", "Upload from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Add Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndCapture();
                    } else {
                        launchGalleryPicker();
                    }
                })
                .show();
    }

    private void launchGalleryPicker() {
        galleryPickerLauncher.launch("image/*");
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
        if (pendingCameraImageUri == null) {
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
            return;
        }
        cameraCaptureLauncher.launch(pendingCameraImageUri);
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "swachhta_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SwachhtaSarthi");
        }
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void removeImage(int index) {
        if (index < selectedImages.size()) {
            selectedImages.remove(index);
            updateImageUI();
        }
    }

    private void updateImageUI() {
        if (selectedImages.isEmpty()) {
            btnUploadInitial.setVisibility(View.VISIBLE);
            layoutThumbnails.setVisibility(View.GONE);
        } else {
            btnUploadInitial.setVisibility(View.GONE);
            layoutThumbnails.setVisibility(View.VISIBLE);

            for (int i = 0; i < MAX_IMAGES; i++) {
                if (i < selectedImages.size()) {
                    frames[i].setVisibility(View.VISIBLE);
                    ivPhotos[i].setImageURI(selectedImages.get(i));
                } else {
                    frames[i].setVisibility(View.GONE);
                }
            }

            if (selectedImages.size() < MAX_IMAGES) {
                btnAddMore.setVisibility(View.VISIBLE);
            } else {
                btnAddMore.setVisibility(View.GONE);
            }
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etDateOfIssue.setText(format.format(calendar.getTime()));
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Select Time")
                .build();
        timePicker.addOnPositiveButtonClickListener(v -> {
            @SuppressLint("DefaultLocale")
            String time = String.format("%02d:%02d", timePicker.getHour(), timePicker.getMinute());
            etTimeOfIssue.setText(time);
        });
        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                etLatitude.setText(String.valueOf(location.getLatitude()));
                etLongitude.setText(String.valueOf(location.getLongitude()));
                Toast.makeText(this, "GPS Updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to get location. Try turning on GPS.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAddressFromCoords(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                etAddress.setText(addr.getAddressLine(0));
                etCity.setText(addr.getLocality());
                etPinCode.setText(addr.getPostalCode());
                Toast.makeText(this, "Address Filled", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error fetching address", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
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
}
