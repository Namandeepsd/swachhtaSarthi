package com.example.swachhtasarthi.ui.components;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.ui.pages.CommunityActivity;
import com.example.swachhtasarthi.ui.pages.CommunityUserDisplayActivity;
import com.example.swachhtasarthi.ui.pages.FeedActivity;
import com.example.swachhtasarthi.ui.pages.HomeActivity;
import com.example.swachhtasarthi.ui.pages.ProfileActivity;
import com.example.swachhtasarthi.ui.pages.ReportActivity;

public class BottomTrayHandler {

    private final Activity activity;
    private final int COLOR_ACTIVE = Color.parseColor("#135BEC");
    private final int COLOR_INACTIVE = Color.parseColor("#94A3B8");

    public BottomTrayHandler(Activity activity) {
        this.activity = activity;
    }

    public void setup() {
        LinearLayout navHome = activity.findViewById(R.id.nav_home);
        LinearLayout navReport = activity.findViewById(R.id.nav_report);
        LinearLayout navCommunity = activity.findViewById(R.id.nav_community);
        LinearLayout navProfile = activity.findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> navigate(HomeActivity.class));
        if (navReport != null) navReport.setOnClickListener(v -> navigate(FeedActivity.class));
        if (navCommunity != null) navCommunity.setOnClickListener(v -> navigate(CommunityActivity.class));
        if (navProfile != null) navProfile.setOnClickListener(v -> navigate(ProfileActivity.class));

        highlightCurrentTab();
    }

    private void navigate(Class<?> targetActivity) {
        if (activity.getClass().equals(targetActivity)) {
            return;
        }

        Intent intent = new Intent(activity, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    private void highlightCurrentTab() {
        // Reset all tabs to inactive state first
        updateTabUI(R.id.imageButtonHome, R.id.textViewHome, COLOR_INACTIVE);
        updateTabUI(R.id.imageButtonReport, R.id.textViewReport, COLOR_INACTIVE);
        updateTabUI(R.id.imageButtonCommunity, R.id.textViewCommunity, COLOR_INACTIVE);
        updateTabUI(R.id.imageButtonProfile, R.id.textViewProfile, COLOR_INACTIVE);

        // Highlight the current activity tab
        if (activity instanceof HomeActivity) {
            updateTabUI(R.id.imageButtonHome, R.id.textViewHome, COLOR_ACTIVE);
        } else if (activity instanceof FeedActivity || activity instanceof ReportActivity) {
            updateTabUI(R.id.imageButtonReport, R.id.textViewReport, COLOR_ACTIVE);
        } else if (activity instanceof CommunityUserDisplayActivity || activity instanceof CommunityActivity) {
            updateTabUI(R.id.imageButtonCommunity, R.id.textViewCommunity, COLOR_ACTIVE);
        } else if (activity instanceof ProfileActivity) {
            updateTabUI(R.id.imageButtonProfile, R.id.textViewProfile, COLOR_ACTIVE);
        }
    }

    private void updateTabUI(int iconId, int textId, int color) {
        ImageView icon = activity.findViewById(iconId);
        TextView text = activity.findViewById(textId);
        
        if (icon != null) {
            icon.setColorFilter(color);
        }
        if (text != null) {
            text.setTextColor(color);
        }
    }
}
