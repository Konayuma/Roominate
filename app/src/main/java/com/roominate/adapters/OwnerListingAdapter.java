package com.roominate.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.roominate.R;
import com.roominate.activities.owner.AddListingActivity;
import com.roominate.models.Property;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Locale;

public class OwnerListingAdapter extends RecyclerView.Adapter<OwnerListingAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Property> properties;

    public OwnerListingAdapter(Context context, ArrayList<Property> properties) {
        this.context = context;
        this.properties = properties;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_owner_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Property property = properties.get(position);

        holder.propertyNameTextView.setText(property.getName());
        holder.propertyAddressTextView.setText(property.getAddress());
        holder.priceTextView.setText(String.format(Locale.getDefault(), "$%.0f / mo", property.getMonthlyRate()));
        holder.statusChip.setText(property.getStatus());

        // Load image using Picasso
        if (property.getImageUrls() != null && !property.getImageUrls().isEmpty()) {
            Picasso.get()
                    .load(property.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_house_placeholder)
                    .error(R.drawable.ic_house_placeholder)
                    .fit()
                    .centerCrop()
                    .into(holder.propertyImageView);
        } else {
            holder.propertyImageView.setImageResource(R.drawable.ic_house_placeholder);
        }

        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddListingActivity.class);
            intent.putExtra("property_id", property.getId());
            context.startActivity(intent);
        });

        // TODO: Implement view bookings functionality
        // holder.viewBookingsButton.setOnClickListener(v -> { ... });
    }

    @Override
    public int getItemCount() {
        return properties.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImageView;
        TextView propertyNameTextView, propertyAddressTextView, priceTextView;
        Chip statusChip;
        MaterialButton editButton, viewBookingsButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImageView = itemView.findViewById(R.id.propertyImageView);
            propertyNameTextView = itemView.findViewById(R.id.propertyNameTextView);
            propertyAddressTextView = itemView.findViewById(R.id.propertyAddressTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            statusChip = itemView.findViewById(R.id.statusChip);
            editButton = itemView.findViewById(R.id.editButton);
            viewBookingsButton = itemView.findViewById(R.id.viewBookingsButton);
        }
    }
}
