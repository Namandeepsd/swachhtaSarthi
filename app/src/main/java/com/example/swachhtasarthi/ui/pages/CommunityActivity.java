package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.android.material.button.MaterialButton;

public class CommunityActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!firebaseManagerAndAuth.isUserLoggedIn()) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_community);

        // Setup Bottom Navigation Tray
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);
        bottomTrayHandler.setup();

        // Initialize UI components and set listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        MaterialButton btnFollow = findViewById(R.id.btnFollow);
        MaterialButton btnMessage = findViewById(R.id.btnMessage);

        if (btnFollow != null) {
            btnFollow.setOnClickListener(v -> {
                String text = btnFollow.getText().toString();
                if (text.equalsIgnoreCase("Follow")) {
                    btnFollow.setText("Following");
                    btnFollow.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
                    Toast.makeText(this, "Following Preeya", Toast.LENGTH_SHORT).show();
                } else {
                    btnFollow.setText("Follow");
                    btnFollow.setBackgroundTintList(getColorStateList(R.color.black)); // Assuming default primary or black
                    // Note: In my XML I used #135BEC, let's use a better way or just toggle.
                }
            });
        }

        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> 
                Toast.makeText(this, "Messaging feature coming soon!", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
