package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class CommunityActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();

    private ImageView coverImage;
    private ImageView profileImage;
    private TextView tvName;
    private TextView tvLocation;
    private TextView tvBio;
    private TextView tvContribution;
    private View postCard;
    private View cardContributionRedirect;
    private MaterialButton btnFollow;
    private MaterialButton btnMessage;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

        initViews();
        setupForCurrentUser();
        fetchCommunityData();
    }

    private void initViews() {
        coverImage = findViewById(R.id.coverImage);
        profileImage = findViewById(R.id.profileImage);
        tvName = findViewById(R.id.tvName);
        tvLocation = findViewById(R.id.tvLocation);
        tvBio = findViewById(R.id.tvBio);
        tvContribution = findViewById(R.id.tvContribution);
        postCard = findViewById(R.id.postCard);
        cardContributionRedirect = findViewById(R.id.cardContributionRedirect);
        btnFollow = findViewById(R.id.btnFollow);
        btnMessage = findViewById(R.id.btnMessage);
    }

    private void setupForCurrentUser() {
        // Own community page: hide follow and contribution section
        if (btnFollow != null) {
            btnFollow.setVisibility(View.GONE);
        }

        if (tvContribution != null) {
            tvContribution.setVisibility(View.GONE);
        }

        if (postCard != null) {
            postCard.setVisibility(View.GONE);
        }

        if (cardContributionRedirect != null) {
            cardContributionRedirect.setVisibility(View.VISIBLE);
            cardContributionRedirect.setOnClickListener(v ->
                    startActivity(new Intent(this, CommunityContributionActivity.class))
            );
        }

        // Replace message with edit action
        if (btnMessage != null) {
            btnMessage.setText("Edit");
            btnMessage.setIconResource(android.R.drawable.ic_menu_edit);
            btnMessage.setOnClickListener(v -> {
                startActivity(new Intent(this, CommunityDetailsActivity.class));
            });
        }
    }

    private void fetchCommunityData() {
        String uid = firebaseManagerAndAuth.getCurrentUserUid();
        if (uid == null) {
            return;
        }

        db.collection("community")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Create your community first", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, CommunityDetailsActivity.class));
                        return;
                    }

                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String description = doc.getString("description");
                    String bannerUrl = doc.getString("channelImage");
                    String profileUrl = doc.getString("profileImage");

                    if (tvName != null) {
                        tvName.setText((name == null || name.trim().isEmpty()) ? "My Community" : name);
                    }

                    if (tvLocation != null) {
                        tvLocation.setText((email == null || email.trim().isEmpty()) ? "" : email);
                    }

                    if (tvBio != null) {
                        tvBio.setText((description == null || description.trim().isEmpty()) ? "No description added." : description);
                    }

                    if (bannerUrl != null && !bannerUrl.trim().isEmpty() && coverImage != null) {
                        Glide.with(this)
                                .load(bannerUrl)
                                .placeholder(R.drawable.cover)
                                .error(R.drawable.cover)
                                .into(coverImage);
                    }

                    if (profileUrl != null && !profileUrl.trim().isEmpty() && profileImage != null) {
                        Glide.with(this)
                                .load(profileUrl)
                                .placeholder(R.drawable.profile_image)
                                .error(R.drawable.profile_image)
                                .into(profileImage);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load community", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCommunityData();
    }
}
