package com.roominate.activities.tenant;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.roominate.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
    }

    private void loadBoardingHouseInfo() {
        // TODO: Load boarding house info from Supabase
        // For now set default values for testing
        boardingHouseNameTextView.setText("Sample Boarding House");
        propertyAddressTextView.setText("123 Main St, City");
        monthlyRate = 1500;
        monthlyRateTextView.setText(String.format("₱%.2f", monthlyRate));
        priceTextView.setText(String.format("₱%.2f / month", monthlyRate));
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

    private void calculateTotal() {
        String durationStr = durationEditText.getText().toString();
        if (!durationStr.isEmpty()) {
            try {
                int months = Integer.parseInt(durationStr);
                durationSummaryTextView.setText(months + " months");
                double total = monthlyRate * months;
                totalAmountTextView.setText(String.format("₱%.2f", total));
            } catch (NumberFormatException e) {
                totalAmountTextView.setText("₱0.00");
            }
        } else {
            durationSummaryTextView.setText("0 months");
            totalAmountTextView.setText("₱0.00");
        }
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

        // TODO: Validate and submit booking to Supabase
        Toast.makeText(this, "Booking functionality to be implemented", Toast.LENGTH_SHORT).show();
    }
}
