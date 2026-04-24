package com.example.swachhtasarthi.ui.pages;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.MyReports;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommunityContributionActivity extends AppCompatActivity {

    private RecyclerView rvContributions;
    private MyReportsAdapter adapter;
    private List<MyReports> contributionList = new ArrayList<>();
    private final FirebaseManagerAndAuth authManager = new FirebaseManagerAndAuth();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_contribution);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvContributions = findViewById(R.id.rvContributions);
        rvContributions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyReportsAdapter(contributionList);
        rvContributions.setAdapter(adapter);

        fetchContributions();
    }

    private void fetchContributions() {
        String uid = authManager.getCurrentUserUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("reports")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contributionList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String description = valueOrFallback(doc.getString("description"), "No description");
                        String address = valueOrFallback(doc.getString("address"), doc.getString("location"));
                        String city = valueOrFallback(doc.getString("city"), doc.getString("title"));
                        String date = valueOrFallback(doc.getString("dateOfIssue"), "Date unavailable");
                        String status = valueOrFallback(doc.getString("status"), "PENDING");

                        String firstImage = extractFirstImage(doc);
                        String title = valueOrFallback(city, "Report");
                        String location = valueOrFallback(address, "Unknown Location");

                        contributionList.add(new MyReports(title, description, location, date, status, firstImage));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching contributions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
}
