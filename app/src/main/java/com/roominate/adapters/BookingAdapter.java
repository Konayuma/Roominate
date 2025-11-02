package com.roominate.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.roominate.R;
import com.roominate.models.Booking;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    private Context context;
    private OnBookingActionListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnBookingActionListener {
        void onViewDetails(Booking booking);
        void onCancelBooking(Booking booking);
    }

    public BookingAdapter(Context context, OnBookingActionListener listener) {
        this.context = context;
        this.bookings = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        
        // Set property details
        holder.propertyNameTextView.setText(booking.getPropertyName());
        holder.propertyAddressTextView.setText(booking.getPropertyAddress());
        
        // Set booking details
        if (booking.getMoveInDate() != null) {
            holder.moveInDateTextView.setText(dateFormat.format(booking.getMoveInDate()));
        }
        holder.durationTextView.setText(booking.getDurationMonths() + " months");
        holder.totalAmountTextView.setText(String.format("K%.0f", booking.getTotalAmount()));
        
        // Set status badge
        holder.statusBadge.setText(booking.getFormattedStatus());
        holder.statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(booking.getStatusColor())));
        
        // Show/hide cancel button based on status
        String status = booking.getStatus().toLowerCase();
        if (status.equals("pending") || status.equals("approved")) {
            holder.cancelButton.setVisibility(View.VISIBLE);
        } else {
            holder.cancelButton.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.viewDetailsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetails(booking);
            }
        });
        
        holder.cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancelBooking(booking);
            }
        });
        
        // TODO: Load property image with Glide or similar
        // Glide.with(context).load(booking.getPropertyImageUrl()).into(holder.propertyImageView);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImageView;
        TextView statusBadge;
        TextView propertyNameTextView;
        TextView propertyAddressTextView;
        TextView moveInDateTextView;
        TextView durationTextView;
        TextView totalAmountTextView;
        Button viewDetailsButton;
        Button cancelButton;

        BookingViewHolder(View itemView) {
            super(itemView);
            propertyImageView = itemView.findViewById(R.id.propertyImageView);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            propertyNameTextView = itemView.findViewById(R.id.propertyNameTextView);
            propertyAddressTextView = itemView.findViewById(R.id.propertyAddressTextView);
            moveInDateTextView = itemView.findViewById(R.id.moveInDateTextView);
            durationTextView = itemView.findViewById(R.id.durationTextView);
            totalAmountTextView = itemView.findViewById(R.id.totalAmountTextView);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            cancelButton = itemView.findViewById(R.id.cancelButton);
        }
    }
}
