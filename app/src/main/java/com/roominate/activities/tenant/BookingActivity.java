package com.roominate.activities.tenant;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView boardingHouseNameTextView;
    private TextView propertyAddressTextView;
    private TextView priceTextView;
    private EditText moveInDateEditText;
    private EditText durationEditText;
    private TextView monthlyRateTextView;
    private TextView durationSummaryTextView;
    private TextView totalAmountTextView;
    private EditText notesEditText;
    private Button submitBookingButton;
    private ProgressBar progressBar;

    private String boardingHouseId;
    private double monthlyRate = 0;
    private SimpleDateFormat dateFormat;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        boardingHouseId = getIntent().getStringExtra("boarding_house_id");
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        selectedDate = Calendar.getInstance();

        initializeViews();
        setupToolbar();
        loadBoardingHouseInfo();
        setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        boardingHouseNameTextView = findViewById(R.id.boardingHouseNameTextView);
        propertyAddressTextView = findViewById(R.id.propertyAddressTextView);
        priceTextView = findViewById(R.id.priceTextView);
        moveInDateEditText = findViewById(R.id.moveInDateEditText);
        durationEditText = findViewById(R.id.durationEditText);
        monthlyRateTextView = findViewById(R.id.monthlyRateTextView);
        durationSummaryTextView = findViewById(R.id.durationSummaryTextView);
        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        notesEditText = findViewById(R.id.notesEditText);
        submitBookingButton = findViewById(R.id.submitBookingButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadBoardingHouseInfo() {
        if (boardingHouseId == null || boardingHouseId.isEmpty()) {
            // Fallback to placeholder
            boardingHouseNameTextView.setText("Sample Boarding House");
            propertyAddressTextView.setText("123 Main St, City");
            monthlyRate = 1500;
            monthlyRateTextView.setText(String.format("K%.2f", monthlyRate));
            priceTextView.setText(String.format("K%.2f / month", monthlyRate));
            return;
        }

        // Load boarding house info from Supabase using the helper
        SupabaseClient.getInstance().getPropertyById(boardingHouseId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        org.json.JSONArray data = response.optJSONArray("data");
                        if (data != null && data.length() > 0) {
                            org.json.JSONObject prop = data.getJSONObject(0);
                            String name = prop.optString("name", prop.optString("title", "Boarding House"));
                            String address = prop.optString("address", "");
                            double rate = prop.optDouble("monthly_rate", prop.optDouble("price", 0.0));

                            boardingHouseNameTextView.setText(name);
                            propertyAddressTextView.setText(address);
                            monthlyRate = rate;
                            monthlyRateTextView.setText(String.format("K%.2f", monthlyRate));
                            priceTextView.setText(String.format("K%.2f / month", monthlyRate));
                        } else {
                            // No data: keep defaults
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Keep defaults and show a brief message
                    Toast.makeText(BookingActivity.this, "Failed to load property: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupListeners() {
        // Date picker setup
        moveInDateEditText.setOnClickListener(v -> showDatePicker());
        moveInDateEditText.setFocusable(false);
        
        // Duration text watcher for automatic total calculation
        durationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        submitBookingButton.setOnClickListener(v -> submitBooking());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                moveInDateEditText.setText(dateFormat.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private double calculateTotal() {
        String durationStr = durationEditText.getText().toString();
        double total = 0.0;
        if (!durationStr.isEmpty()) {
            try {
                int months = Integer.parseInt(durationStr);
                durationSummaryTextView.setText(months + " months");
                total = monthlyRate * months;
                totalAmountTextView.setText(String.format("K%.2f", total));
            } catch (NumberFormatException e) {
                totalAmountTextView.setText("K0.00");
            }
        } else {
            durationSummaryTextView.setText("0 months");
            totalAmountTextView.setText("K0.00");
        }
        return total;
    }

    private void submitBooking() {
        // Validate inputs
        if (moveInDateEditText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select a move-in date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (durationEditText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please enter duration in months", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get form data
        String moveInDate = moveInDateEditText.getText().toString().trim();
        int months = Integer.parseInt(durationEditText.getText().toString().trim());
        
        // Calculate end date based on move-in date and duration
        String endDate = calculateEndDate(moveInDate, months);
        
        // Get total amount
        double totalAmount = calculateTotal();
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        submitBookingButton.setEnabled(false);
        
        // Submit booking to Supabase
        SupabaseClient.getInstance().createBooking(
            boardingHouseId,
            moveInDate,
            endDate,
            totalAmount,
            new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        submitBookingButton.setEnabled(true);
                        Toast.makeText(BookingActivity.this, 
                            "Booking request submitted successfully!", 
                            Toast.LENGTH_LONG).show();
                        
                        // Return to previous activity
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        submitBookingButton.setEnabled(true);
                        Toast.makeText(BookingActivity.this, 
                            "Failed to submit booking: " + error, 
                            Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }
    
    private String calculateEndDate(String startDate, int months) {
        // Accept multiple input formats to be robust across locales and copy/paste values
        String trimmed = startDate != null ? startDate.trim() : "";
        if (trimmed.isEmpty()) return startDate;

        java.util.Date date = null;
        // Try ISO first (yyyy-MM-dd)
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            date = iso.parse(trimmed);
        } catch (Exception ignored) {}

        // Try "MMM dd, yyyy" using device locale
        if (date == null) {
            try {
                SimpleDateFormat f1 = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                date = f1.parse(trimmed);
            } catch (Exception ignored) {}
        }

        // Try English month names as a fallback
        if (date == null) {
            try {
                SimpleDateFormat f2 = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
                date = f2.parse(trimmed);
            } catch (Exception ignored) {}
        }

        if (date == null) {
            Log.e("BookingActivity", "Unable to parse start date: '" + startDate + "'");
            return startDate; // Let backend validate if needed
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, months);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return outputFormat.format(calendar.getTime());
    }
}
