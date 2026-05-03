package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.LoginActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.firebase.firestore.FirebaseFirestore;

public class Settings extends AppCompatActivity {

    private final FirebaseManagerAndAuth authManager = new FirebaseManagerAndAuth();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ImageView ivProfileImage;
    private TextView tvProfileName;
    private TextView tvProfileRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup Bottom Navigation
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);
        bottomTrayHandler.setup();

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
    }

    private void loadUserData() {
        String uid = authManager.getCurrentUserUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String first = doc.getString("firstName");
                String last = doc.getString("lastName");
                String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                if (fullName.isEmpty()) fullName = "User";
                
                tvProfileName.setText(fullName);
                
                String profileUrl = doc.getString("profileImageUrl");
                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(this)
                            .load(profileUrl)
                            .placeholder(R.drawable.profile_image)
                            .circleCrop()
                            .into(ivProfileImage);
                }

                // Dynamic role display if available, else default
                String role = doc.getString("role");
                if (role != null && !role.isEmpty()) {
                    tvProfileRole.setText(role);
                } else {
                    tvProfileRole.setText("Volunteer");
                }
            }
        });
    }

    private void setupClickListeners() {
        TextView tvAccountProfile = findViewById(R.id.tvAccountProfile);
        if (tvAccountProfile != null) {
            tvAccountProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        TextView tvAccountRewards = findViewById(R.id.tvAccountScoreMetrics);
        if (tvAccountRewards != null) {
            tvAccountRewards.setOnClickListener(v -> startActivity(new Intent(this, RewardActivity.class)));
        }

        TextView tvReportProblem = findViewById(R.id.tvReportProblem);
        if (tvReportProblem != null) {
            tvReportProblem.setOnClickListener(v -> {
                Toast.makeText(this, "Report feature coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        TextView tvLogout = findViewById(R.id.tvLogout);
        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {
                authManager.logout();
                Intent intent = new Intent(Settings.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
