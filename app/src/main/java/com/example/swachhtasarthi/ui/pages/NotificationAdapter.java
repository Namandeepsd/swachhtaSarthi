package com.example.swachhtasarthi.ui.pages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.Notification;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<Notification> notifications;
    private final OnInviteActionListener onInviteActionListener;

    public NotificationAdapter(List<Notification> notifications, OnInviteActionListener onInviteActionListener) {
        this.notifications = notifications;
        this.onInviteActionListener = onInviteActionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_notification_display, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(notification.getTime());
        holder.ivIcon.setImageResource(notification.getIconResId());
        if (notification.getReason() != null && !notification.getReason().trim().isEmpty()) {
            holder.tvReason.setVisibility(View.VISIBLE);
            holder.tvReason.setText(notification.getReason());
        } else {
            holder.tvReason.setVisibility(View.GONE);
        }

        if (notification.isInviteActionable()) {
            holder.actionsContainer.setVisibility(View.VISIBLE);
            holder.tvStatus.setVisibility(View.GONE);
            holder.btnAccept.setEnabled(true);
            holder.btnDecline.setEnabled(true);
            holder.btnAccept.setOnClickListener(v -> {
                holder.btnAccept.setEnabled(false);
                holder.btnDecline.setEnabled(false);
                onInviteActionListener.onAccept(notification, holder.getBindingAdapterPosition());
            });
            holder.btnDecline.setOnClickListener(v -> {
                holder.btnAccept.setEnabled(false);
                holder.btnDecline.setEnabled(false);
                onInviteActionListener.onDecline(notification, holder.getBindingAdapterPosition());
            });
        } else {
            holder.actionsContainer.setVisibility(View.GONE);
            String status = notification.getInviteStatus();
            if (status != null && !status.trim().isEmpty()) {
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Invite " + status);
            } else {
                holder.tvStatus.setVisibility(View.GONE);
            }
            holder.btnAccept.setEnabled(true);
            holder.btnDecline.setEnabled(true);
            holder.btnAccept.setOnClickListener(null);
            holder.btnDecline.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvTime, tvReason;
        View actionsContainer;
        MaterialButton btnAccept, btnDecline;
        TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            tvReason = itemView.findViewById(R.id.tvNotificationReason);
            actionsContainer = itemView.findViewById(R.id.layoutInviteActions);
            btnAccept = itemView.findViewById(R.id.btnAcceptInvite);
            btnDecline = itemView.findViewById(R.id.btnDeclineInvite);
            tvStatus = itemView.findViewById(R.id.tvInviteStatus);
        }
    }

    public interface OnInviteActionListener {
        void onAccept(Notification notification, int position);
        void onDecline(Notification notification, int position);
    }
}
