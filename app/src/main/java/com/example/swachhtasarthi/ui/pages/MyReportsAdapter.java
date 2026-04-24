package com.example.swachhtasarthi.ui.pages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.MyReports;
import com.bumptech.glide.Glide;

import java.util.List;

public class MyReportsAdapter extends RecyclerView.Adapter<MyReportsAdapter.ReportViewHolder> {

    private List<MyReports> myReportsList;

    public MyReportsAdapter(List<MyReports> myReportsList) {
        this.myReportsList = myReportsList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.component_reports, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        MyReports myReports = myReportsList.get(position);
        holder.tvTitle.setText(myReports.getTitle());
        holder.tvDescription.setText(toSingleLineEllipsized(myReports.getDescription(), 55));

        String shortLocation = toSingleLineEllipsized(myReports.getLocation(), 32);
        holder.tvLocationTime.setText(shortLocation + " • " + myReports.getTime());
        String status = myReports.getStatus() == null ? "PENDING" : myReports.getStatus();
        holder.tvStatus.setText(status);
        
        if (myReports.getImageUrl() != null && !myReports.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(myReports.getImageUrl())
                    .placeholder(R.drawable.login_signup_hero_img)
                    .error(R.drawable.login_signup_hero_img)
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.login_signup_hero_img);
        }
        
        if ("PENDING".equalsIgnoreCase(status)) {
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag);
        } else if ("IN PROGRESS".equalsIgnoreCase(status)) {
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag); 
        } else {
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag);
        }
    }

    private String toSingleLineEllipsized(String text, int maxChars) {
        if (text == null) return "";

        String singleLine = text.replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (singleLine.length() <= maxChars) {
            return singleLine;
        }

        return singleLine.substring(0, Math.max(0, maxChars - 3)).trim() + "...";
    }

    @Override
    public int getItemCount() {
        return myReportsList.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvDescription, tvLocationTime, tvStatus;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivReportImage);
            tvTitle = itemView.findViewById(R.id.tvReportTitle);
            tvDescription = itemView.findViewById(R.id.tvReportDescription);
            tvLocationTime = itemView.findViewById(R.id.tvReportLocationTime);
            tvStatus = itemView.findViewById(R.id.tvStatusTag);
        }
    }
}
