package com.roominate.activities.tenant;

import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

public class BookingActivity extends AppCompatActivity {

    private TextView boardingHouseNameTextView;
    private TextView priceTextView;
    private DatePicker moveInDatePicker;
    private EditText durationEditText;
    private TextView totalAmountTextView;
    private EditText notesEditText;
    private Button submitBookingButton;

    private String boardingHouseId;
    private double monthlyRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        boardingHouseId = getIntent().getStringExtra("boarding_house_id");

        initializeViews();
        loadBoardingHouseInfo();
        setupListeners();
    }

    private void initializeViews() {
        boardingHouseNameTextView = findViewById(R.id.boardingHouseNameTextView);
        priceTextView = findViewById(R.id.priceTextView);
        moveInDatePicker = findViewById(R.id.moveInDatePicker);
        durationEditText = findViewById(R.id.durationEditText);
        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        notesEditText = findViewById(R.id.notesEditText);
        submitBookingButton = findViewById(R.id.submitBookingButton);
    }

    private void loadBoardingHouseInfo() {
        // TODO: Load boarding house info from Supabase
    }

    private void setupListeners() {
        submitBookingButton.setOnClickListener(v -> submitBooking());

        // Calculate total when duration changes
        // durationEditText.addTextChangedListener(new TextWatcher() {
        //     @Override
        //     public void afterTextChanged(Editable s) {
        //         calculateTotal();
        //     }
        // });
    }

    private void calculateTotal() {
        String durationStr = durationEditText.getText().toString();
        if (!durationStr.isEmpty()) {
            int months = Integer.parseInt(durationStr);
            double total = monthlyRate * months;
            totalAmountTextView.setText(String.format("Total: ₱%.2f", total));
        }
    }

    private void submitBooking() {
        // TODO: Validate and submit booking to Supabase
        Toast.makeText(this, "Booking functionality to be implemented", Toast.LENGTH_SHORT).show();
    }
}
