package com.roominate.activities.tenant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roominate.R;

import java.util.List;

public class HouseAdapter extends RecyclerView.Adapter<HouseAdapter.ViewHolder> {

    private List<House> items;
    private Context context;

    public HouseAdapter(Context context, List<House> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_house, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        House house = items.get(position);
        holder.title.setText(house.title);
        holder.location.setText(house.location);
        holder.price.setText(house.price);
        holder.rating.setText(String.valueOf(house.rating));
        holder.image.setImageResource(house.imageResId);

        holder.itemView.setOnClickListener(v -> {
            // open details - activity exists `BoardingHouseDetailsActivity`
            // pass id via intent
            // Intent intent = new Intent(context, BoardingHouseDetailsActivity.class);
            // intent.putExtra("houseId", house.id);
            // context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, location, price, rating;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.houseImage);
            title = itemView.findViewById(R.id.houseTitle);
            location = itemView.findViewById(R.id.houseLocation);
            price = itemView.findViewById(R.id.housePrice);
            rating = itemView.findViewById(R.id.houseRating);
        }
    }
}
