package com.example.swachhtasarthi.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.swachhtasarthi.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText etQuery;
    private TextView tvEmpty;
    private ResultsAdapter adapter;
    private final List<SearchResult> results = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ImageView btnBack = findViewById(R.id.btnBack);
        etQuery = findViewById(R.id.etQuery);
        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rvResults = findViewById(R.id.rvResults);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultsAdapter(results, this::openResult);
        rvResults.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s == null ? "" : s.toString().trim();
                runSearch(q);
            }
        });
    }

    private void runSearch(@NonNull String query) {
        results.clear();
        adapter.notifyDataSetChanged();

        if (query.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Search for people or communities");
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        // Minimal, robust approach: fetch a small set and filter client-side.
        // This avoids needing Firestore "startsWith" indexes or custom search fields.
        String q = query.toLowerCase(Locale.ROOT);

        db.collection("users")
                .limit(50)
                .get()
                .addOnSuccessListener(snaps -> {
                    for (QueryDocumentSnapshot doc : snaps) {
                        String uid = doc.getId();
                        String first = safe(doc.getString("firstName"));
                        String last = safe(doc.getString("lastName"));
                        String city = safe(doc.getString("city"));
                        String profileUrl = safe(doc.getString("profileImageUrl"));
                        String name = (first + " " + last).trim();
                        if (name.isEmpty()) name = "User";

                        if (matches(q, name, city)) {
                            results.add(SearchResult.user(uid, name, city, profileUrl));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState(query);
                });

        db.collection("community")
                .limit(50)
                .get()
                .addOnSuccessListener(snaps -> {
                    for (QueryDocumentSnapshot doc : snaps) {
                        String communityId = doc.getId();
                        String name = safe(doc.getString("name"));
                        String email = safe(doc.getString("email"));
                        String profileUrl = safe(doc.getString("profileImage"));
                        if (name.isEmpty()) name = "Community";

                        if (matches(q, name, email)) {
                            results.add(SearchResult.community(communityId, name, email, profileUrl));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState(query);
                });
    }

    private void updateEmptyState(String query) {
        if (results.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No results for \"" + query + "\"");
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private boolean matches(String q, String a, String b) {
        return (a != null && a.toLowerCase(Locale.ROOT).contains(q))
                || (b != null && b.toLowerCase(Locale.ROOT).contains(q));
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void openResult(@NonNull SearchResult item) {
        if (item.type == SearchResult.Type.USER) {
            Intent i = new Intent(this, PeopleActivity.class);
            i.putExtra("userId", item.id);
            startActivity(i);
            return;
        }

        Intent i = new Intent(this, CommunityActivity.class);
        i.putExtra("communityId", item.id);
        startActivity(i);
    }

    static class SearchResult {
        enum Type { USER, COMMUNITY }
        final Type type;
        final String id;
        final String title;
        final String subtitle;
        final String imageUrl;

        private SearchResult(Type type, String id, String title, String subtitle, String imageUrl) {
            this.type = type;
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.imageUrl = imageUrl;
        }

        static SearchResult user(String uid, String title, String subtitle, String imageUrl) {
            return new SearchResult(Type.USER, uid, title, subtitle, imageUrl);
        }

        static SearchResult community(String id, String title, String subtitle, String imageUrl) {
            return new SearchResult(Type.COMMUNITY, id, title, subtitle, imageUrl);
        }
    }

    interface OnResultClick {
        void onClick(@NonNull SearchResult result);
    }

    static class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.Holder> {
        private final List<SearchResult> items;
        private final OnResultClick onClick;

        ResultsAdapter(List<SearchResult> items, OnResultClick onClick) {
            this.items = items;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.component_search_result_item, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            SearchResult item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvSubtitle.setText(item.subtitle);
            holder.tvTag.setText(item.type == SearchResult.Type.USER ? "USER" : "COMMUNITY");
            holder.tvTag.setTextColor(item.type == SearchResult.Type.USER ? 0xFF135BEC : 0xFF10B981);

            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.imageUrl)
                        .placeholder(R.drawable.profile_image)
                        .error(R.drawable.profile_image)
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.profile_image);
            }

            holder.itemView.setOnClickListener(v -> onClick.onClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            com.google.android.material.imageview.ShapeableImageView ivAvatar;
            TextView tvTitle, tvSubtitle, tvTag;

            Holder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
                tvTag = itemView.findViewById(R.id.tvTag);
            }
        }
    }
}

