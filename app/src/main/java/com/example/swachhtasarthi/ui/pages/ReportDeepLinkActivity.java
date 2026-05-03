package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReportDeepLinkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent() != null ? getIntent().getData() : null;
        if (data == null) {
            finish();
            return;
        }

        String reportId = data.getQueryParameter("reportId");
        if (reportId == null || reportId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid report link", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseManagerAndAuth auth = new FirebaseManagerAndAuth();
        if (!auth.isUserLoggedIn()) {
            Intent i = new Intent(this, SignupActivity.class);
            i.putExtra("deepLinkReportId", reportId);
            startActivity(i);
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("reports").document(reportId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String ownerId = doc.getString("userId");
                    String city = doc.getString("city");
                    String address = doc.getString("address");
                    String description = doc.getString("description");

                    Intent intent = new Intent(this, VolunteerActivity.class);
                    intent.putExtra("reportId", reportId);
                    intent.putExtra("reportOwnerId", ownerId);
                    intent.putExtra("city", city);
                    intent.putExtra("address", address);
                    intent.putExtra("description", description);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to open report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}

