package com.roominate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roominate.models.Property;
import com.roominate.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(Property property);
    }

    private final List<Property> properties;
    private final Context context;
    private final OnItemClickListener listener;

    public PropertyAdapter(Context context, List<Property> properties, OnItemClickListener listener) {
        this.context = context;
        this.properties = properties;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_property_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Property p = properties.get(position);
        holder.title.setText(p.getName());
        holder.address.setText(p.getAddress());
    // monthly rate may be a floating-point value; use a float-friendly format
    double rate = p.getMonthlyRate();
    String formattedRate = String.format("%,.0f", rate); // no decimal places
    holder.price.setText("₱" + formattedRate + "/mo");
        if (p.getThumbnailUrl() != null && !p.getThumbnailUrl().isEmpty()) {
            Picasso.get().load(p.getThumbnailUrl()).fit().centerCrop().into(holder.thumbnail);
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_house_placeholder);
        }
        holder.itemView.setOnClickListener(v -> listener.onItemClick(p));
    }

    @Override
    public int getItemCount() {
        return properties.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView address;
        TextView price;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.property_thumbnail);
            title = itemView.findViewById(R.id.property_title);
            address = itemView.findViewById(R.id.property_address);
            price = itemView.findViewById(R.id.property_price);
        }
    }
}
