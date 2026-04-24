package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.LoginActivity;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();

    // UI Components
    private ShapeableImageView ivProfileImage;
    private TextView tvProfileName, tvProfileRole;
    private TextView tvAccountProfile, tvAccountNotifications, tvAccountScoreMetrics;
    private TextView tvReportProblem, tvMoreKnowMore, tvLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!firebaseManagerAndAuth.isUserLoggedIn()) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_settings);

        initializeComponents();
        setupClickListeners();

        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);
        bottomTrayHandler.setup();
    }

    private void initializeComponents() {
        // Profile Header
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);

        // Account Section
        tvAccountProfile = findViewById(R.id.tvAccountProfile);
        tvAccountNotifications = findViewById(R.id.tvAccountNotifications);
        tvAccountScoreMetrics = findViewById(R.id.tvAccountScoreMetrics);

        // More Section
        tvReportProblem = findViewById(R.id.tvReportProblem);
        tvMoreKnowMore = findViewById(R.id.tvMoreKnowMore);
        tvLogout = findViewById(R.id.tvLogout);
    }

    private void setupClickListeners() {
        // Account Profile Click
        if (tvAccountProfile != null) {
            tvAccountProfile.setOnClickListener(v -> {
                // TODO: Navigate to Edit Profile screen
                Toast.makeText(this, "Edit Profile feature coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        // Notifications Click
        if (tvAccountNotifications != null) {
            tvAccountNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        // Score Metrics Click
        if (tvAccountScoreMetrics != null) {
            tvAccountScoreMetrics.setOnClickListener(v -> {
                // TODO: Navigate to Score Metrics screen
                Toast.makeText(this, "Score Metrics feature coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        // Report Problem Click
        if (tvReportProblem != null) {
            tvReportProblem.setOnClickListener(v -> {
                String url = "https://docs.google.com/forms/d/e/1FAIpQLSf4G4Q0gxknrMS495bjmKhsMBW8WPn4-Gg1G9iyrbnFs0itLw/viewform?usp=publish-editor";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "No browser found to open link", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Know More Click
        if (tvMoreKnowMore != null) {
            tvMoreKnowMore.setOnClickListener(v -> {
                // TODO: Show App Info or About Us
                Toast.makeText(this, "About Swachhta Sarthi coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        // Logout Click
        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {
                firebaseManagerAndAuth.logout();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
