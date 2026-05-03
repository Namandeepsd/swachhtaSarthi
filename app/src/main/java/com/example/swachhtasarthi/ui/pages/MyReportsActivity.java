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
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MyReportsActivity extends AppCompatActivity {

    private RecyclerView rvReports;
    private MyReportsAdapter adapter;
    private final List<MyReports> myReportsList = new ArrayList<>();
    private final FirebaseManagerAndAuth authManager = new FirebaseManagerAndAuth();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvReports = findViewById(R.id.rvMyReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyReportsAdapter(myReportsList);
        rvReports.setAdapter(adapter);

        fetchAllMyReports();
    }

    private void fetchAllMyReports() {
        String uid = authManager.getCurrentUserUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("reports")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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

                    myReportsList.clear();
                    for (ReportRow row : rows) {
                        myReportsList.add(row.report);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching reports: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0L;
        }
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
}

