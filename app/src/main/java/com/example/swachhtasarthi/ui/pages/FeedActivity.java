package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.model.FeedItem;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth authManager = new FirebaseManagerAndAuth();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!authManager.isUserLoggedIn()) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_feed);

        // Setup Bottom Navigation
        BottomTrayHandler bottomTrayHandler = new BottomTrayHandler(this);
        bottomTrayHandler.setup();

        RecyclerView rvFeed = findViewById(R.id.rvFeed);
        rvFeed.setLayoutManager(new LinearLayoutManager(this));

        List<FeedItem> feedItems = new ArrayList<>();
        feedItems.add(new FeedItem("Ravi Gupta", "Rewari, Haryana", "PENDING", "3B Kapriwas, Haryana... Waste accumulation near park area.", R.drawable.profile_image, R.drawable.login_signup_hero_img));
        feedItems.add(new FeedItem("Preeya", "Gurugram, Sector 44", "RESOLVED", "Cleanliness drive conducted near Metro station.", R.drawable.profile_image, R.drawable.login_signup_hero_img));
        feedItems.add(new FeedItem("Namandeep", "Alwar Bypass", "IN PROGRESS", "Garbage collection scheduled for today evening.", R.drawable.profile_image, R.drawable.login_signup_hero_img));

        FeedAdapter adapter = new FeedAdapter(feedItems);
        rvFeed.setAdapter(adapter);
    }
}
