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
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // UI Components
    private ShapeableImageView ivProfileImage;
    private TextView tvProfileName, tvProfileRole;
    private TextView tvAccountProfile, tvAccountScoreMetrics;
    private TextView tvDonate, tvReportProblem, tvMoreKnowMore, tvLogout;

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
        loadBasicUserData();
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
        tvAccountScoreMetrics = findViewById(R.id.tvAccountScoreMetrics);

        // More Section
        tvDonate = findViewById(R.id.tvDonate);
        tvReportProblem = findViewById(R.id.tvReportProblem);
        tvMoreKnowMore = findViewById(R.id.tvMoreKnowMore);
        tvLogout = findViewById(R.id.tvLogout);
    }

    private void loadBasicUserData() {
        String uid = firebaseManagerAndAuth.getCurrentUserUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String firstName = documentSnapshot.getString("firstName");
                String lastName = documentSnapshot.getString("lastName");
                if (firstName != null && lastName != null) {
                    tvProfileName.setText(firstName + " " + lastName);
                }
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(this).load(profileImageUrl).placeholder(R.drawable.profile_image).into(ivProfileImage);
                }
                // Role could be dynamic, but for now it's "Volunteer" in XML
            }
        });
    }

    private void setupClickListeners() {
        // Account Profile Click - Now redirects to ProfileActivity
        if (tvAccountProfile != null) {
            tvAccountProfile.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        // Rewards Click
        if (tvAccountScoreMetrics != null) {
            tvAccountScoreMetrics.setOnClickListener(v -> {
                Intent intent = new Intent(this, RewardActivity.class);
                startActivity(intent);
            });
        }

        // Donate Click
        if (tvDonate != null) {
            tvDonate.setOnClickListener(v -> {
                String upiId = "8708181941@ybl";
                String payeeName = "Swachhta Sarthi";
                String transactionNote = "Donation for Swachhta Sarthi";
                String amount = "0"; // Let user enter amount in their UPI app
                
                Uri uri = Uri.parse("upi://pay").buildUpon()
                        .appendQueryParameter("pa", upiId)
                        .appendQueryParameter("pn", payeeName)
                        .appendQueryParameter("tn", transactionNote)
                        .appendQueryParameter("am", amount)
                        .appendQueryParameter("cu", "INR")
                        .build();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);

                try {
                    Intent chooser = Intent.createChooser(intent, "Pay with...");
                    startActivity(chooser);
                } catch (Exception e) {
                    Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show();
                }
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
                String url = "https://docs.google.com/forms/d/e/1FAIpQLSc9Ob0agWBzWqSxTA01LvckyWco4tbQvBehWdxpKJohE9_q7Q/viewform?usp=publish-editor";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "No browser found to open link", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Logout Click
        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {
                firebaseManagerAndAuth.logout();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
