package com.example.swachhtasarthi.service;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AppNotificationService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void sendToUser(String userId,
                           String type,
                           String title,
                           String message,
                           String reason,
                           Map<String, Object> extra) {
        if (userId == null || userId.trim().isEmpty()) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", safe(type));
        payload.put("title", safe(title));
        payload.put("message", safe(message));
        payload.put("reason", safe(reason));
        payload.put("status", "info");
        payload.put("createdAt", FieldValue.serverTimestamp());
        if (extra != null) {
            payload.putAll(extra);
        }

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(payload);
    }

    public void sendToCommunity(String communityId,
                                String type,
                                String title,
                                String message,
                                String reason,
                                Map<String, Object> extra) {
        if (communityId == null || communityId.trim().isEmpty()) return;

        // Send to the community owner
        sendToUser(communityId, type, title, message, reason, extra);

        // Broadcast to all members
        db.collection("community")
                .document(communityId)
                .collection("members")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String memberId = doc.getId();
                        if (!memberId.equals(communityId)) {
                            sendToUser(memberId, type, title, message, reason, extra);
                        }
                    }
                });
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
