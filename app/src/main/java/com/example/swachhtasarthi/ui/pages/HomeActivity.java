package com.example.swachhtasarthi.ui.pages;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.service.MyReports;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;

    FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();
    RecyclerView rvReports;
    private View btnReport;
    private MapView mapView;
    private TextView tvUserName;
    private TextView tvUserLocation;
    private FusedLocationProviderClient fusedLocationClient;

    MyReportsAdapter myReportsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!firebaseManagerAndAuth.isUserLoggedIn()) {
            Intent intent = new Intent(HomeActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        btnReport = findViewById(R.id.btnReport);
        rvReports = findViewById(R.id.rvReports);
        mapView = findViewById(R.id.mapView);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserLocation = findViewById(R.id.tvUserLocation);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);

        bottomTrayHandler.setup();
        setupOpenStreetMap();
        loadLoggedInUserInNavbar();
        requestLocationAndShowOnMap();

        if (btnReport != null) {
            btnReport.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ReportActivity.class);
                startActivity(intent);
            });
        }

        rvReports.setLayoutManager(new LinearLayoutManager(this));

        // Adding Dummy Data for display
        List<MyReports> myReports = new ArrayList<>();
        myReports.add(new MyReports("Alwar Bypass", "Overflowing waste bins near main road.", "Sector 6, Bhiwadi", "5 mins ago", "PENDING", R.drawable.login_signup_hero_img));
        myReports.add(new MyReports("Sidhrawali, Dharuhera", "Illegal dumping of construction materials.", "North Park", "2 hours ago", "IN PROGRESS", R.drawable.login_signup_hero_img));
        myReports.add(new MyReports("Nai Wali, Rewari", "Overflowed Garbage Bins collected.", "Sabzi Mandi", "Yesterday", "RESOLVED", R.drawable.login_signup_hero_img));

        myReportsAdapter = new MyReportsAdapter(myReports);
        rvReports.setAdapter(myReportsAdapter);
    }

    private void setupOpenStreetMap() {
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        GeoPoint defaultPoint = new GeoPoint(28.6139, 77.2090);
        mapView.getController().setCenter(defaultPoint);
    }

    private void loadLoggedInUserInNavbar() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Use default value while loading
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
            mapView.getController().animateTo(userPoint);

            mapView.getOverlays().clear();
            Marker marker = new Marker(mapView);
            marker.setPosition(userPoint);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle("You are here");
            mapView.getOverlays().add(marker);
            mapView.invalidate();

            updateNavbarCity(location.getLatitude(), location.getLongitude());
        });
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
