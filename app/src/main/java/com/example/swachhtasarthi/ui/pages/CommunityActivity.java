package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.R;
import com.example.swachhtasarthi.service.AppNotificationService;
import com.example.swachhtasarthi.service.FirebaseManagerAndAuth;
import com.example.swachhtasarthi.ui.auth.SignupActivity;
import com.example.swachhtasarthi.ui.components.BottomTrayHandler;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityActivity extends AppCompatActivity {
    private final FirebaseManagerAndAuth firebaseManagerAndAuth = new FirebaseManagerAndAuth();

    private ImageView coverImage;
    private ImageView profileImage;
    private TextView tvName;
    private TextView tvLocation;
    private TextView tvBio;
    private TextView tvContribution;
    private View cardContributionRedirect;
    private MaterialButton btnFollow;
    private MaterialButton btnMessage;
    private MaterialButton btnAddMember;
    private MaterialButton btnCreateCommunity;
    private LinearLayout layoutNoCommunity;
    private View scrollView;
    private View notificationBadge;
    private View layoutFollowers, layoutMembers;

    private TextView tvFollowersCount, tvMembersCount, tvComplaintsCount;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String communityIdToShow;
    private boolean isFollowing = false;
    private boolean memberConfirmed = false;
    private final List<MemberCandidate> allUsersForInvite = new ArrayList<>();
    private final AppNotificationService appNotificationService = new AppNotificationService();

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
        setupClickListeners();
        checkImpendingNotifications();

        resolveCommunityToShowAsync(() -> {
            fetchCommunityData();
            checkIfFollowing();
        });
    }

    private void checkImpendingNotifications() {
        String uid = firebaseManagerAndAuth.getCurrentUserUid();
        if (uid == null || notificationBadge == null) return;

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null && !snapshots.isEmpty()) {
                        notificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void initViews() {
        coverImage = findViewById(R.id.coverImage);
        profileImage = findViewById(R.id.profileImage);
        tvName = findViewById(R.id.tvName);
        tvLocation = findViewById(R.id.tvLocation);
        tvBio = findViewById(R.id.tvBio);
        tvContribution = findViewById(R.id.tvContribution);
        cardContributionRedirect = findViewById(R.id.cardContributionRedirect);
        btnFollow = findViewById(R.id.btnFollow);
        btnMessage = findViewById(R.id.btnMessage);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnCreateCommunity = findViewById(R.id.btnCreateCommunity);
        layoutNoCommunity = findViewById(R.id.layoutNoCommunity);
        scrollView = findViewById(R.id.scrollView);
        notificationBadge = findViewById(R.id.notificationBadge);
        layoutFollowers = findViewById(R.id.layoutFollowers);
        layoutMembers = findViewById(R.id.layoutMembers);

        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        tvMembersCount = findViewById(R.id.tvMembersCount);
        tvComplaintsCount = findViewById(R.id.tvComplaintsCount);
    }

    private void setupClickListeners() {
        if (btnCreateCommunity != null) {
            btnCreateCommunity.setOnClickListener(v ->
                    startActivity(new Intent(this, CommunityDetailsActivity.class))
            );
        }

        if (btnFollow != null) {
            btnFollow.setOnClickListener(v -> toggleFollow());
        }

        if (btnAddMember != null) {
            btnAddMember.setOnClickListener(v -> showAddMemberDialog());
        }
        
        if (layoutMembers != null) {
            layoutMembers.setOnClickListener(v -> showMembersListDialog("Community Members", "members"));
        }
        
        if (layoutFollowers != null) {
            layoutFollowers.setOnClickListener(v -> showMembersListDialog("Followers", "followers"));
        }

        View ivNotification = findViewById(R.id.ivNotification);
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }
    }

    private void showMembersListDialog(String title, String collectionName) {
        if (communityIdToShow == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_member, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.tvSearchHint);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressUsers);
        RecyclerView rv = dialogView.findViewById(R.id.rvMemberSuggestions);
        // Hide search for now to keep it simple, or repurposed
        View searchView = dialogView.findViewById(R.id.etMemberSearch);
        if (searchView != null) searchView.setVisibility(View.GONE);
        
        tvTitle.setText(title);
        List<MemberCandidate> list = new ArrayList<>();
        MemberSuggestionAdapter adapter = new MemberSuggestionAdapter(list, candidate -> {
            // Optional: navigate to profile
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        progressBar.setVisibility(View.VISIBLE);
        db.collection("community").document(communityIdToShow).collection(collectionName)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        tvTitle.setText("No " + collectionName + " yet.");
                        return;
                    }

                    List<String> uids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snaps) {
                        // In members collection, ID is stored as 'id'. In followers, it's document ID.
                        if (collectionName.equals("members")) {
                             uids.add(doc.getString("id"));
                        } else {
                             uids.add(doc.getId());
                        }
                    }

                    fetchUserDetails(uids, list, adapter, progressBar, tvTitle, collectionName);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load " + collectionName, Toast.LENGTH_SHORT).show();
                });

        dialog.show();
    }

    private void fetchUserDetails(List<String> uids, List<MemberCandidate> list, MemberSuggestionAdapter adapter, ProgressBar pb, TextView tvTitle, String type) {
        if (uids.isEmpty()) {
            pb.setVisibility(View.GONE);
            return;
        }

        // Firestore 'in' query supports up to 30 IDs
        List<String> limitedUids = uids.subList(0, Math.min(uids.size(), 30));
        
        db.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), limitedUids)
                .get()
                .addOnSuccessListener(snaps -> {
                    pb.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot doc : snaps) {
                        list.add(new MemberCandidate(
                                doc.getId(),
                                safe(doc.getString("firstName")),
                                safe(doc.getString("lastName")),
                                safe(doc.getString("email")),
                                safe(doc.getString("phone"))
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    if (list.isEmpty()) {
                        tvTitle.setText("No active users found.");
                    } else {
                        tvTitle.setText("Showing " + list.size() + " " + type);
                    }
                })
                .addOnFailureListener(e -> {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(this, "Error fetching profiles", Toast.LENGTH_SHORT).show();
                });
    }

    private void resolveCommunityToShowAsync(Runnable onResolved) {
        String fromIntent = getIntent().getStringExtra("communityId");
        if (fromIntent != null && !fromIntent.trim().isEmpty()) {
            communityIdToShow = fromIntent.trim();
            onResolved.run();
            return;
        }
        String currentUid = firebaseManagerAndAuth.getCurrentUserUid();
        if (currentUid == null) {
            onResolved.run();
            return;
        }

        // 1. Priority: Check if user OWNS a community
        db.collection("community")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        communityIdToShow = currentUid;
                        onResolved.run();
                    } else {
                        // 2. Secondary: Check if they are a MEMBER or FOLLOWER
                        checkMembershipAndFollows(currentUid, onResolved);
                    }
                })
                .addOnFailureListener(e -> checkMembershipAndFollows(currentUid, onResolved));
    }

    private void checkMembershipAndFollows(String currentUid, Runnable onResolved) {
        db.collection("users").document(currentUid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                String joinedId = userDoc.getString("joinedCommunityId");
                if (joinedId != null && !joinedId.isEmpty()) {
                    communityIdToShow = joinedId;
                    onResolved.run();
                    return;
                }
            }
            
            db.collectionGroup("members")
                    .whereEqualTo("id", currentUid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snaps -> {
                        if (!snaps.isEmpty()) {
                            try {
                                communityIdToShow = snaps.getDocuments().get(0).getReference().getParent().getParent().getId();
                                db.collection("users").document(currentUid).set(Collections.singletonMap("joinedCommunityId", communityIdToShow), SetOptions.merge());
                                onResolved.run();
                            } catch (Exception e) {
                                searchByNotifications(currentUid, onResolved);
                            }
                        } else {
                            searchByNotifications(currentUid, onResolved);
                        }
                    })
                    .addOnFailureListener(e -> searchByNotifications(currentUid, onResolved));
        }).addOnFailureListener(e -> searchByNotifications(currentUid, onResolved));
    }

    private void searchByNotifications(String currentUid, Runnable onResolved) {
        db.collection("users").document(currentUid).collection("notifications")
                .whereIn("type", Arrays.asList("community_invite", "community_follow"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        for (QueryDocumentSnapshot doc : snaps) {
                            String commId = doc.getString("communityId");
                            if (commId != null && !commId.isEmpty()) {
                                communityIdToShow = commId;
                                onResolved.run();
                                return;
                            }
                        }
                    }
                    communityIdToShow = currentUid;
                    onResolved.run();
                })
                .addOnFailureListener(e -> {
                    communityIdToShow = currentUid;
                    onResolved.run();
                });
    }

    private void redirectToNoCommunity() {
        Intent intent = new Intent(this, CommunityUserDisplayActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupForMode(boolean exists) {
        String currentUid = firebaseManagerAndAuth.getCurrentUserUid();
        boolean isOwn = currentUid != null && currentUid.equals(communityIdToShow);

        if (!exists) {
            redirectToNoCommunity();
            return;
        }

        if (layoutNoCommunity != null) layoutNoCommunity.setVisibility(View.GONE);
        if (scrollView != null) scrollView.setVisibility(View.VISIBLE);

        if (isOwn) {
            if (btnFollow != null) btnFollow.setVisibility(View.GONE);
            if (btnAddMember != null) btnAddMember.setVisibility(View.VISIBLE);
            if (tvContribution != null) tvContribution.setVisibility(View.GONE);

            if (cardContributionRedirect != null) {
                cardContributionRedirect.setVisibility(View.VISIBLE);
                cardContributionRedirect.setOnClickListener(v ->
                        startActivity(new Intent(this, CommunityContributionActivity.class))
                );
            }

            if (btnMessage != null) {
                btnMessage.setText("Edit");
                btnMessage.setIconResource(android.R.drawable.ic_menu_edit);
                btnMessage.setOnClickListener(v -> startActivity(new Intent(this, CommunityDetailsActivity.class)));
            }
        } else {
            boolean isMember = isCurrentUserMemberOfShownCommunity();

            if (btnAddMember != null) btnAddMember.setVisibility(View.GONE);
            if (btnFollow != null) btnFollow.setVisibility(isMember ? View.GONE : View.VISIBLE);

            if (cardContributionRedirect != null) {
                cardContributionRedirect.setVisibility(isMember ? View.VISIBLE : View.GONE);
                if (isMember) {
                    cardContributionRedirect.setOnClickListener(v ->
                            startActivity(new Intent(this, CommunityContributionActivity.class))
                    );
                }
            }

            if (btnMessage != null) {
                if (isMember) {
                    btnMessage.setText("Member");
                    btnMessage.setEnabled(false);
                } else {
                    btnMessage.setText("Message");
                    btnMessage.setIconResource(android.R.drawable.ic_dialog_email);
                    btnMessage.setOnClickListener(v ->
                            Toast.makeText(this, "Messaging feature coming soon", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }
    }

    private boolean isCurrentUserMemberOfShownCommunity() {
        return memberConfirmed;
    }

    private void fetchCommunityData() {
        if (communityIdToShow == null) {
            setupForMode(false);
            return;
        }

        db.collection("community")
                .document(communityIdToShow)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        setupForMode(false);
                        return;
                    }

                    checkMemberAndThenSetup(doc.exists());

                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String description = doc.getString("description");
                    String bannerUrl = doc.getString("channelImage");
                    String profileUrl = doc.getString("profileImage");

                    refreshStatsCounts();

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
                        loadImageInto(coverImage, bannerUrl, R.drawable.cover, false);
                    }

                    if (profileUrl != null && !profileUrl.trim().isEmpty() && profileImage != null) {
                        loadImageInto(profileImage, profileUrl, R.drawable.profile_image, true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load community", Toast.LENGTH_SHORT).show();
                    setupForMode(false);
                });
    }

    private void loadImageInto(ImageView target, String value, int placeholderRes, boolean circleCrop) {
        if (target == null) return;
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            target.setImageResource(placeholderRes);
            return;
        }

        if (v.startsWith("content://")) {
            try {
                target.setImageURI(android.net.Uri.parse(v));
            } catch (Exception ignored) {
                target.setImageResource(placeholderRes);
            }
            return;
        }

        com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> req = Glide.with(this)
                .load(v)
                .placeholder(placeholderRes)
                .error(placeholderRes);
        if (circleCrop) req = req.circleCrop();
        req.into(target);
    }

    private void checkMemberAndThenSetup(boolean exists) {
        String currentUid = firebaseManagerAndAuth.getCurrentUserUid();
        if (currentUid == null || communityIdToShow == null) {
            memberConfirmed = false;
            setupForMode(exists);
            return;
        }

        if (currentUid.equals(communityIdToShow)) {
            memberConfirmed = false;
            setupForMode(exists);
            return;
        }

        db.collection("community")
                .document(communityIdToShow)
                .collection("members")
                .document(currentUid)
                .get()
                .addOnSuccessListener(memberDoc -> {
                    memberConfirmed = memberDoc.exists();
                    setupForMode(exists);
                })
                .addOnFailureListener(e -> {
                    memberConfirmed = false;
                    setupForMode(exists);
                });
    }

    private void refreshStatsCounts() {
        if (communityIdToShow == null) return;

        db.collection("community")
                .document(communityIdToShow)
                .collection("followers")
                .get()
                .addOnSuccessListener(snaps -> {
                    if (tvFollowersCount != null) tvFollowersCount.setText(String.valueOf(snaps.size()));
                });

        db.collection("community")
                .document(communityIdToShow)
                .collection("members")
                .get()
                .addOnSuccessListener(snaps -> {
                    if (tvMembersCount != null) tvMembersCount.setText(String.valueOf(snaps.size()));
                });

        db.collection("reports")
                .whereEqualTo("userId", communityIdToShow)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (tvComplaintsCount != null) tvComplaintsCount.setText(String.valueOf(snaps.size()));
                });
    }

    private void checkIfFollowing() {
        String currentUid = firebaseManagerAndAuth.getCurrentUserUid();
        if (currentUid == null || communityIdToShow == null || currentUid.equals(communityIdToShow)) return;

        db.collection("community")
                .document(communityIdToShow)
                .collection("followers")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    isFollowing = doc.exists();
                    updateFollowButtonUI();
                });
    }

    private void toggleFollow() {
        String currentUid = firebaseManagerAndAuth.getCurrentUserUid();
        if (currentUid == null || communityIdToShow == null) return;

        if (isFollowing) {
            // Unfollow
            db.collection("community")
                    .document(communityIdToShow)
                    .collection("followers")
                    .document(currentUid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        db.collection("community").document(communityIdToShow)
                                .update("followersCount", FieldValue.increment(-1));
                        isFollowing = false;
                        updateFollowButtonUI();
                        fetchCommunityData();
                    });
        } else {
            // Follow
            Map<String, Object> followerData = new HashMap<>();
            followerData.put("timestamp", FieldValue.serverTimestamp());
            db.collection("community")
                    .document(communityIdToShow)
                    .collection("followers")
                    .document(currentUid)
                    .set(followerData)
                    .addOnSuccessListener(aVoid -> {
                        db.collection("community").document(communityIdToShow)
                                .update("followersCount", FieldValue.increment(1));
                        isFollowing = true;
                        updateFollowButtonUI();
                        fetchCommunityData();
                        Map<String, Object> selfExtra = new HashMap<>();
                        selfExtra.put("communityId", communityIdToShow);
                        appNotificationService.sendToUser(
                                currentUid,
                                "community_follow",
                                "Community Followed",
                                "You are now following this community.",
                                "Reason: You tapped follow",
                                selfExtra
                        );
                    });
        }
    }

    private void updateFollowButtonUI() {
        if (isFollowing) {
            btnFollow.setText("Following");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
        } else {
            btnFollow.setText("Follow");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#135BEC")));
        }
    }

    private void showAddMemberDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_member, null, false);
        androidx.appcompat.widget.AppCompatEditText etSearch = dialogView.findViewById(R.id.etMemberSearch);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressUsers);
        TextView tvHint = dialogView.findViewById(R.id.tvSearchHint);
        TextView tvSelected = dialogView.findViewById(R.id.tvSelectedUser);
        RecyclerView rvSuggestions = dialogView.findViewById(R.id.rvMemberSuggestions);

        List<MemberCandidate> filteredUsers = new ArrayList<>();
        MemberSuggestionAdapter suggestionAdapter = new MemberSuggestionAdapter(filteredUsers, candidate -> {
            tvSelected.setText("Selected: " + candidate.displayName() + "  (" + candidate.email + ")");
            tvSelected.setVisibility(View.VISIBLE);
            etSearch.setText(candidate.email);
            etSearch.setSelection(etSearch.getText() == null ? 0 : etSearch.getText().length());
        });
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(suggestionAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Invite", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String memberInfo = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
                if (memberInfo.isEmpty()) {
                    etSearch.setError("Pick user by email, name or phone");
                    return;
                }
                addMember(memberInfo);
                dialog.dismiss();
            });
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s == null ? "" : s.toString().trim();
                filterInviteCandidates(query, filteredUsers, suggestionAdapter, tvHint, tvSelected);
            }
        });

        progressBar.setVisibility(View.VISIBLE);
        tvHint.setText("Loading users...");
        loadUsersForInvite(() -> {
            progressBar.setVisibility(View.GONE);
            tvHint.setText("Search by email, first name, last name, full name or mobile number");
            filterInviteCandidates("", filteredUsers, suggestionAdapter, tvHint, tvSelected);
        });

        dialog.show();
    }

    private void addMember(String memberInfo) {
        String currentUid = firebaseManagerAndAuth.getCurrentUserUid();
        if (currentUid == null || communityIdToShow == null) return;
        String normalizedQuery = normalize(memberInfo);
        MemberCandidate candidate = findCandidateByQuery(normalizedQuery);
        if (candidate == null) {
            Toast.makeText(this, "User not found. Invite only registered users.", Toast.LENGTH_LONG).show();
            return;
        }

        String invitedUid = candidate.uid;
        if (currentUid.equals(invitedUid)) {
            Toast.makeText(this, "You are already the owner of this community.", Toast.LENGTH_SHORT).show();
            return;
        }

        checkAndSendInvite(invitedUid);
    }

    private void loadUsersForInvite(Runnable onDone) {
        if (!allUsersForInvite.isEmpty()) {
            onDone.run();
            return;
        }
        db.collection("users")
                .limit(300)
                .get()
                .addOnSuccessListener(snaps -> {
                    allUsersForInvite.clear();
                    for (QueryDocumentSnapshot doc : snaps) {
                        String uid = doc.getId();
                        String first = safe(doc.getString("firstName"));
                        String last = safe(doc.getString("lastName"));
                        String email = safe(doc.getString("email"));
                        String phone = safe(doc.getString("phone"));
                        allUsersForInvite.add(new MemberCandidate(uid, first, last, email, phone));
                    }
                    onDone.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    onDone.run();
                });
    }

    private void filterInviteCandidates(String query,
                                        List<MemberCandidate> filteredUsers,
                                        MemberSuggestionAdapter adapter,
                                        TextView tvHint,
                                        TextView tvSelected) {
        String q = normalize(query);
        filteredUsers.clear();
        for (MemberCandidate candidate : allUsersForInvite) {
            if (q.isEmpty() || candidate.matches(q)) {
                filteredUsers.add(candidate);
                if (filteredUsers.size() >= 20) break;
            }
        }
        adapter.notifyDataSetChanged();
        tvSelected.setVisibility(View.GONE);
        if (filteredUsers.isEmpty()) {
            tvHint.setText("No matching registered users");
        } else {
            tvHint.setText("Tap a suggestion or keep typing");
        }
    }

    private MemberCandidate findCandidateByQuery(String normalizedQuery) {
        if (normalizedQuery.isEmpty()) return null;
        for (MemberCandidate candidate : allUsersForInvite) {
            if (candidate.matches(normalizedQuery)) {
                return candidate;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private static class MemberCandidate {
        final String uid;
        final String firstName;
        final String lastName;
        final String email;
        final String phone;

        MemberCandidate(String uid, String firstName, String lastName, String email, String phone) {
            this.uid = uid;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
        }

        boolean matches(String q) {
            String full = (firstName + " " + lastName).trim().toLowerCase(Locale.ROOT);
            return email.toLowerCase(Locale.ROOT).contains(q)
                    || firstName.toLowerCase(Locale.ROOT).contains(q)
                    || lastName.toLowerCase(Locale.ROOT).contains(q)
                    || full.contains(q)
                    || phone.toLowerCase(Locale.ROOT).contains(q);
        }

        String displayName() {
            String full = (firstName + " " + lastName).trim();
            return full.isEmpty() ? "User" : full;
        }
    }

    private static class MemberSuggestionAdapter extends RecyclerView.Adapter<MemberSuggestionAdapter.Holder> {
        interface OnCandidateClick {
            void onClick(MemberCandidate candidate);
        }

        private final List<MemberCandidate> items;
        private final OnCandidateClick onCandidateClick;

        MemberSuggestionAdapter(List<MemberCandidate> items, OnCandidateClick onCandidateClick) {
            this.items = items;
            this.onCandidateClick = onCandidateClick;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_member_suggestion, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            MemberCandidate item = items.get(position);
            holder.tvName.setText(item.displayName());
            holder.tvMeta.setText(item.email + (item.phone.isEmpty() ? "" : "  •  " + item.phone));
            holder.itemView.setOnClickListener(v -> onCandidateClick.onClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView tvName;
            final TextView tvMeta;

            Holder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCandidateName);
                tvMeta = itemView.findViewById(R.id.tvCandidateMeta);
            }
        }
    }

    private void checkAndSendInvite(String invitedUid) {
        if (communityIdToShow == null) return;
        db.collection("community")
                .document(communityIdToShow)
                .collection("members")
                .document(invitedUid)
                .get()
                .addOnSuccessListener(memberDoc -> {
                    if (memberDoc.exists()) {
                        Toast.makeText(this, "This user is already a member.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    checkPendingInvite(invitedUid);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to check membership", Toast.LENGTH_SHORT).show()
                );
    }

    private void checkPendingInvite(String invitedUid) {
        if (communityIdToShow == null) return;
        db.collection("community")
                .document(communityIdToShow)
                .collection("memberInvites")
                .document(invitedUid)
                .get()
                .addOnSuccessListener(inviteDoc -> {
                    String status = safe(inviteDoc.getString("status"));
                    if (inviteDoc.exists() && "pending".equals(status)) {
                        Toast.makeText(this, "Invite already sent and pending.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendCommunityInvite(invitedUid);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not verify previous invites", Toast.LENGTH_SHORT).show()
                );
    }

    private void sendCommunityInvite(String invitedUid) {
        if (communityIdToShow == null) return;
        db.collection("community")
                .document(communityIdToShow)
                .get()
                .addOnSuccessListener(communityDoc -> {
                    String communityName = communityDoc.getString("name");
                    if (communityName == null || communityName.trim().isEmpty()) {
                        communityName = "My Community";
                    }

                    String message = "You have been invited to join " + communityName;
                    Map<String, Object> inviteData = new HashMap<>();
                    inviteData.put("type", "community_invite");
                    inviteData.put("title", "Community Invite");
                    inviteData.put("status", "pending");
                    inviteData.put("communityId", communityIdToShow);
                    inviteData.put("communityName", communityName);
                    inviteData.put("invitedBy", firebaseManagerAndAuth.getCurrentUserUid());
                    inviteData.put("message", message);
                    inviteData.put("reason", "Reason: Community owner invited you");
                    inviteData.put("createdAt", FieldValue.serverTimestamp());

                    Map<String, Object> memberInviteData = new HashMap<>(inviteData);
                    memberInviteData.put("invitedUserId", invitedUid);
                    memberInviteData.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("community")
                            .document(communityIdToShow)
                            .collection("memberInvites")
                            .document(invitedUid)
                            .set(memberInviteData)
                            .addOnSuccessListener(unused ->
                                    db.collection("users")
                                            .document(invitedUid)
                                            .collection("notifications")
                                            .add(inviteData)
                                            .addOnSuccessListener(docRef ->
                                                    Toast.makeText(this, "Invite sent successfully", Toast.LENGTH_SHORT).show()
                                            )
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Invite saved, but notification could not be sent.", Toast.LENGTH_LONG).show()
                                            )
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to send invite", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load community details", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (communityIdToShow != null) {
            fetchCommunityData();
            checkIfFollowing();
        }
        checkImpendingNotifications();
    }
}
