package com.example.swachhtasarthi.service;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class FirebaseManagerAndAuth {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    public FirebaseManagerAndAuth() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Signup User
    public void signup(String email, String password, OnCompleteListener<AuthResult> listener) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    // Login User
    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    // Save User Data to Firestore
    public void saveUserData(String uid, Map<String, Object> userData, OnCompleteListener<Void> listener) {
        db.collection("users").document(uid).set(userData).addOnCompleteListener(listener);
    }

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public String getCurrentUserUid() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }

    public void submitReport(Map<String, Object> reportData, OnCompleteListener<DocumentReference> listener) {
        db.collection("reports").add(reportData).addOnCompleteListener(listener);
    }

    // Logout
    public void logout() {
        mAuth.signOut();
    }
}
