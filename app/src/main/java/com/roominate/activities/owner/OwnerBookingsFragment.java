package com.roominate.activities.owner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roominate.R;

public class OwnerBookingsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ImageButton menuButton;
    private TextView emptyStateText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_owner_bookings, container, false);
        
        recyclerView = v.findViewById(R.id.recyclerViewBookings);
        emptyStateText = v.findViewById(R.id.emptyStateText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        menuButton = v.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view -> {
            if (getActivity() instanceof OwnerDashboardActivity) {
                ((OwnerDashboardActivity) getActivity()).openDrawer();
            }
        });

        // TODO: Load bookings from Supabase
        updateEmptyState();

        return v;
    }
    
    private void updateEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
    }
}
