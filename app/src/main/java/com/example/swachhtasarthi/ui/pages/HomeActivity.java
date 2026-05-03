package com.example.swachhtasarthi.ui.pages;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.model.MyReports;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Collections;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;

    FirebaseManagerAndAuth authManager = new FirebaseManagerAndAuth();
    RecyclerView rvReports;
    private View btnReport;
    private MapView mapView;
    private View btnZoomIn;
    private View btnZoomOut;
    private View btnMyLocation;
    private TextView tvNearestDistance;
    private TextView tvUserName;
    private TextView tvUserLocation;
    private ImageView ivUserProfile;
    private TextView tvScoreValue;
    private TextView tvReportsCount;
    private TextView tvResolvedCount;
    private TextView tvPendingCount;
    private TextView tvVolunteeringCount;
    private TextView tvViewAll;
    private ImageView ivNotification;
    private View notificationBadge;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker userLocationMarker;
    private GeoPoint currentUserPoint;
    private final List<Marker> reportMarkers = new ArrayList<>();
    private final List<GeoPoint> pendingReportPoints = new ArrayList<>();

    private MyReportsAdapter myReportsAdapter;
    private final List<MyReports> myReportsList = new ArrayList<>();
    private boolean showAllReports = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!authManager.isUserLoggedIn()) {
            Intent intent = new Intent(HomeActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        btnReport = findViewById(R.id.btnReport);
        rvReports = findViewById(R.id.rvReports);
        mapView = findViewById(R.id.mapView);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        tvNearestDistance = findViewById(R.id.tvNearestDistance);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserLocation = findViewById(R.id.tvUserLocation);
        ivUserProfile = findViewById(R.id.ivUserProfile);
        tvScoreValue = findViewById(R.id.tvScoreValue);
        tvReportsCount = findViewById(R.id.tvReportsCount);
        tvResolvedCount = findViewById(R.id.tvResolvedCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvVolunteeringCount = findViewById(R.id.tvVolunteeringCount);
        tvViewAll = findViewById(R.id.tvViewAll);
        ivNotification = findViewById(R.id.ivNotification);
        notificationBadge = findViewById(R.id.notificationBadge);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);

        bottomTrayHandler.setup();
        setupOpenStreetMap();
        setupMapControls();
        loadLoggedInUserInNavbar();
        requestLocationAndShowOnMap();
        fetchPendingReportsForMap();
        checkImpendingNotifications();
        requestNotificationPermission();

        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        if (btnReport != null) {
            btnReport.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ReportActivity.class);
                startActivity(intent);
            });
        }

        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                showAllReports = !showAllReports;
                tvViewAll.setText(showAllReports ? "Show less" : "View all");
                fetchMyReports();
            });
        }

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        myReportsAdapter = new MyReportsAdapter(myReportsList);
        rvReports.setAdapter(myReportsAdapter);

        fetchMyReports();
        fetchUserStats();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void checkImpendingNotifications() {
        String uid = authManager.getCurrentUserUid();
        if (uid == null || notificationBadge == null) return;

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null && !snapshots.isEmpty()) {
                        notificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void fetchUserStats() {
        String uid = authManager.getCurrentUserUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch user doc for score
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        long score = 0;
                        Object val = doc.get("rewardScoreCurrent");
                        if (val instanceof Number) score = ((Number) val).longValue();
                        if (tvScoreValue != null) tvScoreValue.setText(String.valueOf(score));
                    }
                });

        // Fetch reports for counts
        db.collection("reports")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snaps -> {
                    int total = snaps.size();
                    int resolved = 0;
                    int pending = 0;
                    for (QueryDocumentSnapshot doc : snaps) {
                        String status = doc.getString("status");
                        if ("RESOLVED".equalsIgnoreCase(status)) resolved++;
                        else if ("PENDING".equalsIgnoreCase(status)) pending++;
                    }
                    if (tvReportsCount != null) tvReportsCount.setText(total + " Reports");
                    if (tvResolvedCount != null) tvResolvedCount.setText(resolved + " Resolved");
                    if (tvPendingCount != null) tvPendingCount.setText(pending + " Pending");
                });

        // Fetch volunteer actions
        db.collection("volunteerActions")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (tvVolunteeringCount != null) tvVolunteeringCount.setText(snaps.size() + " Volunt.");
                });
    }

    private void fetchMyReports() {
        String uid = authManager.getCurrentUserUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("reports")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myReportsList.clear();
                    List<ReportRow> rows = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        long createdAt = toLong(doc.get("createdAt"));
                        String description = valueOrFallback(doc.getString("description"), "No description");
                        String address = valueOrFallback(doc.getString("address"), doc.getString("location"));
                        String city = valueOrFallback(doc.getString("city"), doc.getString("title"));
                        String date = valueOrFallback(doc.getString("dateOfIssue"), "Date unavailable");
                        String status = valueOrFallback(doc.getString("status"), "PENDING");

                        String firstImage = extractFirstImage(doc);
                        String title = valueOrFallback(city, "Report");
                        String location = valueOrFallback(address, "Unknown Location");

                        rows.add(new ReportRow(createdAt, new MyReports(title, description, location, date, status, firstImage)));
                    }

                    Collections.sort(rows, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                    int max = showAllReports ? rows.size() : Math.min(3, rows.size());
                    for (int i = 0; i < max; i++) {
                        myReportsList.add(rows.get(i).report);
                    }
                    myReportsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching reports: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static class ReportRow {
        final long createdAt;
        final MyReports report;

        ReportRow(long createdAt, MyReports report) {
            this.createdAt = createdAt;
            this.report = report;
        }
    }

    private String valueOrFallback(String value, String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value : fallback;
    }

    private String extractFirstImage(QueryDocumentSnapshot doc) {
        Object imageUrisObj = doc.get("imageUris");
        if (imageUrisObj instanceof List) {
            List<?> images = (List<?>) imageUrisObj;
            if (!images.isEmpty() && images.get(0) != null) {
                return String.valueOf(images.get(0));
            }
        }

        String imageUrl = doc.getString("imageUrl");
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl;
        }

        Object imagesObj = doc.get("images");
        if (imagesObj instanceof List) {
            List<?> images = (List<?>) imagesObj;
            if (!images.isEmpty() && images.get(0) instanceof Map) {
                Object url = ((Map<?, ?>) images.get(0)).get("url");
                if (url != null) {
                    return String.valueOf(url);
                }
            }
        }

        return "";
    }

    private void setupOpenStreetMap() {
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getController().setZoom(15.0);
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        GeoPoint defaultPoint = new GeoPoint(28.6139, 77.2090);
        mapView.getController().setCenter(defaultPoint);
    }

    private void setupMapControls() {
        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(v -> {
                if (mapView != null) {
                    mapView.getController().zoomIn();
                }
            });
        }

        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(v -> {
                if (mapView != null) {
                    mapView.getController().zoomOut();
                }
            });
        }

        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(v -> centerOnCurrentUser());
        }
    }

    private void centerOnCurrentUser() {
        if (currentUserPoint != null && mapView != null) {
            mapView.getController().animateTo(currentUserPoint);
            return;
        }
        requestLocationAndShowOnMap();
    }

    private void loadLoggedInUserInNavbar() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        if (tvUserName != null) tvUserName.setText("User");

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String city = documentSnapshot.getString("city");
                    String profileUrl = documentSnapshot.getString("profileImageUrl");

                    if ((firstName != null && !firstName.isEmpty()) || (lastName != null && !lastName.isEmpty())) {
                        String fullName = "";
                        if (firstName != null && !firstName.isEmpty()) {
                            fullName = firstName;
                        }
                        if (lastName != null && !lastName.isEmpty()) {
                            fullName = fullName.isEmpty() ? lastName : fullName + " " + lastName;
                        }
                        if (tvUserName != null) tvUserName.setText(fullName);
                    }

                    if (city != null && !city.isEmpty() && tvUserLocation != null) {
                        tvUserLocation.setText(city);
                    }

                    if (profileUrl != null && !profileUrl.isEmpty() && ivUserProfile != null) {
                        Glide.with(this)
                                .load(profileUrl)
                                .placeholder(R.drawable.profile_image)
                                .circleCrop()
                                .into(ivUserProfile);
                    }
                });
    }

    private void requestLocationAndShowOnMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location == null) {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                return;
            }

            GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            currentUserPoint = userPoint;
            mapView.getController().animateTo(userPoint);

            if (userLocationMarker == null) {
                userLocationMarker = new Marker(mapView);
                userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                userLocationMarker.setTitle("You are here");
                mapView.getOverlays().add(userLocationMarker);
            }

            userLocationMarker.setPosition(userPoint);
            mapView.invalidate();
            updateNearestDistanceBadge();

            updateNavbarCity(location.getLatitude(), location.getLongitude());
        });
    }

    private void fetchPendingReportsForMap() {
        String currentUid = authManager.getCurrentUserUid();
        if (currentUid == null) return;

        FirebaseFirestore.getInstance().collection("reports")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    clearReportMarkers();
                    pendingReportPoints.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = toDouble(doc.get("latitude"));
                        Double lon = toDouble(doc.get("longitude"));
                        if (lat == null || lon == null) {
                            continue;
                        }

                        String reportOwnerId = valueOrFallback(doc.getString("userId"), "");
                        boolean isCurrentUsersReport = currentUid.equals(reportOwnerId);

                        String city = valueOrFallback(doc.getString("city"), "Reported Location");
                        String address = valueOrFallback(doc.getString("address"), "No address");
                        String description = valueOrFallback(doc.getString("description"), "No description");

                        Marker marker = new Marker(mapView);
                        GeoPoint reportPoint = new GeoPoint(lat, lon);
                        marker.setPosition(reportPoint);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                        marker.setTitle(city);
                        marker.setSubDescription(address);

                        Drawable pin = ContextCompat.getDrawable(
                                this,
                                isCurrentUsersReport ? R.drawable.map_pin_my_report : R.drawable.map_pin_other_report
                        );
                        if (pin != null) {
                            marker.setIcon(pin);
                        }

                        String reportId = doc.getId();
                        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                            Intent volunteerIntent = new Intent(HomeActivity.this, VolunteerActivity.class);
                            volunteerIntent.putExtra("reportId", reportId);
                            volunteerIntent.putExtra("reportOwnerId", reportOwnerId);
                            volunteerIntent.putExtra("city", city);
                            volunteerIntent.putExtra("address", address);
                            volunteerIntent.putExtra("description", description);
                            startActivity(volunteerIntent);
                            return true;
                        });

                        reportMarkers.add(marker);
                        pendingReportPoints.add(reportPoint);
                        mapView.getOverlays().add(marker);
                    }

                    updateNearestDistanceBadge();
                    mapView.invalidate();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load report locations", Toast.LENGTH_SHORT).show());
    }

    private void updateNearestDistanceBadge() {
        if (tvNearestDistance == null) return;

        if (currentUserPoint == null || pendingReportPoints.isEmpty()) {
            tvNearestDistance.setText("--");
            return;
        }

        double minMeters = Double.MAX_VALUE;
        for (GeoPoint point : pendingReportPoints) {
            if (point == null) continue;
            double distance = currentUserPoint.distanceToAsDouble(point);
            if (distance < minMeters) {
                minMeters = distance;
            }
        }

        if (minMeters == Double.MAX_VALUE) {
            tvNearestDistance.setText("--");
            return;
        }

        if (minMeters < 1000) {
            tvNearestDistance.setText(String.format(Locale.getDefault(), "%dm", (int) Math.round(minMeters)));
        } else {
            tvNearestDistance.setText(String.format(Locale.getDefault(), "%.1fkm", (minMeters / 1000.0)));
        }
    }

    private void clearReportMarkers() {
        if (reportMarkers.isEmpty()) return;
        mapView.getOverlays().removeAll(reportMarkers);
        reportMarkers.clear();
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) return null;
            return Double.parseDouble(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return Long.parseLong(text);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private void updateNavbarCity(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                if (city == null || city.isEmpty()) {
                    city = addresses.get(0).getSubAdminArea();
                }
                if (city != null && !city.isEmpty() && tvUserLocation != null) {
                    tvUserLocation.setText(city);
                }
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        fetchMyReports(); // Refresh reports when returning to Home
        fetchUserStats();
        fetchPendingReportsForMap();
        checkImpendingNotifications();
        loadLoggedInUserInNavbar(); // Also refresh user profile
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationAndShowOnMap();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
