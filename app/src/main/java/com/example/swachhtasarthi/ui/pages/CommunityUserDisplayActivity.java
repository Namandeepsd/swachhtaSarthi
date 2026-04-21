package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CommunityUserDisplayActivity extends AppCompatActivity {

    private LinearLayout layoutNoCommunity;
    private View layoutCommunityDisplay;
    private ProgressBar progressBar;
    private MaterialButton btnCreateCommunity;
    
    private ImageView ivChannelBanner, ivCommunityProfile;
    private TextView tvCommunityName, tvCommunityEmail, tvCommunityDesc;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_user_display);

        uid = FirebaseAuth.getInstance().getUid();
        
        initViews();
        
        // Setup Bottom Navigation
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);
        bottomTrayHandler.setup();
        
        checkUserCommunity();
    }

    private void initViews() {
        layoutNoCommunity = findViewById(R.id.layoutNoCommunity);
        layoutCommunityDisplay = findViewById(R.id.layoutCommunityDisplay);
        progressBar = findViewById(R.id.progressBar);
        btnCreateCommunity = findViewById(R.id.btnCreateCommunity);

        ivChannelBanner = findViewById(R.id.ivChannelBanner);
        ivCommunityProfile = findViewById(R.id.ivCommunityProfile);
        tvCommunityName = findViewById(R.id.tvCommunityName);
        tvCommunityEmail = findViewById(R.id.tvCommunityEmail);
        tvCommunityDesc = findViewById(R.id.tvCommunityDesc);

        btnCreateCommunity.setOnClickListener(v -> {
            startActivity(new Intent(this, CommunityDetailsActivity.class));
        });
    }

    private void checkUserCommunity() {
        if (uid == null) return;

        progressBar.setVisibility(View.VISIBLE);
        db.collection("community")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    if (doc.exists()) {
                        showCommunityData(doc.getString("name"), 
                                         doc.getString("email"), 
                                         doc.getString("description"),
                                         doc.getString("channelImage"),
                                         doc.getString("profileImage"));
                    } else {
                        layoutNoCommunity.setVisibility(View.VISIBLE);
                        layoutCommunityDisplay.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Permission Denied: Ensure Firestore rules allow access to 'community' collection.", Toast.LENGTH_LONG).show();
                });
    }

    private void showCommunityData(String name, String email, String desc, String channelImg, String profileImg) {
        layoutNoCommunity.setVisibility(View.GONE);
        layoutCommunityDisplay.setVisibility(View.VISIBLE);

        tvCommunityName.setText(name);
        tvCommunityEmail.setText(email);
        tvCommunityDesc.setText(desc);

        // Using Glide for reliable image loading from URLs
        if (channelImg != null && !channelImg.isEmpty()) {
            Glide.with(this).load(channelImg).placeholder(R.drawable.login_signup_hero_img).into(ivChannelBanner);
        }
        if (profileImg != null && !profileImg.isEmpty()) {
            Glide.with(this).load(profileImg).placeholder(R.drawable.profile_image).circleCrop().into(ivCommunityProfile);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserCommunity();
    }
}
