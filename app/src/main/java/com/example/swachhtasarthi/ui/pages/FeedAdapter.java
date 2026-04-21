package com.example.swachhtasarthi.ui.pages;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private final List<FeedItem> feedItems;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private Context appContext;

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
        if (profileUrl != null && !profileUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(profileUrl)
                    .placeholder(item.getUserProfileImage())
                    .error(item.getUserProfileImage())
                    .into(holder.ivUserProfile);
        } else {
            holder.ivUserProfile.setImageResource(item.getUserProfileImage());
        }

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(item.getFallbackPostImage())
                    .error(item.getFallbackPostImage())
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setImageResource(item.getFallbackPostImage());
        }

        holder.tvUpvoteCount.setText(String.valueOf(item.getUpvoteCount()));
        holder.tvCommentCount.setText(String.valueOf(item.getCommentCount()));

        bindDescriptionReadMore(holder, item);
        styleStatusTag(holder, item.getStatus());
        applyUpvoteIconState(holder, item.isUpvotedByCurrentUser());

        holder.ivShare.setOnClickListener(v -> shareReport(holder, item));
        holder.ivContribute.setOnClickListener(v -> openVolunteerScreen(holder, item));
        holder.ivUpvote.setOnClickListener(v -> toggleUpvote(holder, item));
        holder.ivComment.setOnClickListener(v -> openCommentDialog(holder, item));
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
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
        String shareText = "Swachhta Sarthi Report\n"
                + "Location: " + safe(item.getLocation()) + "\n"
                + "Status: " + safe(item.getStatus()) + "\n"
                + "Details: " + safe(item.getDescription());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        holder.itemView.getContext().startActivity(Intent.createChooser(shareIntent, "Share report via"));
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

        boolean isUpvotedNow = !item.isUpvotedByCurrentUser();
        long currentUpvotes = item.getUpvoteCount();
        long newCount = isUpvotedNow ? currentUpvotes + 1 : Math.max(0, currentUpvotes - 1);

        // Update UI immediately for responsiveness
        item.setUpvotedByCurrentUser(isUpvotedNow);
        item.setUpvoteCount(newCount);
        holder.tvUpvoteCount.setText(String.valueOf(newCount));
        applyUpvoteIconState(holder, isUpvotedNow);

        DocumentReference reportRef = firestore.collection("reports").document(item.getReportId());
        // Use TOP-LEVEL collection for visibility as requested
        String likeDocId = item.getReportId() + "_" + currentUid;
        DocumentReference likeRef = firestore.collection("likes").document(likeDocId);

        if (isUpvotedNow) {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("reportId", item.getReportId());
            likeData.put("userId", currentUid);
            likeData.put("createdAt", System.currentTimeMillis());
            
            likeRef.set(likeData);
            reportRef.update(
                "upvoteCount", FieldValue.increment(1),
                "likes", FieldValue.increment(1)
            );
        } else {
            likeRef.delete();
            reportRef.update(
                "upvoteCount", FieldValue.increment(-1),
                "likes", FieldValue.increment(-1)
            );
        }
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

        // Load from TOP-LEVEL collection
        loadComments(item.getReportId(), list, adapter, tvNo);

        AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        btn.setOnClickListener(v -> {
            String msg = et.getText().toString().trim();
            if (msg.isEmpty()) return;
            et.setText("");
            postComment(item, currentUid, msg, () -> {
                // Update UI count
                long newCount = item.getCommentCount() + 1;
                item.setCommentCount(newCount);
                holder.tvCommentCount.setText(String.valueOf(newCount));
                loadComments(item.getReportId(), list, adapter, tvNo);
            });
        });
        dialog.show();
    }

    private void loadComments(String reportId, List<CommentItem> list, CommentsPopupAdapter adapter, TextView tvNo) {
        // Query TOP-LEVEL collection filtered by reportId
        firestore.collection("comments")
                .whereEqualTo("reportId", reportId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    list.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        list.add(new CommentItem(
                                safe(doc.getString("userName")),
                                safe(doc.getString("message")),
                                toLong(doc.get("createdAt"))
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    tvNo.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    // Handle case where index might be missing or other query errors
                    tvNo.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void postComment(FeedItem item, String uid, String msg, Runnable onSuccess) {
        firestore.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String fName = userDoc.getString("firstName");
            String lName = userDoc.getString("lastName");
            String uName = ((fName == null ? "" : fName) + " " + (lName == null ? "" : lName)).trim();
            if (uName.isEmpty()) uName = "Community Member";

            DocumentReference reportRef = firestore.collection("reports").document(item.getReportId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("reportId", item.getReportId());
            data.put("userId", uid);
            data.put("userName", uName);
            data.put("message", msg);
            data.put("createdAt", System.currentTimeMillis());

            // Add to TOP-LEVEL collection
            firestore.collection("comments").add(data).addOnSuccessListener(unused -> {
                // Increment count on report
                reportRef.update("commentCount", FieldValue.increment(1));
                onSuccess.run();
            });
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserProfile, ivPostImage, ivContribute, ivUpvote, ivComment, ivShare;
        TextView tvUserName, tvLocation, tvStatusTag, tvDescription, tvReadMore, tvUpvoteCount, tvCommentCount;
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
