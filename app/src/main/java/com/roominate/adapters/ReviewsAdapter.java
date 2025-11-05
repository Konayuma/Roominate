package com.roominate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.roominate.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private Context context;
    private JSONArray reviews;
    private SimpleDateFormat dateFormat;

    public ReviewsAdapter(Context context, JSONArray reviews) {
        this.context = context;
        this.reviews = reviews != null ? reviews : new JSONArray();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        try {
            JSONObject review = reviews.getJSONObject(position);
            
            // Get user data from join
            JSONObject user = review.optJSONObject("users");
            String reviewerName = "Anonymous";
            if (user != null) {
                reviewerName = user.optString("display_name", "Anonymous");
                if (reviewerName.isEmpty() || reviewerName.equals("null")) {
                    reviewerName = "User";
                }
            }
            
            int rating = review.optInt("rating", 0);
            String comment = review.optString("comment", "");
            String createdAt = review.optString("created_at", "");
            
            holder.reviewerNameText.setText(reviewerName);
            holder.ratingBar.setRating(rating);
            holder.commentText.setText(comment);
            
            // Format date
            if (!createdAt.isEmpty()) {
                try {
                    // Parse ISO 8601 date
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    Date date = isoFormat.parse(createdAt);
                    if (date != null) {
                        holder.dateText.setText(dateFormat.format(date));
                    }
                } catch (Exception e) {
                    holder.dateText.setText(createdAt.substring(0, Math.min(10, createdAt.length())));
                }
            }
            
            // Show/hide comment based on content
            if (comment.isEmpty()) {
                holder.commentText.setVisibility(View.GONE);
            } else {
                holder.commentText.setVisibility(View.VISIBLE);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return reviews.length();
    }

    public void updateReviews(JSONArray newReviews) {
        this.reviews = newReviews != null ? newReviews : new JSONArray();
        notifyDataSetChanged();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerNameText;
        RatingBar ratingBar;
        TextView commentText;
        TextView dateText;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerNameText = itemView.findViewById(R.id.reviewerNameText);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            commentText = itemView.findViewById(R.id.commentText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}
