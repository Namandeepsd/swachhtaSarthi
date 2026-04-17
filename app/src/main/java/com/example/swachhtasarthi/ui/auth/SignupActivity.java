package com.example.swachhtasarthi.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.pages.HomeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

public class SignupActivity extends AppCompatActivity {
    EditText etFirstName, etLastName, etEmail, etPhone, etPassword;
    AutoCompleteTextView actvGender, actvDob;
    MaterialButton btnSignup;
    TextView tvLogin;
    FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (firebaseManagerAndAuth.isUserLoggedIn()) {
            startActivity(new Intent(SignupActivity.this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_signup);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        actvGender = findViewById(R.id.actvGender);
        actvDob = findViewById(R.id.actvDob);

        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);

        setupGenderSpinner();
        setupDatePicker();

        btnSignup.setOnClickListener(v -> handleSignup());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupGenderSpinner() {
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders);
        actvGender.setAdapter(adapter);
    }

    private void setupDatePicker() {
        actvDob.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(2000, Calendar.JANUARY, 1);
            long selection = calendar.getTimeInMillis();

            CalendarConstraints constraints = new CalendarConstraints.Builder()
                    .setOpenAt(selection)
                    .setValidator(DateValidatorPointBackward.now())
                    .build();

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date of Birth")
                    .setSelection(selection)
                    .setCalendarConstraints(constraints)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selectedSelection -> {
                Calendar selectedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                selectedDate.setTimeInMillis(selectedSelection);
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                actvDob.setText(format.format(selectedDate.getTime()));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
    }

    private void handleSignup() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String gender = actvGender.getText().toString().trim();
        String dob = actvDob.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || phone.isEmpty() || gender.isEmpty()
                || dob.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!email.contains("@")) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.length() != 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        signupUser(email, password);
    }

    private void signupUser(String email, String password) {
        firebaseManagerAndAuth.signup(email, password, task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                saveUserData(uid);
            } else {
                Log.e("AUTH_ERROR", "Signup failed", task.getException());
                Toast.makeText(this, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserData(String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("firstName", etFirstName.getText().toString().trim());
        user.put("lastName", etLastName.getText().toString().trim());
        user.put("email", etEmail.getText().toString().trim());
        user.put("phone", etPhone.getText().toString().trim());
        user.put("gender", actvGender.getText().toString().trim());
        user.put("dob", actvDob.getText().toString().trim());
        user.put("score", 0);
        user.put("complaints", 0);
        user.put("resolved", 0);
        user.put("pending", 0);

        firebaseManagerAndAuth.saveUserData(uid, user, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                finish();
            } else {
                Log.e("FIRESTORE_ERROR", "Error saving data", task.getException());
                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
