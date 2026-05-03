package com.example.swachhtasarthi.ui.pages;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.MyReports;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeopleActivity extends AppCompatActivity {

    private static final String TAG = "PeopleActivity";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId;

    private TextView tvName, tvCity, tvScore, tvReportsCount, tvContributionsCount, tvEmptyState;
    private ImageView ivAvatar;
    private RecyclerView rvReports, rvContributions;
    
    private MyReportsAdapter reportsAdapter;
    private final List<MyReports> reportsList = new ArrayList<>();
    
    private MyReportsAdapter contributionsAdapter;
    private final List<MyReports> contributionsList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people);

        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadUserProfile();
        loadUserReports();
        loadUserContributions();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ivAvatar = findViewById(R.id.ivAvatar);
        tvName = findViewById(R.id.tvName);
        tvCity = findViewById(R.id.tvCity);
        tvScore = findViewById(R.id.tvScore);
        tvReportsCount = findViewById(R.id.tvReportsCount);
        tvContributionsCount = findViewById(R.id.tvContributionsCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvReports = findViewById(R.id.rvUserReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        reportsAdapter = new MyReportsAdapter(reportsList);
        rvReports.setAdapter(reportsAdapter);

        rvContributions = findViewById(R.id.rvUserContributions);
        rvContributions.setLayoutManager(new LinearLayoutManager(this));
        contributionsAdapter = new MyReportsAdapter(contributionsList);
        rvContributions.setAdapter(contributionsAdapter);
    }

    private void loadUserProfile() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String first = doc.getString("firstName");
                    String last = doc.getString("lastName");
                    String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
                    tvName.setText(name.isEmpty() ? "User" : name);
                    tvCity.setText(doc.getString("city"));

                    long score = 0;
                    Object val = doc.get("rewardScoreCurrent");
                    if (val instanceof Number) score = ((Number) val).longValue();
                    tvScore.setText(String.valueOf(score));

                    String profileUrl = doc.getString("profileImageUrl");
                    if (profileUrl != null && !profileUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileUrl)
                                .placeholder(R.drawable.profile_image)
                                .circleCrop()
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.drawable.profile_image);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user profile", e));
    }

    private void loadUserReports() {
        // Temporarily removing .orderBy() to avoid requirement for composite indexes
        db.collection("reports")
                .whereEqualTo("userId", userId)
                .limit(10)
                .get()
                .addOnSuccessListener(snaps -> {
                    reportsList.clear();
                    for (QueryDocumentSnapshot doc : snaps) {
                        reportsList.add(mapToReport(doc));
                    }
                    reportsAdapter.notifyDataSetChanged();
                    tvReportsCount.setText(String.valueOf(snaps.size()));
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user reports", e);
                    Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserContributions() {
        // Temporarily removing .orderBy() to avoid requirement for composite indexes
        db.collection("volunteerActions")
                .whereEqualTo("userId", userId)
                .limit(10)
                .get()
                .addOnSuccessListener(snaps -> {
                    contributionsList.clear();
                    for (QueryDocumentSnapshot doc : snaps) {
                        contributionsList.add(mapActionToReport(doc));
                    }
                    contributionsAdapter.notifyDataSetChanged();
                    tvContributionsCount.setText(String.valueOf(snaps.size()));
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user contributions", e);
                    Toast.makeText(this, "Failed to load contributions", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (reportsList.isEmpty() && contributionsList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private MyReports mapToReport(QueryDocumentSnapshot doc) {
        String title = valueOrFallback(doc.getString("city"), "Report");
        String description = valueOrFallback(doc.getString("description"), "");
        String address = valueOrFallback(doc.getString("address"), "No location");
        String date = valueOrFallback(doc.getString("dateOfIssue"), "");
        String status = valueOrFallback(doc.getString("status"), "PENDING");
        String imageUrl = extractFirstImage(doc);

        return new MyReports(title, description, address, date, status, imageUrl);
    }

    private MyReports mapActionToReport(QueryDocumentSnapshot doc) {
        String city = valueOrFallback(doc.getString("city"), "Resolved Task");
        String address = valueOrFallback(doc.getString("address"), "");
        String imageUrl = valueOrFallback(doc.getString("proofImageUrl"), "");
        String type = valueOrFallback(doc.getString("contributionType"), "Individual");
        
        return new MyReports(city, "Contribution: " + type, address, "Completed", "RESOLVED", imageUrl);
    }

    private String valueOrFallback(String v, String f) {
        return v == null || v.trim().isEmpty() ? f : v;
    }

    private String extractFirstImage(QueryDocumentSnapshot doc) {
        // 1. Check imageUris list
        Object imageUrisObj = doc.get("imageUris");
        if (imageUrisObj instanceof List) {
            List<?> images = (List<?>) imageUrisObj;
            if (!images.isEmpty() && images.get(0) != null) return String.valueOf(images.get(0));
        }

        // 2. Check imageUrl string
        String imageUrl = doc.getString("imageUrl");
        if (imageUrl != null && !imageUrl.trim().isEmpty()) return imageUrl;

        // 3. Check images list of objects (used in some parts of the app)
        Object imagesObj = doc.get("images");
        if (imagesObj instanceof List) {
            List<?> images = (List<?>) imagesObj;
            if (!images.isEmpty() && images.get(0) instanceof Map) {
                Object url = ((Map<?, ?>) images.get(0)).get("url");
                if (url != null) return String.valueOf(url);
            }
        }

        return "";
    }
}
