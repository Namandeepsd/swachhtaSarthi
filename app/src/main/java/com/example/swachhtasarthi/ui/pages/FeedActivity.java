package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.FeedItem;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth authManager = new FirebaseManagerAndAuth();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private final List<FeedItem> feedItems = new ArrayList<>();
    private FeedAdapter adapter;
    private View notificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!authManager.isUserLoggedIn()) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_feed);

        // Setup Bottom Navigation
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);
        bottomTrayHandler.setup();

        notificationBadge = findViewById(R.id.notificationBadge);
        loadLoggedInUserInNavbar();
        checkImpendingNotifications();

        View search = findViewById(R.id.etGlobalSearch);
        if (search != null) {
            search.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        }

        View ivNotification = findViewById(R.id.ivNotification);
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }

        RecyclerView rvFeed = findViewById(R.id.rvFeed);
        rvFeed.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FeedAdapter(feedItems);
        rvFeed.setAdapter(adapter);

        fetchFilteredFeedReports();
    }

    private void checkImpendingNotifications() {
        String uid = authManager.getCurrentUserUid();
        if (uid == null || notificationBadge == null) return;

        firestore.collection("users")
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

    private void loadLoggedInUserInNavbar() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserLocation = findViewById(R.id.tvUserLocation);
        if (tvUserName != null) tvUserName.setText("User");

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String city = documentSnapshot.getString("city");

                    String fullName = "";
                    if (firstName != null && !firstName.trim().isEmpty()) {
                        fullName = firstName.trim();
                    }
                    if (lastName != null && !lastName.trim().isEmpty()) {
                        fullName = fullName.isEmpty() ? lastName.trim() : fullName + " " + lastName.trim();
                    }
                    if (!fullName.isEmpty() && tvUserName != null) {
                        tvUserName.setText(fullName);
                    }
                    if (city != null && !city.trim().isEmpty() && tvUserLocation != null) {
                        tvUserLocation.setText(city.trim());
                    }
                });
    }

    private void fetchFilteredFeedReports() {
        String currentUid = authManager.getCurrentUserUid();
        if (currentUid == null || currentUid.trim().isEmpty()) {
            return;
        }

        firestore.collection("reports")
                .whereNotEqualTo("status", "RESOLVED")
                .limit(500)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to load feed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots == null) return;

                    feedItems.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String ownerId = valueOrFallback(doc.getString("userId"), "");
                        String status = valueOrFallback(doc.getString("status"), "PENDING");
                        String reportByType = valueOrFallback(doc.getString("reportByType"), "individual");

                        String city = valueOrFallback(doc.getString("city"), "Reported Location");
                        String address = valueOrFallback(doc.getString("address"), "No address");
                        String description = valueOrFallback(doc.getString("description"), "No description available");
                        String imageUrl = extractFirstImage(doc);
                        String latitude = valueOrFallback(toStringDouble(doc.get("latitude")), "");
                        String longitude = valueOrFallback(toStringDouble(doc.get("longitude")), "");
                        String pinCode = valueOrFallback(doc.getString("pinCode"), "");
                        String dateOfIssue = valueOrFallback(doc.getString("dateOfIssue"), "");
                        String timeOfIssue = valueOrFallback(doc.getString("timeOfIssue"), "");
                        long createdAt = toLong(doc.get("createdAt"));
                        long upvoteCount = doc.contains("likes")
                            ? toLong(doc.get("likes"))
                            : toLong(doc.get("upvoteCount"));
                        long commentCount = toLong(doc.get("commentCount"));

                        ensureEngagementFields(doc, upvoteCount, commentCount);

                        feedItems.add(new FeedItem(
                                doc.getId(),
                                ownerId,
                                "community".equalsIgnoreCase(reportByType) ? "Community report" : "Individual report",
                                city,
                                status,
                                description,
                                address,
                                latitude,
                                longitude,
                                pinCode,
                                dateOfIssue,
                                timeOfIssue,
                                createdAt,
                                imageUrl,
                                "",
                                upvoteCount,
                                commentCount,
                                false,
                                R.drawable.profile_image,
                                R.drawable.login_signup_hero_img
                        ));
                    }

                    Collections.sort(feedItems, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    adapter.notifyDataSetChanged();
                    hydrateUserUpvoteState(currentUid);
                    hydrateUploaderInfo();

                    if (feedItems.isEmpty()) {
                        Toast.makeText(this, "No active reports found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void hydrateUserUpvoteState(String currentUid) {
        for (int i = 0; i < feedItems.size(); i++) {
            int index = i;
            FeedItem item = feedItems.get(i);
            String reportId = item.getReportId();
            if (reportId == null || reportId.trim().isEmpty()) {
                continue;
            }

            String upvoteDocId = reportId + "_" + currentUid;
            firestore.collection("likes")
                    .document(upvoteDocId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        item.setUpvotedByCurrentUser(snapshot.exists());
                        if (index < feedItems.size()) {
                            adapter.notifyItemChanged(index);
                        }
                    })
                    .addOnFailureListener(e -> firestore.collection("reports")
                            .document(reportId)
                            .collection("likes")
                            .document(currentUid)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                item.setUpvotedByCurrentUser(snapshot.exists());
                                if (index < feedItems.size()) {
                                    adapter.notifyItemChanged(index);
                                }
                            }));
        }
    }

    private void hydrateUploaderInfo() {
        for (int i = 0; i < feedItems.size(); i++) {
            int index = i;
            FeedItem item = feedItems.get(i);
            String ownerId = item.getReportOwnerId();
            if (ownerId == null || ownerId.trim().isEmpty()) {
                continue;
            }

            firestore.collection("users")
                    .document(ownerId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.exists()) {
                            return;
                        }

                        String firstName = valueOrFallback(snapshot.getString("firstName"), "");
                        String lastName = valueOrFallback(snapshot.getString("lastName"), "");
                        String fullName = (firstName + " " + lastName).trim();
                        if (fullName.isEmpty()) {
                            fullName = "Community Member";
                        }

                        String profileImageUrl = valueOrFallback(snapshot.getString("profileImageUrl"), "");
                        item.setUserName(fullName);
                        item.setUploaderProfileUrl(profileImageUrl);

                        if (index < feedItems.size()) {
                            adapter.notifyItemChanged(index);
                        }
                    });
        }
    }

    private void ensureEngagementFields(QueryDocumentSnapshot doc, long upvoteCount, long commentCount) {
        Map<String, Object> defaults = new HashMap<>();
        if (!doc.contains("likes")) {
            defaults.put("likes", upvoteCount);
        }
        if (!doc.contains("upvoteCount")) {
            defaults.put("upvoteCount", upvoteCount);
        }
        if (!doc.contains("commentCount")) {
            defaults.put("commentCount", commentCount);
        }

        if (!defaults.isEmpty()) {
            firestore.collection("reports")
                    .document(doc.getId())
                    .set(defaults, SetOptions.merge());
        }
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

    private String valueOrFallback(String value, String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value : fallback;
    }

    private boolean isResolved(String status) {
        return "RESOLVED".equalsIgnoreCase(status);
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String toStringDouble(Object value) {
        if (value == null) return "";
        if (value instanceof Number) {
            return String.valueOf(((Number) value).doubleValue());
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) return "";
        try {
            return String.valueOf(Double.parseDouble(s));
        } catch (Exception ignored) {
            return "";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkImpendingNotifications();
    }
}
