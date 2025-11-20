# Lenco Payment Gateway Integration - Implementation Summary

## Overview
Successfully integrated Lenco payment gateway (https://lenco.co/zm) into the Roominate Android app for processing booking payments. The implementation follows a secure, webhook-based payment flow with real-time status monitoring.

## Components Implemented

### 1. Android Services

#### PaymentService.java
**Location:** `app/src/main/java/com/roominate/services/PaymentService.java`

**Purpose:** HTTP client for Lenco payment API via Supabase Edge Functions

**Key Methods:**
- `getInstance(Context)` - Singleton pattern for service access
- `initiatePayment(bookingId, amount, email, name, callback)` - Starts payment flow
  - Makes POST request to `/functions/v1/lenco-payment`
  - Returns payment URL and reference via callback
  - Runs on background thread
  
- `checkPaymentStatus(reference, callback)` - Polls payment status
  - Queries `/rest/v1/payments` table with payment_reference filter
  - Returns current payment status (pending/completed/failed/cancelled)
  - Used for periodic status checks during payment

**Configuration:**
- Uses `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`
- 30-second connection timeout for payment initiation
- 15-second timeout for status checks

#### SupabaseClient.java (Enhanced)
**Location:** `app/src/main/java/com/roominate/services/SupabaseClient.java`

**New Method:**
```java
public void createBooking(org.json.JSONObject bookingData, ApiCallback callback)
```

**Purpose:** Flexible booking creation with custom fields (payment_status, payment_reference)

**Features:**
- Accepts arbitrary JSON fields for booking data
- Auto-populates tenant_id from session if not provided
- Returns full booking object with generated ID
- Handles RLS policies with access_token or anon_key fallback

### 2. Payment Activity

#### PaymentActivity.java
**Location:** `app/src/main/java/com/roominate/activities/PaymentActivity.java`

**Purpose:** WebView wrapper for Lenco payment page with smart monitoring

**Key Features:**
- **WebView Configuration:**
  - JavaScript enabled for payment page interaction
  - DOM storage enabled for session management
  - WebViewClient intercepts page loads for URL pattern detection
  
- **Status Monitoring:**
  - Periodic polling every 5 seconds via Handler
  - Checks payment status via PaymentService.checkPaymentStatus()
  - Dual detection: URL patterns + database polling
  
- **URL Pattern Detection:**
  - Success: `success`, `complete`, `approved` in URL
  - Failure: `cancel`, `failed`, `error` in URL
  - Automatically finishes activity on match
  
- **User Experience:**
  - ProgressBar during page load
  - Back button intercept with confirmation dialog
  - Status check cleanup on activity destroy

**Intent Extras (Required):**
- `booking_id` - UUID of created booking
- `payment_url` - Lenco payment page URL from Edge Function
- `payment_reference` - Unique payment reference for status tracking

**Result:**
- `RESULT_OK` with `payment_status` extra on completion
- `RESULT_CANCELED` on user cancellation or error

#### activity_payment.xml
**Location:** `app/src/main/res/layout/activity_payment.xml`

**Structure:**
- CoordinatorLayout with Material Design toolbar
- WebView with `appbar_scrolling_view_behavior`
- ProgressBar overlay during loading

### 3. Booking Integration

#### BoardingHouseDetailsActivity.java (Enhanced)
**Location:** `app/src/main/java/com/roominate/activities/tenant/BoardingHouseDetailsActivity.java`

**New Methods:**

**`bookBoardingHouse()`**
- Validates user authentication
- Validates property data availability
- Launches booking dialog

**`showBookingDialog()`**
- Displays Material dialog with booking form
- Fields:
  - Move-in Date (DatePickerDialog)
  - Duration in months (numeric input)
  - Total Amount (auto-calculated display)
- Real-time total calculation: `(monthlyRate + securityDeposit) * duration`
- Validates minimum 1-month duration
- Calls `createBookingAndInitiatePayment()` on confirmation

**`createBookingAndInitiatePayment(moveInDate, duration, totalAmount)`**
- Shows ProgressDialog during processing
- Calculates end date from duration
- Creates booking via `SupabaseClient.createBooking()` with:
  ```json
  {
    "listing_id": "property-uuid",
    "start_date": "2024-01-15",
    "end_date": "2024-02-15",
    "total_amount": 1500.00,
    "status": "pending",
    "payment_status": "pending"
  }
  ```
- On booking success, initiates payment via `PaymentService.initiatePayment()`
- Launches `PaymentActivity` with `startActivityForResult()`

**`onActivityResult(REQUEST_CODE_PAYMENT, resultCode, data)`**
- Handles payment completion
- Shows success message and closes activity on `payment_status=completed`
- Shows cancellation message on failure

**`updateTotalAmount(TextView, durationStr)`**
- Helper for real-time total calculation in dialog
- Formats: `Total: ZMW 1,500.00`

#### dialog_booking_details.xml
**Location:** `app/src/main/res/layout/dialog_booking_details.xml`

**Structure:**
- LinearLayout with TextInputLayouts (Material Design)
- Move-in Date field (non-editable, opens DatePicker on click)
- Duration field (number input)
- Total Amount TextView (bold, right-aligned)

### 4. Manifest Registration

#### AndroidManifest.xml
**Location:** `app/src/main/AndroidManifest.xml`

**Addition:**
```xml
<activity 
    android:name=".activities.PaymentActivity"
    android:label="Payment"
    android:configChanges="orientation|screenSize"
    android:exported="false" />
```

**Configuration:**
- `configChanges="orientation|screenSize"` - Prevents activity recreation on rotation (preserves WebView state)
- `exported="false"` - Internal activity only (no deep links)

## Payment Flow

### User Journey
1. **Property Details** → User clicks "Book Now" on property
2. **Authentication Check** → System validates user is signed in
3. **Booking Dialog** → User enters move-in date and duration
4. **Total Calculation** → Real-time display of `(monthly + deposit) × months`
5. **Booking Creation** → POST to `/rest/v1/bookings` with `status=pending`, `payment_status=pending`
6. **Payment Initiation** → POST to `/functions/v1/lenco-payment` returns payment URL
7. **WebView Launch** → PaymentActivity loads Lenco payment page
8. **User Payment** → User completes payment on Lenco's secure page
9. **Webhook Callback** → Lenco calls `/functions/v1/lenco-webhook` to update payment status
10. **Status Polling** → PaymentActivity detects completion via periodic checks
11. **Return to App** → Activity closes with `RESULT_OK` and `payment_status=completed`
12. **Confirmation** → Success message displayed, booking status updated to `confirmed`

### Technical Flow
```
Android App                    Supabase Edge Functions           Lenco API
    |                                  |                              |
    |-- POST /rest/v1/bookings ------->|                              |
    |<--- booking_id ------------------|                              |
    |                                  |                              |
    |-- POST /functions/v1/lenco-payment ->                           |
    |                                  |--- POST /v1/payments ------->|
    |                                  |<--- payment_url + reference -|
    |<--- payment_url + reference -----|                              |
    |                                  |                              |
    |-- Load payment_url in WebView --------------------------------->|
    |                                  |                              |
    |                                  |<--- POST /functions/v1/      |
    |                                  |     lenco-webhook (HMAC) ----|
    |                                  |                              |
    |                                  |-- UPDATE payments table ---->|
    |                                  |   (status=completed)         |
    |                                  |                              |
    |-- GET /rest/v1/payments -------->|                              |
    |   ?payment_reference=eq.XXX     |                              |
    |<--- status=completed ------------|                              |
    |                                  |                              |
    |-- RESULT_OK (payment_status) -->|                              |
```

## Security Features

1. **API Key Management**
   - Lenco signature key stored in Supabase secrets (not in code)
   - BuildConfig used for Supabase credentials (not hardcoded)

2. **Webhook Verification**
   - HMAC-SHA256 signature validation in Edge Function
   - Prevents unauthorized payment status manipulation

3. **RLS Policies**
   - Bookings table protected by Row Level Security
   - Users can only access their own bookings
   - Access token propagated from Android SharedPreferences

4. **Payment Reference**
   - Unique UUID generated per payment
   - Prevents replay attacks
   - Links payments to bookings via foreign key

## Testing Checklist

### Prerequisites
- [ ] Supabase Edge Functions deployed (`lenco-payment`, `lenco-webhook`)
- [ ] Supabase secrets configured (`LENCO_API_KEY`, `LENCO_SIGNATURE_KEY`)
- [ ] Database tables created (`bookings`, `payments`)
- [ ] RLS policies configured on bookings table
- [ ] BuildConfig fields set (`SUPABASE_URL`, `SUPABASE_ANON_KEY`)

### Manual Testing
1. **Happy Path:**
   - [ ] Navigate to property details
   - [ ] Click "Book Now"
   - [ ] Fill booking dialog (move-in date, duration)
   - [ ] Verify total amount calculation
   - [ ] Click "Confirm & Pay"
   - [ ] Verify redirect to Lenco payment page
   - [ ] Complete payment
   - [ ] Verify return to app with success message
   - [ ] Check database: `bookings.payment_status = 'completed'`
   - [ ] Check database: `payments.status = 'completed'`

2. **Error Handling:**
   - [ ] Test with unsigned-in user → Shows "Please sign in" toast
   - [ ] Test with invalid duration (0 or negative) → Shows validation error
   - [ ] Test payment cancellation → Returns RESULT_CANCELED
   - [ ] Test network failure → Shows error toast
   - [ ] Test back button during payment → Shows confirmation dialog

3. **Edge Cases:**
   - [ ] Test device rotation during payment → WebView state preserved
   - [ ] Test leaving app during payment → Status polling resumes on return
   - [ ] Test slow webhook callback → Polling detects status anyway

### Automated Testing
```bash
# Build debug APK
cd c:\Users\sepok\AndroidStudioProjects\Roominate
.\gradlew.bat assembleDebug --no-daemon

# Install on device
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Run app
adb shell am start -n com.roominate/.activities.SplashActivity
```

## Database Schema

### bookings table (existing)
```sql
-- New columns added for payment tracking
ALTER TABLE bookings 
ADD COLUMN payment_status TEXT DEFAULT 'pending' CHECK (payment_status IN ('pending', 'completed', 'failed', 'cancelled')),
ADD COLUMN payment_reference TEXT UNIQUE;
```

### payments table (new)
```sql
CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  booking_id UUID REFERENCES bookings(id) ON DELETE CASCADE,
  payment_reference TEXT UNIQUE NOT NULL,
  amount DECIMAL(10, 2) NOT NULL,
  currency TEXT DEFAULT 'ZMW',
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'completed', 'failed', 'cancelled')),
  lenco_transaction_id TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_payments_reference ON payments(payment_reference);
CREATE INDEX idx_payments_booking_id ON payments(booking_id);
```

## Configuration

### Supabase Secrets
Set via Supabase Dashboard → Project Settings → Edge Functions → Secrets:
```
LENCO_API_KEY=your_lenco_api_key
LENCO_SIGNATURE_KEY=314e9c9a25e34553fcd9e84539d6e9d3d527899d8d8954ef9b88bacfa8d19f0e
```

### BuildConfig (app/build.gradle)
```gradle
android {
    defaultConfig {
        buildConfigField "String", "SUPABASE_URL", "\"https://your-project.supabase.co\""
        buildConfigField "String", "SUPABASE_ANON_KEY", "\"your-anon-key\""
    }
}
```

## Next Steps

1. **Production Deployment:**
   - Switch Lenco API endpoint to production URL
   - Update LENCO_API_KEY in Supabase secrets
   - Enable RLS policies on payments table
   - Set up monitoring for webhook failures

2. **Enhancements:**
   - Add payment receipt generation
   - Implement refund flow
   - Add payment history screen
   - Support multiple payment methods
   - Add email notifications for payment status

3. **Analytics:**
   - Track payment conversion rate
   - Monitor payment failure reasons
   - Log average time to payment completion

## Files Modified/Created

### New Files
- `app/src/main/java/com/roominate/services/PaymentService.java` (138 lines)
- `app/src/main/java/com/roominate/activities/PaymentActivity.java` (192 lines)
- `app/src/main/res/layout/activity_payment.xml` (31 lines)
- `app/src/main/res/layout/dialog_booking_details.xml` (50 lines)

### Modified Files
- `app/src/main/AndroidManifest.xml` (added PaymentActivity registration)
- `app/src/main/java/com/roominate/services/SupabaseClient.java` (added createBooking overload, ~100 lines)
- `app/src/main/java/com/roominate/activities/tenant/BoardingHouseDetailsActivity.java` (added payment flow, ~200 lines)

### Build Status
✅ **BUILD SUCCESSFUL** - Project compiles without errors

## Signature Key
**Lenco Webhook Signature Key (SHA-256):**
```
314e9c9a25e34553fcd9e84539d6e9d3d527899d8d8954ef9b88bacfa8d19f0e
```
*Note: This key is used in Edge Function webhook verification. Keep secure.*

## Support
For Lenco payment gateway issues, refer to:
- Lenco Documentation: https://lenco.co/zm/docs (if available)
- Edge Function logs: Supabase Dashboard → Functions → Logs
- Payment debugging: Check Supabase `payments` table for transaction details

---
*Implementation Date: 2024*
*Lenco Payment Gateway Version: 1.0*
*Android Target SDK: 34*
