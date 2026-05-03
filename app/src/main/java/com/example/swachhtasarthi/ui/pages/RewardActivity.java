package com.example.swachhtasarthi.ui.pages;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.AppNotificationService;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RewardActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth auth = new FirebaseManagerAndAuth();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private TextView tvScoreValue;
    private TextView tvPointsValue;
    private TextView tvCommunityPointsValue;
    private TextView tvScoreBand;
    private TextView tvNextMilestone;
    private LinearProgressIndicator scoreProgress;
    private ProgressBar loading;
    private RecyclerView rvActivities;
    private RecyclerView rvRewards;
    private MaterialButton btnShareImpact;
    private MaterialButton btnRedeemHelp;

    private ActivityAdapter activityAdapter;
    private RewardAdapter rewardAdapter;
    private RewardComputation latestComputation;
    private final AppNotificationService appNotificationService = new AppNotificationService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!auth.isUserLoggedIn()) {
            startActivity(new android.content.Intent(this, SignupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_reward);
        new BottomTrayHandler(this).setup();
        initViews();
        setupStaticActions();
        loadRewardData();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBackReward);
        ImageView btnScoreInfo = findViewById(R.id.btnScoreInfo);
        btnBack.setOnClickListener(v -> finish());
        btnScoreInfo.setOnClickListener(v -> showScoringFormulaDialog());

        tvScoreValue = findViewById(R.id.tvScoreValue);
        tvPointsValue = findViewById(R.id.tvPointsValue);
        tvCommunityPointsValue = findViewById(R.id.tvCommunityPointsValue);
        tvScoreBand = findViewById(R.id.tvScoreBand);
        tvNextMilestone = findViewById(R.id.tvNextMilestone);
        scoreProgress = findViewById(R.id.scoreProgress);
        loading = findViewById(R.id.progressRewards);
        rvActivities = findViewById(R.id.rvActivityBreakdown);
        rvRewards = findViewById(R.id.rvRewardOffers);
        btnShareImpact = findViewById(R.id.btnShareImpact);
        btnRedeemHelp = findViewById(R.id.btnRedeemHelp);

        activityAdapter = new ActivityAdapter(new ArrayList<>());
        rewardAdapter = new RewardAdapter(new ArrayList<>(), this::handleRewardClick);

        rvActivities.setLayoutManager(new LinearLayoutManager(this));
        rvActivities.setAdapter(activityAdapter);

        rvRewards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRewards.setAdapter(rewardAdapter);
    }

    private void setupStaticActions() {
        btnShareImpact.setOnClickListener(v -> shareImpactSnapshot());
        btnRedeemHelp.setOnClickListener(v -> showRedemptionHelpDialog());
    }

    private void loadRewardData() {
        String uid = auth.getCurrentUserUid();
        if (uid == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        Tasks.whenAllSuccess(
                db.collection("users").document(uid).get(),
                db.collection("reports").whereEqualTo("userId", uid).whereEqualTo("reportByType", "individual").get(),
                db.collection("reports").whereEqualTo("postedBy", uid).whereEqualTo("reportByType", "community").get(),
                db.collection("comments").whereEqualTo("userId", uid).get(),
                db.collection("likes").whereEqualTo("userId", uid).get(),
                db.collection("volunteerActions").whereEqualTo("userId", uid).get()
        ).addOnSuccessListener(results -> {
            DocumentSnapshot userDoc = (DocumentSnapshot) results.get(0);
            QuerySnapshot indReports = (QuerySnapshot) results.get(1);
            QuerySnapshot commReports = (QuerySnapshot) results.get(2);
            QuerySnapshot comments = (QuerySnapshot) results.get(3);
            QuerySnapshot likes = (QuerySnapshot) results.get(4);
            QuerySnapshot volunteers = (QuerySnapshot) results.get(5);

            RewardComputation rewardComputation = computeRewards(userDoc, indReports, commReports, comments, likes, volunteers);
            bindRewardState(rewardComputation);
            showLoading(false);
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Could not load reward data", Toast.LENGTH_SHORT).show();
        });
    }

    private RewardComputation computeRewards(DocumentSnapshot userDoc,
                                            QuerySnapshot indReports,
                                            QuerySnapshot commReports,
                                            QuerySnapshot comments,
                                            QuerySnapshot likes,
                                            QuerySnapshot volunteers) {
        int reportsCount = indReports.size();
        int commReportsCount = commReports.size();
        int commentsCount = comments.size();
        int likesCount = likes.size();
        int volunteerCount = volunteers.size();

        int profileBonus = profileCompletenessBonus(userDoc);
        int engagementBlendBonus = engagementBlendBonus(reportsCount + commReportsCount, commentsCount, likesCount, volunteerCount);

        List<ActivityMetric> metrics = new ArrayList<>();
        metrics.add(new ActivityMetric("Volunteering", volunteerCount, 120, "High-impact contribution"));
        metrics.add(new ActivityMetric("Individual Reports", reportsCount, 70, "Personal issue reports"));
        metrics.add(new ActivityMetric("Community Reports", commReportsCount, 100, "Reports through your community"));
        metrics.add(new ActivityMetric("Comment Engagement", commentsCount, 15, "Helpful civic discussions"));
        metrics.add(new ActivityMetric("Community Support", likesCount, 8, "Likes and upvotes"));
        metrics.add(new ActivityMetric("Profile Trust Bonus", profileBonus > 0 ? 1 : 0, profileBonus, "Profile completion"));
        metrics.add(new ActivityMetric("Consistency Bonus", engagementBlendBonus > 0 ? 1 : 0, engagementBlendBonus, "Balanced engagement"));

        int totalPoints = 0;
        int commPoints = 0;
        for (ActivityMetric m : metrics) {
            int p = m.totalPoints();
            totalPoints += p;
            if (m.title.equals("Community Reports")) {
                commPoints += p;
            }
        }

        // CIBIL-style bounded score [0, 999]
        int score = 300;
        score += Math.min(200, reportsCount * 20);
        score += Math.min(150, commReportsCount * 25);
        score += Math.min(260, volunteerCount * 35);
        score += Math.min(100, commentsCount * 6);
        score += Math.min(80, likesCount * 3);
        score += profileBonus > 0 ? 80 : 0;
        score += Math.min(100, engagementBlendBonus);
        score = Math.max(0, Math.min(999, score));

        List<RewardOffer> offers = new ArrayList<>();
        offers.add(RewardOffer.lockedTemplate("Rs 50 Grocery Coupon", "Coupon", 350, 250));
        offers.add(RewardOffer.lockedTemplate("5% Utility Bill Cashback", "Cashback", 500, 480));
        offers.add(RewardOffer.lockedTemplate("Rs 150 Food Voucher", "Coupon", 650, 800));
        offers.add(RewardOffer.lockedTemplate("Rs 200 Marketplace Coupon", "Coupon", 760, 1100));
        offers.add(RewardOffer.lockedTemplate("10% Transport Cashback", "Cashback", 850, 1500));
        offers.add(RewardOffer.lockedTemplate("Rs 500 Mega Coupon", "Coupon", 930, 2100));

        for (RewardOffer offer : offers) {
            offer.unlocked = score >= offer.requiredScore && totalPoints >= offer.requiredPoints;
        }

        return new RewardComputation(score, totalPoints, commPoints, scoreBand(score), metrics, offers);
    }

    private int profileCompletenessBonus(DocumentSnapshot userDoc) {
        if (userDoc == null || !userDoc.exists()) return 0;
        boolean hasFirstName = hasText(userDoc.getString("firstName"));
        boolean hasLastName = hasText(userDoc.getString("lastName"));
        boolean hasEmail = hasText(userDoc.getString("email"));
        boolean hasPhone = hasText(userDoc.getString("phone"));
        boolean hasCity = hasText(userDoc.getString("city"));
        int count = 0;
        if (hasFirstName) count++;
        if (hasLastName) count++;
        if (hasEmail) count++;
        if (hasPhone) count++;
        if (hasCity) count++;
        if (count >= 5) return 75;
        if (count >= 4) return 50;
        if (count >= 3) return 25;
        return 0;
    }

    private int engagementBlendBonus(int totalReports, int comments, int likes, int volunteers) {
        int categoriesActive = 0;
        if (totalReports > 0) categoriesActive++;
        if (comments > 0) categoriesActive++;
        if (likes > 0) categoriesActive++;
        if (volunteers > 0) categoriesActive++;
        if (categoriesActive <= 1) return 0;
        if (categoriesActive == 2) return 25;
        if (categoriesActive == 3) return 55;
        return 90;
    }

    private void bindRewardState(RewardComputation data) {
        latestComputation = data;
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.getDefault());
        tvScoreValue.setText(String.format(Locale.getDefault(), "%03d", data.score));
        tvPointsValue.setText(nf.format(data.points));
        tvCommunityPointsValue.setText(nf.format(data.communityPoints));
        tvScoreBand.setText(data.band);
        scoreProgress.setMax(1000);
        scoreProgress.setProgressCompat(data.score, true);

        RewardOffer next = nextLockedReward(data.offers);
        if (next == null) {
            tvNextMilestone.setText("All rewards unlocked. Keep your score strong!");
        } else {
            int scoreLeft = Math.max(0, next.requiredScore - data.score);
            int pointsLeft = Math.max(0, next.requiredPoints - data.points);
            tvNextMilestone.setText("Next: " + next.title + "  •  +" + scoreLeft + " score, +" + pointsLeft + " pts");
        }

        activityAdapter.update(data.metrics);
        rewardAdapter.update(data.offers);
        syncScoreProgressAndNotify(data);
    }

    private void syncScoreProgressAndNotify(RewardComputation data) {
        String uid = auth.getCurrentUserUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            long previousBest = 0L;
            Object value = userDoc.get("rewardScoreBest");
            if (value instanceof Number) previousBest = ((Number) value).longValue();

            Map<String, Object> updates = new HashMap<>();
            updates.put("rewardScoreCurrent", data.score);
            updates.put("rewardPointsCurrent", data.points);
            updates.put("rewardCommunityPointsCurrent", data.communityPoints);
            updates.put("rewardBandCurrent", data.band);

            if (data.score > previousBest) {
                updates.put("rewardScoreBest", data.score);
                String reason = bestReasonSummary(data.metrics);
                appNotificationService.sendToUser(
                        uid,
                        "score_increase",
                        "Score Increased",
                        "Your civic score improved to " + data.score + ".",
                        "Reason: " + reason,
                        new HashMap<>()
                );
            }
            db.collection("users").document(uid).update(updates);
        });
    }

    private String bestReasonSummary(List<ActivityMetric> metrics) {
        ActivityMetric best = null;
        for (ActivityMetric m : metrics) {
            if (m.totalPoints() <= 0) continue;
            if (best == null || m.totalPoints() > best.totalPoints()) {
                best = m;
            }
        }
        if (best == null) return "Profile and engagement consistency";
        return best.title + " contributed the most";
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRewardData();
    }

    private RewardOffer nextLockedReward(List<RewardOffer> offers) {
        for (RewardOffer offer : offers) {
            if (!offer.unlocked) return offer;
        }
        return null;
    }

    private void showLoading(boolean isLoading) {
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvActivities.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        rvRewards.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private String scoreBand(int score) {
        if (score >= 900) return "Legendary";
        if (score >= 800) return "Exceptional";
        if (score >= 700) return "Strong";
        if (score >= 600) return "Promising";
        if (score >= 450) return "Growing";
        return "Starter";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void shareImpactSnapshot() {
        if (latestComputation == null) {
            Toast.makeText(this, "Loading your impact details", Toast.LENGTH_SHORT).show();
            return;
        }
        String shareText = "My Swachhta Sarthi impact:\n"
                + "Score: " + latestComputation.score + "/999 (" + latestComputation.band + ")\n"
                + "Total Points: " + latestComputation.points + "\n"
                + "Community Points: " + latestComputation.communityPoints + "\n"
                + "I am improving my city through reports, volunteering and community engagement.";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share your impact"));
    }

    private void showRedemptionHelpDialog() {
        String message = "How redemption works:\n\n"
                + "1) Earn points via individual/community reporting, volunteering, comments and support.\n"
                + "2) Improve score using consistent, balanced activity.\n"
                + "3) A reward unlocks only when both score and points requirements are met.\n"
                + "4) Tap an unlocked reward to view and copy your code.\n\n"
                + "Current reward list is hardcoded for demo.";
        new AlertDialog.Builder(this)
                .setTitle("Reward Redemption Guide")
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showScoringFormulaDialog() {
        String formula = "Transparency: score increments\n\n"
                + "Base Score = 300\n"
                + "Ind. Report score = +20 each (max +200)\n"
                + "Comm. Report score = +25 each (max +150)\n"
                + "Volunteer score = +35 each (max +260)\n"
                + "Comment score = +6 each (max +100)\n"
                + "Support score (likes) = +3 each (max +80)\n"
                + "Profile trust bonus = +80\n"
                + "Consistency bonus = up to +90\n\n"
                + "Final score is clamped between 0 and 999.\n\n"
                + "Point increments\n"
                + "Volunteering: 120 pts each\n"
                + "Comm. reporting: 100 pts each\n"
                + "Ind. reporting: 70 pts each\n"
                + "Comment engagement: 15 pts each\n"
                + "Community support: 8 pts each";
        new AlertDialog.Builder(this)
                .setTitle("How Your Score Is Calculated")
                .setMessage(formula)
                .setPositiveButton("Close", null)
                .show();
    }

    private void handleRewardClick(RewardOffer reward) {
        if (!reward.unlocked) {
            int scoreLeft = latestComputation == null ? 0 : Math.max(0, reward.requiredScore - latestComputation.score);
            int pointsLeft = latestComputation == null ? 0 : Math.max(0, reward.requiredPoints - latestComputation.points);
            String message = "Unlock requirements:\n"
                    + "- Score needed: " + reward.requiredScore + " (need +" + scoreLeft + ")\n"
                    + "- Points needed: " + reward.requiredPoints + " (need +" + pointsLeft + ")";
            new AlertDialog.Builder(this)
                    .setTitle(reward.title)
                    .setMessage(message)
                    .setPositiveButton("Okay", null)
                    .show();
            return;
        }

        String code = buildRewardCode(reward);
        String message = "Reward: " + reward.title + "\n"
                + "Type: " + reward.type + "\n"
                + "Code: " + code + "\n\n"
                + "Use this code at partner checkout.";
        new AlertDialog.Builder(this)
                .setTitle("Reward Unlocked")
                .setMessage(message)
                .setNegativeButton("Close", null)
                .setPositiveButton("Copy Code", (d, w) -> copyToClipboard(code))
                .show();
    }

    private String buildRewardCode(RewardOffer reward) {
        String cleaned = reward.type.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return cleaned + "-" + reward.requiredScore + "-" + reward.requiredPoints;
    }

    private void copyToClipboard(String code) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "Clipboard unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Reward Code", code));
        Toast.makeText(this, "Code copied", Toast.LENGTH_SHORT).show();
    }

    private static class RewardComputation {
        final int score;
        final int points;
        final int communityPoints;
        final String band;
        final List<ActivityMetric> metrics;
        final List<RewardOffer> offers;

        RewardComputation(int score, int points, int communityPoints, String band, List<ActivityMetric> metrics, List<RewardOffer> offers) {
            this.score = score;
            this.points = points;
            this.communityPoints = communityPoints;
            this.band = band;
            this.metrics = metrics;
            this.offers = offers;
        }
    }

    private static class ActivityMetric {
        final String title;
        final int count;
        final int pointsPerAction;
        final String subtitle;

        ActivityMetric(String title, int count, int pointsPerAction, String subtitle) {
            this.title = title;
            this.count = count;
            this.pointsPerAction = pointsPerAction;
            this.subtitle = subtitle;
        }

        int totalPoints() {
            return Math.max(0, count * pointsPerAction);
        }
    }

    private static class RewardOffer {
        final String title;
        final String type;
        final int requiredScore;
        final int requiredPoints;
        boolean unlocked;

        RewardOffer(String title, String type, int requiredScore, int requiredPoints, boolean unlocked) {
            this.title = title;
            this.type = type;
            this.requiredScore = requiredScore;
            this.requiredPoints = requiredPoints;
            this.unlocked = unlocked;
        }

        static RewardOffer lockedTemplate(String title, String type, int requiredScore, int requiredPoints) {
            return new RewardOffer(title, type, requiredScore, requiredPoints, false);
        }
    }

    private static class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.Holder> {
        private final List<ActivityMetric> items;

        ActivityAdapter(List<ActivityMetric> items) {
            this.items = items;
        }

        void update(List<ActivityMetric> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward_activity_metric, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            ActivityMetric item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvSubtitle.setText(item.subtitle);
            holder.tvCount.setText("x" + item.count);
            holder.tvPoints.setText("+" + item.totalPoints() + " pts");
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvCount, tvPoints;

            Holder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvMetricTitle);
                tvSubtitle = itemView.findViewById(R.id.tvMetricSubtitle);
                tvCount = itemView.findViewById(R.id.tvMetricCount);
                tvPoints = itemView.findViewById(R.id.tvMetricPoints);
            }
        }
    }

    private static class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.Holder> {
        interface OnRewardClick {
            void onClick(RewardOffer reward);
        }

        private final List<RewardOffer> items;
        private final OnRewardClick onClick;

        RewardAdapter(List<RewardOffer> items, OnRewardClick onClick) {
            this.items = items;
            this.onClick = onClick;
        }

        void update(List<RewardOffer> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward_offer, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            RewardOffer item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvType.setText(item.type);
            holder.tvRequirement.setText("Need " + item.requiredScore + " score & " + item.requiredPoints + " pts");
            holder.tvState.setText(item.unlocked ? "Redeem" : "Locked");
            holder.tvState.setTextColor(item.unlocked ? 0xFF135BEC : 0xFF94A3B8);
            holder.itemView.setOnClickListener(v -> onClick.onClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvType, tvRequirement, tvState;

            Holder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvRewardTitle);
                tvType = itemView.findViewById(R.id.tvRewardType);
                tvRequirement = itemView.findViewById(R.id.tvRewardRequirement);
                tvState = itemView.findViewById(R.id.tvRewardState);
            }
        }
    }
}
