package com.example.swachhtasarthi.ui.pages;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.R;
import com.google.android.material.button.MaterialButton;

public class VolunteerActivity extends AppCompatActivity {

    private String reportId, reportOwnerId, city, address, description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer);

        reportId = getIntent().getStringExtra("reportId");
        reportOwnerId = getIntent().getStringExtra("reportOwnerId");
        city = getIntent().getStringExtra("city");
        address = getIntent().getStringExtra("address");
        description = getIntent().getStringExtra("description");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvAddress = findViewById(R.id.tvAddress);
        MaterialButton btnVolunteer = findViewById(R.id.btnVolunteer);

        tvTitle.setText(city != null ? city : "Report Details");
        tvDescription.setText(description != null ? description : "No description provided");
        tvAddress.setText(address != null ? address : "No address provided");

        btnVolunteer.setOnClickListener(v -> {
            Toast.makeText(this, "Thank you for volunteering! Feature coming soon.", Toast.LENGTH_SHORT).show();
        });
    }
}
