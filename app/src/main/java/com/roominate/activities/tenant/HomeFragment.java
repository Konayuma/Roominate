package com.roominate.activities.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roominate.R;
import com.roominate.adapters.PropertyAdapter;
import com.roominate.models.Property;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private PropertyAdapter adapter;
    private List<Property> properties = new ArrayList<>();
    private ImageButton menuButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        
        recyclerView = v.findViewById(R.id.recyclerViewHouses);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        menuButton = v.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view -> {
            // Open drawer from parent activity
            if (getActivity() instanceof TenantDashboardActivity) {
                ((TenantDashboardActivity) getActivity()).openDrawer();
            }
        });

        loadSampleData();

        adapter = new PropertyAdapter(getContext(), properties, property -> {
            Intent i = new Intent(getActivity(), BoardingHouseDetailsActivity.class);
            // BoardingHouseDetailsActivity expects the extra key "boarding_house_id"
            i.putExtra("boarding_house_id", property.id);
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);

        return v;
    }

    private void loadSampleData() {
        properties.clear();
        properties.add(new Property("1", "Cozy Studio near Downtown", "123 Main St, City", "₱8,000 / month", ""));
        properties.add(new Property("2", "Bright 1BR with Balcony", "45 Rizal Ave, City", "₱12,000 / month", ""));
        properties.add(new Property("3", "Furnished Room for Rent", "78 Mnt St, City", "₱6,500 / month", ""));
        properties.add(new Property("4", "Modern Studio Loft", "21 Ocean Dr, City", "₱15,000 / month", ""));
    }
}