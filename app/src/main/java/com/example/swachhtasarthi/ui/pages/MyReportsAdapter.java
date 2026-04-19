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
        holder.tvDescription.setText(myReports.getDescription());
        holder.tvLocationTime.setText(myReports.getLocation() + " • " + myReports.getTime());
        holder.tvStatus.setText(myReports.getStatus());
        holder.ivImage.setImageResource(myReports.getImageResId());
        
        // Update status background based on text if needed, but for now just text
        if (myReports.getStatus().equalsIgnoreCase("PENDING")) {
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag);
        } else if (myReports.getStatus().equalsIgnoreCase("IN PROGRESS")) {
             // You could add more drawables here
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag); 
        } else {
             holder.tvStatus.setBackgroundResource(R.drawable.bg_pending_tag);
        }
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
