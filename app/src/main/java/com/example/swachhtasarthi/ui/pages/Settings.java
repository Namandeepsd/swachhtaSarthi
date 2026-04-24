package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.LoginActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;

public class Settings extends AppCompatActivity {

    private final FirebaseManagerAndAuth authManager = new FirebaseManagerAndAuth();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup Bottom Navigation
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);
        bottomTrayHandler.setup();

        TextView tvReportProblem = findViewById(R.id.tvReportProblem);
        TextView tvLogout = findViewById(R.id.tvLogout);

        if (tvReportProblem != null) {
            tvReportProblem.setOnClickListener(v -> {
                Toast.makeText(this, "Report feature coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {
                authManager.logout();
                Intent intent = new Intent(Settings.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
