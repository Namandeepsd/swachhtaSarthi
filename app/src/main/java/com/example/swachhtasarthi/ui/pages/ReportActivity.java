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

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FieldValue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ReportActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final int MAX_IMAGES = 3;

    public enum ReportStatus {
        PENDING, IN_PROGRESS, RESOLVED
    }

    private EditText etLatitude, etLongitude, etAddress, etCity, etPinCode, etDateOfIssue, etTimeOfIssue, etDescription;
    private MaterialButton btnFetchGPS, btnAutoFillAddress, btnPost;
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
        btnPost = findViewById(R.id.btnPostIssue);

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

        btnPost.setOnClickListener(v -> submitReportToFirestore());
    }

    private void submitReportToFirestore() {
        String description = etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("userId", firebaseManagerAndAuth.getCurrentUserUid());
        reportData.put("latitude", etLatitude.getText().toString());
        reportData.put("longitude", etLongitude.getText().toString());
        reportData.put("address", etAddress.getText().toString());
        reportData.put("city", etCity.getText().toString());
        reportData.put("pinCode", etPinCode.getText().toString());
        reportData.put("dateOfIssue", etDateOfIssue.getText().toString());
        reportData.put("timeOfIssue", etTimeOfIssue.getText().toString());
        reportData.put("description", description);
        reportData.put("status", ReportStatus.PENDING.name());
        reportData.put("createdAt", FieldValue.serverTimestamp());

        // Note: Real apps should upload images to Firebase Storage and store URLs.
        // Storing local URIs as strings will only work on the same device.
        List<String> imageUris = new ArrayList<>();
        for (Uri uri : selectedImages) {
            imageUris.add(uri.toString());
        }
        reportData.put("imageUris", imageUris);

        btnPost.setEnabled(false);
        firebaseManagerAndAuth.submitReport(reportData, task -> {
            btnPost.setEnabled(true);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Issue Reported Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to report issue: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
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
}
