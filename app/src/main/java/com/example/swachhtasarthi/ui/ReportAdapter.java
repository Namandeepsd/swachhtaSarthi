package com.example.swachhtasarthi.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.Report;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<Report> reportList;

    public ReportAdapter(List<Report> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.component_reports, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.tvTitle.setText(report.getTitle());
        holder.tvDescription.setText(report.getDescription());
        holder.tvLocationTime.setText(report.getLocation() + " • " + report.getTime());
        holder.tvStatus.setText(report.getStatus());
        holder.ivImage.setImageResource(report.getImageResId());
        
        // Update status background based on text if needed, but for now just text
        if (report.getStatus().equalsIgnoreCase("PENDING")) {
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag);
        } else if (report.getStatus().equalsIgnoreCase("IN PROGRESS")) {
             // You could add more drawables here
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag); 
        } else {
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
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
