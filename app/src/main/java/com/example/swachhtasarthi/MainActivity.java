package com.example.swachhtasarthi;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.pages.HomeActivity;
import com.example.swachhtasarthi.ui.auth.SignupActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();

        if (firebaseManagerAndAuth.isUserLoggedIn()) {
            startActivity(new Intent(this, HomeActivity.class));
        } else {
            startActivity(new Intent(this, SignupActivity.class));
        }
        finish();
    }
}