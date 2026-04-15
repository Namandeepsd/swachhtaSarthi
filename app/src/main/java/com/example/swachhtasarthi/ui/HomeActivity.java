package com.example.swachhtasarthi.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.AuthManager;
import com.example.swachhtasarthi.service.Report;
import com.example.swachhtasarthi.ui.auth.SignupActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    AuthManager authManager = new AuthManager();
    RecyclerView rvReports;
    ReportAdapter reportAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!authManager.isUserLoggedIn()) {
            Intent intent = new Intent(HomeActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        rvReports = findViewById(R.id.rvReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        // Adding Dummy Data for display
        List<Report> reports = new ArrayList<>();
        reports.add(new Report("Alwar Bypass", "Overflowing waste bins near main road.", "Sector 6, Bhiwadi", "5 mins ago", "PENDING", R.drawable.login_signup_hero_img));
        reports.add(new Report("Sidhrawali, Dharuhera", "Illegal dumping of construction materials.", "North Park", "2 hours ago", "IN PROGRESS", R.drawable.login_signup_hero_img));
        reports.add(new Report("Nai Wali, Rewari", "Overflowed Garbage Bins collected.", "Sabzi Mandi", "Yesterday", "RESOLVED", R.drawable.login_signup_hero_img));

        reportAdapter = new ReportAdapter(reports);
        rvReports.setAdapter(reportAdapter);
    }
}
