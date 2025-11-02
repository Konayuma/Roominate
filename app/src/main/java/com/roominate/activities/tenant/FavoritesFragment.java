package com.roominate.activities.tenant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roominate.R;
import com.roominate.adapters.PropertyAdapter;
import com.roominate.models.BoardingHouse;
import com.roominate.models.Favorite;
import com.roominate.models.Property;
import com.roominate.services.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FavoritesFragment extends Fragment {
    private static final String TAG = "FavoritesFragment";
    
    private RecyclerView recyclerView;
    private PropertyAdapter adapter;
    private List<Property> favoriteProperties = new ArrayList<>();
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView resultsCount;
    private Button exploreButton;
    
    private String userId;
    private OkHttpClient httpClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        loadUserId();
        loadFavorites();
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        progressBar = view.findViewById(R.id.progressBar);
        emptyState = view.findViewById(R.id.emptyState);
        resultsCount = view.findViewById(R.id.resultsCount);
        exploreButton = view.findViewById(R.id.exploreButton);
        
        httpClient = new OkHttpClient();
        
        exploreButton.setOnClickListener(v -> {
            // Navigate to home tab
            if (getActivity() instanceof TenantDashboardActivity) {
                ((TenantDashboardActivity) getActivity()).navigateToHome();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new PropertyAdapter(getContext(), favoriteProperties, property -> {
            Intent intent = new Intent(getActivity(), BoardingHouseDetailsActivity.class);
            intent.putExtra("boarding_house_id", property.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadUserId() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "User ID not found in session");
            showEmptyState();
        }
    }

    private void loadFavorites() {
        if (userId == null || userId.isEmpty()) {
            showEmptyState();
            return;
        }
        
        showLoading();
        
        new Thread(() -> {
            try {
                String supabaseUrl = com.roominate.BuildConfig.SUPABASE_URL;
                String supabaseKey = com.roominate.BuildConfig.SUPABASE_ANON_KEY;
                
                // Query favorites with boarding house details
                String url = supabaseUrl + "/rest/v1/favorites?user_id=eq." + userId + "&select=*,boarding_houses(*)";
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + supabaseKey)
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                String responseBody = response.body().string();
                
                Log.d(TAG, "Favorites response: " + responseBody);
                
                if (response.isSuccessful()) {
                    JSONArray favoritesArray = new JSONArray(responseBody);
                    List<Property> properties = new ArrayList<>();
                    
                    for (int i = 0; i < favoritesArray.length(); i++) {
                        JSONObject favoriteObj = favoritesArray.getJSONObject(i);
                        
                        if (favoriteObj.has("boarding_houses")) {
                            JSONObject bhObj = favoriteObj.getJSONObject("boarding_houses");
                            
                            String id = bhObj.optString("id", "");
                            String title = bhObj.optString("title", "");
                            String address = bhObj.optString("address", "");
                            double price = bhObj.optDouble("price_per_month", 0);
                            String thumbnailUrl = ""; // TODO: Get from properties_media
                            
                            Property property = new Property(
                                id,
                                title,
                                address,
                                String.format("₱%.2f / month", price),
                                thumbnailUrl
                            );
                            properties.add(property);
                        }
                    }
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        favoriteProperties.clear();
                        favoriteProperties.addAll(properties);
                        adapter.notifyDataSetChanged();
                        updateResultsCount(properties.size());
                        
                        if (properties.isEmpty()) {
                            showEmptyState();
                        } else {
                            showContent();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "Failed to load favorites", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading favorites", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        }).start();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        updateResultsCount(0);
    }

    private void updateResultsCount(int count) {
        String text = count == 1 ? "1 saved property" : count + " saved properties";
        resultsCount.setText(text);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload favorites when fragment becomes visible
        loadFavorites();
    }
}
