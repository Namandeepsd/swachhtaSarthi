package com.example.swachhtasarthi.ui.pages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.FeedItem;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private final List<FeedItem> feedItems;

    public FeedAdapter(List<FeedItem> feedItems) {
        this.feedItems = feedItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        holder.ivUserProfile.setImageResource(item.getUserProfileImage());
        holder.ivPostImage.setImageResource(item.getPostImage());
        
        // Dynamic status tag styling
        if ("RESOLVED".equalsIgnoreCase(item.getStatus())) {
            holder.tvStatusTag.setBackgroundResource(R.drawable.bg_pending_tag); // Reuse or create bg_resolved_tag
            holder.tvStatusTag.setTextColor(0xFF10B981); // Emerald Green
        }
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserProfile, ivPostImage;
        TextView tvUserName, tvLocation, tvStatusTag, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatusTag = itemView.findViewById(R.id.tvStatusTag);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}
