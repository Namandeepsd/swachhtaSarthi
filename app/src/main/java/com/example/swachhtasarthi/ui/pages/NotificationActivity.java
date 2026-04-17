package com.example.swachhtasarthi.ui.pages;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        List<Notification> notifications = new ArrayList<>();
        notifications.add(new Notification("Ravi Gupta Issue resolved", "3h ago", R.drawable.notification));
        notifications.add(new Notification("Someone near you posted.", "4h ago", R.drawable.notification));
        notifications.add(new Notification("New NGO registered near you!", "5h ago", R.drawable.notification));
        notifications.add(new Notification("New follow request.", "5h ago", R.drawable.notification));
        notifications.add(new Notification("Your Account is verified successfully", "12h ago", R.drawable.notification));

        NotificationAdapter adapter = new NotificationAdapter(notifications);
        rvNotifications.setAdapter(adapter);
    }
}
