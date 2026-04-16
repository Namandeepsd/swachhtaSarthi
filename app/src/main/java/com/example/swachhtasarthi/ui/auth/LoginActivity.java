package com.example.swachhtasarthi.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.pages.HomeActivity;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

	private EditText etEmail;
	private EditText etPassword;
	private TextView tvSignup;
	private MaterialButton btnLogin;
	private final FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (firebaseManagerAndAuth.isUserLoggedIn()) {
			startActivity(new Intent(LoginActivity.this, HomeActivity.class));
			finish();
			return;
		}

		setContentView(R.layout.activity_login);

		etEmail = findViewById(R.id.etEmail);
		etPassword = findViewById(R.id.etPassword);
		tvSignup = findViewById(R.id.tvSignup);
		btnLogin = findViewById(R.id.btnLogin);

		btnLogin.setOnClickListener(v -> handleLogin());
		tvSignup.setOnClickListener(v -> {
			startActivity(new Intent(LoginActivity.this, SignupActivity.class));
			finish();
		});
	}

	private void handleLogin() {
		String email = etEmail.getText().toString().trim();
		String password = etPassword.getText().toString().trim();

		if (email.isEmpty() || password.isEmpty()) {
			Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
			return;
		}

		firebaseManagerAndAuth.login(email, password, task -> {
			if (task.isSuccessful()) {
				Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
				startActivity(new Intent(LoginActivity.this, HomeActivity.class));
				finish();
			} else {
				Log.e("AUTH_ERROR", "Login failed", task.getException());
				String message = task.getException() != null ? task.getException().getMessage() : "Login failed";
				Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
			}
		});
	}
}
