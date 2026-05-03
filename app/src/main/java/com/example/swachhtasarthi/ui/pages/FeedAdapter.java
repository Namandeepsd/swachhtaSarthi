package com.example.swachhtasarthi.ui.pages;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.FeedItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private final List<FeedItem> feedItems;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private Context appContext;
    private final Map<String, ListenerRegistration> reportEngagementListeners = new HashMap<>();

    public FeedAdapter(List<FeedItem> feedItems) {
        this.feedItems = feedItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (appContext == null) {
            appContext = parent.getContext().getApplicationContext();
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_feed_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FeedItem item = feedItems.get(position);
        holder.tvUserName.setText(item.getUserName());
        holder.tvLocation.setText(item.getLocation());
        holder.tvStatusTag.setText(item.getStatus());
        holder.tvDescription.setText(item.getDescription());

        String profileUrl = item.getUploaderProfileUrl();
        if (profileUrl != null && !profileUrl.trim().isEmpty() && !profileUrl.trim().startsWith("content://")) {
            Glide.with(holder.itemView.getContext())
                    .load(profileUrl)
                    .placeholder(item.getUserProfileImage())
                    .error(item.getUserProfileImage())
                    .into(holder.ivUserProfile);
        } else if (profileUrl != null && profileUrl.trim().startsWith("content://")) {
            try {
                holder.ivUserProfile.setImageURI(android.net.Uri.parse(profileUrl.trim()));
            } catch (Exception ignored) {
                holder.ivUserProfile.setImageResource(item.getUserProfileImage());
            }
        } else {
            holder.ivUserProfile.setImageResource(item.getUserProfileImage());
        }

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrl.trim().startsWith("content://")) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(item.getFallbackPostImage())
                    .error(item.getFallbackPostImage())
                    .into(holder.ivPostImage);
        } else if (imageUrl != null && imageUrl.trim().startsWith("content://")) {
            try {
                holder.ivPostImage.setImageURI(android.net.Uri.parse(imageUrl.trim()));
            } catch (Exception ignored) {
                holder.ivPostImage.setImageResource(item.getFallbackPostImage());
            }
        } else {
            holder.ivPostImage.setImageResource(item.getFallbackPostImage());
        }

        holder.tvUpvoteCount.setText(String.valueOf(item.getUpvoteCount()));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        bindDescriptionReadMore(holder, item);
        styleStatusTag(holder, item.getStatus());
        applyUpvoteIconState(holder, item.isUpvotedByCurrentUser());
        ensureReportEngagementListener(holder, item);

        holder.ivShare.setOnClickListener(v -> shareReport(holder, item));
        holder.ivContribute.setOnClickListener(v -> openVolunteerScreen(holder, item));
        holder.ivUpvote.setOnClickListener(v -> toggleUpvote(holder, item));
        holder.ivComment.setOnClickListener(v -> openCommentDialog(holder, item));
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        String reportId = holder.boundReportId;
        if (reportId != null) {
            holder.boundReportId = null;
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for (ListenerRegistration reg : reportEngagementListeners.values()) {
            if (reg != null) reg.remove();
        }
        reportEngagementListeners.clear();
    }

    private void styleStatusTag(@NonNull ViewHolder holder, String status) {
        holder.tvStatusTag.setBackgroundResource(R.drawable.bg_pending_tag);
        if ("RESOLVED".equalsIgnoreCase(status)) {
            holder.tvStatusTag.setTextColor(0xFF10B981);
        } else if ("IN PROGRESS".equalsIgnoreCase(status)) {
            holder.tvStatusTag.setTextColor(0xFF2563EB);
        } else {
            holder.tvStatusTag.setTextColor(0xFFE67E22);
        }
    }

    private void shareReport(@NonNull ViewHolder holder, @NonNull FeedItem item) {
        String reportId = safe(item.getReportId());
        String mapsLink = buildMapsLink(item);

        String shareText = "Swachhta Sarthi Report\n\n"
                + "Report ID: " + reportId + "\n"
                + "Status: " + safe(item.getStatus()) + "\n"
                + "City: " + safe(item.getLocation()) + "\n"
                + "Full address: " + safe(item.getAddress()) + "\n"
                + "PIN: " + safe(item.getPinCode()) + "\n"
                + "Lat,Lng: " + safe(item.getLatitude()) + ", " + safe(item.getLongitude()) + "\n"
                + "Date/Time: " + safe(item.getDateOfIssue()) + " " + safe(item.getTimeOfIssue()) + "\n"
                + "Description: " + safe(item.getDescription()) + "\n"
                + "Image: " + safe(item.getImageUrl()) + "\n\n"
                + "Open in maps: " + mapsLink;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        holder.itemView.getContext().startActivity(Intent.createChooser(shareIntent, "Share report via"));
    }

    private String buildDeepLink(@NonNull FeedItem item) {
        String reportId = item.getReportId();
        if (reportId == null) reportId = "";
        Uri uri = new Uri.Builder()
                .scheme("swachhtasarthi")
                .authority("report")
                .appendQueryParameter("reportId", reportId)
                .appendQueryParameter("ownerId", safeEmpty(item.getReportOwnerId()))
                .appendQueryParameter("city", safeEmpty(item.getLocation()))
                .appendQueryParameter("address", safeEmpty(item.getAddress()))
                .appendQueryParameter("description", safeEmpty(item.getDescription()))
                .appendQueryParameter("imageUrl", safeEmpty(item.getImageUrl()))
                .appendQueryParameter("latitude", safeEmpty(item.getLatitude()))
                .appendQueryParameter("longitude", safeEmpty(item.getLongitude()))
                .appendQueryParameter("pinCode", safeEmpty(item.getPinCode()))
                .appendQueryParameter("dateOfIssue", safeEmpty(item.getDateOfIssue()))
                .appendQueryParameter("timeOfIssue", safeEmpty(item.getTimeOfIssue()))
                .build();
        return uri.toString();
    }

    private String buildMapsLink(@NonNull FeedItem item) {
        String lat = safeEmpty(item.getLatitude());
        String lon = safeEmpty(item.getLongitude());
        if (!lat.isEmpty() && !lon.isEmpty()) {
            return "https://maps.google.com/?q=" + Uri.encode(lat + "," + lon);
        }
        String addr = safeEmpty(item.getAddress());
        if (!addr.isEmpty()) {
            return "https://maps.google.com/?q=" + Uri.encode(addr);
        }
        return "N/A";
    }

    private String safeEmpty(String s) {
        return (s == null) ? "" : s;
    }

    private void openVolunteerScreen(@NonNull ViewHolder holder, @NonNull FeedItem item) {
        Intent intent = new Intent(holder.itemView.getContext(), VolunteerActivity.class);
        intent.putExtra("reportId", item.getReportId());
        intent.putExtra("reportOwnerId", item.getReportOwnerId());
        intent.putExtra("city", item.getLocation());
        intent.putExtra("address", item.getAddress());
        intent.putExtra("description", item.getDescription());
        holder.itemView.getContext().startActivity(intent);
    }

    private void toggleUpvote(@NonNull ViewHolder holder, @NonNull FeedItem item) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        boolean targetUpvotedState = !item.isUpvotedByCurrentUser();
        boolean previousUpvotedState = item.isUpvotedByCurrentUser();
        long previousCount = item.getUpvoteCount();
        long optimisticCount = targetUpvotedState ? previousCount + 1 : Math.max(0, previousCount - 1);

        // Optimistic UI, but rollback on failure so it doesn't look "local only".
        item.setUpvotedByCurrentUser(targetUpvotedState);
        item.setUpvoteCount(optimisticCount);
        holder.tvUpvoteCount.setText(String.valueOf(optimisticCount));
        applyUpvoteIconState(holder, targetUpvotedState);
        holder.ivUpvote.setEnabled(false);

        DocumentReference reportRef = firestore.collection("reports").document(item.getReportId());
        // Use TOP-LEVEL collection for visibility as requested
        String likeDocId = item.getReportId() + "_" + currentUid;
        DocumentReference likeRef = firestore.collection("likes").document(likeDocId);

        firestore.runTransaction(tx -> {
            if (targetUpvotedState) {
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("reportId", item.getReportId());
                likeData.put("userId", currentUid);
                likeData.put("createdAt", FieldValue.serverTimestamp());
                tx.set(likeRef, likeData);
                tx.update(reportRef,
                        "upvoteCount", FieldValue.increment(1),
                        "likes", FieldValue.increment(1)
                );
            } else {
                tx.delete(likeRef);
                tx.update(reportRef,
                        "upvoteCount", FieldValue.increment(-1),
                        "likes", FieldValue.increment(-1)
                );
            }
            return null;
        }).addOnSuccessListener(unused -> holder.ivUpvote.setEnabled(true))
          .addOnFailureListener(e -> {
              holder.ivUpvote.setEnabled(true);
              // Rollback optimistic UI.
              item.setUpvotedByCurrentUser(previousUpvotedState);
              item.setUpvoteCount(previousCount);
              holder.tvUpvoteCount.setText(String.valueOf(previousCount));
              applyUpvoteIconState(holder, previousUpvotedState);
              Toast.makeText(holder.itemView.getContext(), "Like failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          });
    }

    private void openCommentDialog(@NonNull ViewHolder holder, @NonNull FeedItem item) {
        Context context = holder.itemView.getContext();
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_comments_popup, null, false);
        RecyclerView rv = view.findViewById(R.id.rvComments);
        TextView tvNo = view.findViewById(R.id.tvNoComments);
        EditText et = view.findViewById(R.id.etCommentInput);
        ImageView btn = view.findViewById(R.id.ivSendComment);

        List<CommentItem> list = new ArrayList<>();
        CommentsPopupAdapter adapter = new CommentsPopupAdapter(list);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(adapter);

        // Real-time updates so comments look global.
        ListenerRegistration commentsReg = listenComments(item.getReportId(), list, adapter, tvNo);

        AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        dialog.setOnDismissListener(d -> {
            if (commentsReg != null) commentsReg.remove();
        });
        btn.setOnClickListener(v -> {
            String msg = et.getText().toString().trim();
            if (msg.isEmpty()) return;
            et.setText("");
            postComment(holder, item, currentUid, msg);
        });
        dialog.show();
    }

    private ListenerRegistration listenComments(String reportId, List<CommentItem> list, CommentsPopupAdapter adapter, TextView tvNo) {
        return firestore.collection("comments")
                .whereEqualTo("reportId", reportId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(tvNo.getContext(), "Failed to load comments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        tvNo.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                        return;
                    }
                    if (snapshots == null) {
                        tvNo.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                        return;
                    }
                    list.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        list.add(new CommentItem(
                                safe(doc.getString("userName")),
                                safe(doc.getString("message")),
                                toLong(doc.get("createdAt"))
                        ));
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.createdAt, a.createdAt));
                    adapter.notifyDataSetChanged();
                    tvNo.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void postComment(@NonNull ViewHolder holder, FeedItem item, String uid, String msg) {
        Context context = holder.itemView.getContext();
        holder.ivComment.setEnabled(false);
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    String fName = userDoc.getString("firstName");
                    String lName = userDoc.getString("lastName");
                    String uName = ((fName == null ? "" : fName) + " " + (lName == null ? "" : lName)).trim();
                    if (uName.isEmpty()) uName = "Community Member";

                    DocumentReference reportRef = firestore.collection("reports").document(item.getReportId());
                    DocumentReference commentRef = firestore.collection("comments").document();

                    Map<String, Object> data = new HashMap<>();
                    data.put("reportId", item.getReportId());
                    data.put("userId", uid);
                    data.put("userName", uName);
                    data.put("message", msg);
                    data.put("createdAt", FieldValue.serverTimestamp());

                    WriteBatch batch = firestore.batch();
                    batch.set(commentRef, data);
                    batch.update(reportRef, "commentCount", FieldValue.increment(1));
                    batch.commit()
                            .addOnSuccessListener(unused -> holder.ivComment.setEnabled(true))
                            .addOnFailureListener(e -> {
                                holder.ivComment.setEnabled(true);
                                Toast.makeText(context, "Comment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    holder.ivComment.setEnabled(true);
                    Toast.makeText(context, "Comment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyUpvoteIconState(@NonNull ViewHolder holder, boolean upvoted) {
        holder.ivUpvote.setColorFilter(upvoted ? Color.parseColor("#135BEC") : Color.parseColor("#64748B"));
    }

    private void bindDescriptionReadMore(@NonNull ViewHolder holder, @NonNull FeedItem item) {
        String desc = safe(item.getDescription());
        holder.tvDescription.setText(desc);
        if (desc.length() <= 55) {
            holder.tvReadMore.setVisibility(View.GONE);
            holder.tvDescription.setMaxLines(1);
            return;
        }
        holder.tvReadMore.setVisibility(View.VISIBLE);
        boolean exp = item.isDescriptionExpanded();
        holder.tvDescription.setMaxLines(exp ? Integer.MAX_VALUE : 1);
        holder.tvReadMore.setText(exp ? "Read less" : "Read more");
        holder.tvReadMore.setOnClickListener(v -> {
            item.setDescriptionExpanded(!item.isDescriptionExpanded());
            notifyItemChanged(holder.getBindingAdapterPosition());
        });
    }

    private long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "N/A" : s;
    }

    private String formatTimeAgo(long m) {
        long d = Math.max(0L, System.currentTimeMillis() - m);
        if (d < 60000L) return "now";
        if (d < 3600000L) return (d / 60000L) + "m";
        if (d < 86400000L) return (d / 3600000L) + "h";
        return (d / 86400000L) + "d";
    }

    private void ensureReportEngagementListener(@NonNull ViewHolder holder, @NonNull FeedItem item) {
        String reportId = item.getReportId();
        holder.boundReportId = reportId;
        if (reportId == null || reportId.trim().isEmpty()) return;
        if (reportEngagementListeners.containsKey(reportId)) return;

        ListenerRegistration reg = firestore.collection("reports")
                .document(reportId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    long upvotes = toLong(snapshot.get("upvoteCount"));
                    if (upvotes == 0L) {
                        upvotes = toLong(snapshot.get("likes"));
                    }
                    long comments = toLong(snapshot.get("commentCount"));
                    item.setUpvoteCount(upvotes);
                    item.setCommentCount(comments);

                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        holder.tvUpvoteCount.setText(String.valueOf(upvotes));
                        holder.tvCommentCount.setText(String.valueOf(comments));
                    }
                });
        reportEngagementListeners.put(reportId, reg);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserProfile, ivPostImage, ivContribute, ivUpvote, ivComment, ivShare;
        TextView tvUserName, tvLocation, tvStatusTag, tvDescription, tvReadMore, tvUpvoteCount, tvCommentCount;
        String boundReportId;
        public ViewHolder(@NonNull View v) {
            super(v);
            ivUserProfile = v.findViewById(R.id.ivUserProfile);
            ivPostImage = v.findViewById(R.id.ivPostImage);
            tvUserName = v.findViewById(R.id.tvUserName);
            tvLocation = v.findViewById(R.id.tvLocation);
            tvStatusTag = v.findViewById(R.id.tvStatusTag);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvReadMore = v.findViewById(R.id.tvReadMore);
            ivContribute = v.findViewById(R.id.ivContribute);
            ivUpvote = v.findViewById(R.id.ivUpvote);
            ivComment = v.findViewById(R.id.ivComment);
            ivShare = v.findViewById(R.id.ivShare);
            tvUpvoteCount = v.findViewById(R.id.tvUpvoteCount);
            tvCommentCount = v.findViewById(R.id.tvCommentCount);
        }
    }

    private static class CommentItem {
        final String userName, message;
        final long createdAt;
        CommentItem(String u, String m, long c) { userName = u; message = m; createdAt = c; }
    }

    private class CommentsPopupAdapter extends RecyclerView.Adapter<CommentsPopupAdapter.CommentViewHolder> {
        private final List<CommentItem> items;
        CommentsPopupAdapter(List<CommentItem> i) { items = i; }
        @NonNull @Override public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new CommentViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.component_comment_item, p, false));
        }
        @Override public void onBindViewHolder(@NonNull CommentViewHolder h, int p) {
            CommentItem i = items.get(p);
            h.tvName.setText(i.userName);
            h.tvMsg.setText(i.message);
            h.tvTime.setText(formatTimeAgo(i.createdAt));
        }
        @Override public int getItemCount() { return items.size(); }
        class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvMsg, tvTime;
            CommentViewHolder(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvCommentName);
                tvMsg = v.findViewById(R.id.tvCommentMessage);
                tvTime = v.findViewById(R.id.tvCommentTime);
            }
        }
    }
}
