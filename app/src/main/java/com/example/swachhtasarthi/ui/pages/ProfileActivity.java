package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;

public class ProfileActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!firebaseManagerAndAuth.isUserLoggedIn()) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_settings);
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);
        bottomTrayHandler.setup();
    }
}
