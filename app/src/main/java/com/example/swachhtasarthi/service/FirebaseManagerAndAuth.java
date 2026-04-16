package com.example.swachhtasarthi.service;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseManagerAndAuth {

    private FirebaseAuth mAuth;
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    public FirebaseManagerAndAuth(){
        mAuth = FirebaseAuth.getInstance();
    }

    //Signup User
    public void signup(String email, String password, OnCompleteListener<AuthResult> listner){
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(listner);
    }

    //Login User
    public void login (String email, String password, OnCompleteListener<AuthResult> listener){
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(listener);
    }
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // 🚪 LOGOUT
    public void logout() {
        mAuth.signOut();
    }



}
