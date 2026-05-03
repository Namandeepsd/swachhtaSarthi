package com.example.swachhtasarthi.ui.pages;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.Notification;
import com.example.swachhtasarthi.service.AppNotificationService;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnInviteActionListener {

    private final FirebaseManagerAndAuth auth = new FirebaseManagerAndAuth();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Notification> notifications = new ArrayList<>();
    private NotificationAdapter adapter;
    private String currentUid;
    private final AppNotificationService appNotificationService = new AppNotificationService();
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        RecyclerView rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty = findViewById(R.id.tvEmptyNotifications);

        adapter = new NotificationAdapter(notifications, this);
        rvNotifications.setAdapter(adapter);

        currentUid = auth.getCurrentUserUid();
        if (currentUid == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchNotifications();
    }

    private void fetchNotifications() {
        db.collection("users")
                .document(currentUid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(snaps -> {
                    notifications.clear();
                    for (QueryDocumentSnapshot doc : snaps) {
                        String type = safe(doc.getString("type"));
                        String status = safe(doc.getString("status"));
                        String communityName = safe(doc.getString("communityName"));
                        if (communityName.isEmpty()) communityName = "a community";

                        String title = safe(doc.getString("title"));
                        if (title.isEmpty()) title = defaultTitle(type);

                        String message = safe(doc.getString("message"));
                        if (message.isEmpty()) message = "You were invited to join " + communityName;

                        String reason = safe(doc.getString("reason"));
                        Timestamp ts = doc.getTimestamp("createdAt");
                        boolean actionable = "pending".equals(status) && "community_invite".equals(type);

                        notifications.add(new Notification(
                                doc.getId(),
                                type,
                                title,
                                message,
                                reason,
                                toRelativeTime(ts),
                                R.drawable.notification,
                                actionable,
                                status,
                                safe(doc.getString("communityId")),
                                communityName
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    if (tvEmpty != null) tvEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onAccept(Notification notification, int position) {
        handleInviteAction(notification, position, true);
    }

    @Override
    public void onDecline(Notification notification, int position) {
        handleInviteAction(notification, position, false);
    }

    private void handleInviteAction(Notification notification, int position, boolean accept) {
        if (notification.getId() == null || notification.getCommunityId() == null || notification.getCommunityId().trim().isEmpty()) {
            Toast.makeText(this, "Invalid invite data", Toast.LENGTH_SHORT).show();
            return;
        }

        String inviteDocId = notification.getId();
        String communityId = notification.getCommunityId();

        db.runTransaction(transaction -> {
            DocumentReference inviteRef = db.collection("users")
                    .document(currentUid)
                    .collection("notifications")
                    .document(inviteDocId);
            DocumentSnapshot inviteSnap = transaction.get(inviteRef);

            if (!inviteSnap.exists()) throw new IllegalStateException("Invite notification missing");
            if (!"pending".equals(inviteSnap.getString("status"))) throw new IllegalStateException("Invite already processed");

            if (accept) {
                // 1. Add to members list
                DocumentReference memberRef = db.collection("community")
                        .document(communityId)
                        .collection("members")
                        .document(currentUid);

                if (!transaction.get(memberRef).exists()) {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("id", currentUid);
                    memberData.put("joinedAt", FieldValue.serverTimestamp());
                    transaction.set(memberRef, memberData);

                    // 2. Increment members count
                    DocumentReference communityRef = db.collection("community").document(communityId);
                    transaction.update(communityRef, "membersCount", FieldValue.increment(1));

                    // 3. Update user profile with joinedCommunityId
                    DocumentReference userRef = db.collection("users").document(currentUid);
                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("joinedCommunityId", communityId);
                    transaction.set(userRef, userUpdate, SetOptions.merge());
                }
            }

            // 4. Update notification status in user profile
            transaction.update(inviteRef,
                    "status", accept ? "accepted" : "declined",
                    "actedAt", FieldValue.serverTimestamp());

            // 5. Update invite status in community collection
            DocumentReference communityInviteRef = db.collection("community")
                    .document(communityId)
                    .collection("memberInvites")
                    .document(currentUid);

            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("status", accept ? "accepted" : "declined");
            statusUpdate.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(communityInviteRef, statusUpdate, SetOptions.merge());

            return null;
        }).addOnSuccessListener(unused -> {
            sendUpdateNotifications(communityId, accept);
            Toast.makeText(this, accept ? "Joined community!" : "Invite declined", Toast.LENGTH_SHORT).show();
            fetchNotifications();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            fetchNotifications();
        });
    }

    private void sendUpdateNotifications(String communityId, boolean accept) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("communityId", communityId);

        // Notify self
        appNotificationService.sendToUser(
                currentUid,
                "community_membership_update",
                accept ? "Welcome to the community!" : "Invite Declined",
                accept ? "You are now a member." : "You declined the invitation.",
                "System generated",
                extra
        );

        if (accept) {
            // Notify community members
            extra.put("memberId", currentUid);
            appNotificationService.sendToCommunity(
                    communityId,
                    "community_member_joined",
                    "New Member",
                    "Someone just joined your community via invite.",
                    "Invite accepted",
                    extra
            );
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String toRelativeTime(Timestamp timestamp) {
        if (timestamp == null) return "now";
        long diffMs = System.currentTimeMillis() - timestamp.toDate().getTime();
        if (diffMs < 0) diffMs = 0;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
        if (minutes < 1) return "now";
        if (minutes < 60) return minutes + "m ago";
        long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
        if (hours < 24) return hours + "h ago";
        return TimeUnit.MILLISECONDS.toDays(diffMs) + "d ago";
    }

    private String defaultTitle(String type) {
        switch (safe(type)) {
            case "community_invite": return "Community Invite";
            case "community_follow": return "New Follow";
            case "score_increase": return "Score Boost";
            case "report_completed": return "Report Resolved";
            default: return "Notification";
        }
    }
}
