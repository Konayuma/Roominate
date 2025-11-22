# Tenant Booking Receipt Implementation

## Overview
Tenants can now view their confirmed and pending bookings as receipt-style cards in their MyBookingsFragment. The bookings display all relevant information including dates, durations, amounts, and property details.

## Feature: Booking Receipt View

### What's Implemented

#### 1. **MyBookingsFragment** (Fully Implemented)
**File:** `app/src/main/java/com/roominate/ui/fragments/MyBookingsFragment.java`

**Capabilities:**
- ✅ Load tenant's bookings from Supabase
- ✅ Display bookings with property details (name, address, image placeholder)
- ✅ Show booking status (Pending, Confirmed)
- ✅ Display move-in date, duration, and total amount
- ✅ Filter bookings by status: All, Confirmed, Pending
- ✅ Dynamic filter button styling (highlighted button shows current filter)
- ✅ Empty state messaging based on filter selection
- ✅ Thread-safe UI updates with `runOnUiThread()`
- ✅ Error handling and user feedback

**Key Methods:**
```java
loadBookings(String status)           // Load bookings from backend, filtered by status
parseBooking(JSONObject bookingObj)   // Parse JSON into Booking model with property details
updateFilterButtonStyles()            // Highlight active filter button
updateEmptyState()                    // Show/hide empty state based on booking count
```

#### 2. **Fragment Layout** (Updated)
**File:** `app/src/main/res/layout/fragment_my_bookings.xml`

**Updates:**
- ✅ Replaced TabLayout with filter buttons (All, Confirmed, Pending)
- ✅ Buttons highlight when active using system_blue color
- ✅ Material button styling with outline style
- ✅ Responsive grid layout
- ✅ Updated empty state messaging
- ✅ RecyclerView for booking list display

#### 3. **Booking Item Card** (Already Optimized)
**File:** `app/src/main/res/layout/item_booking_card.xml`

**Receipt-Style Display:**
- Property image (80x80dp placeholder)
- Status badge (Pending/Confirmed)
- Property name and address
- Move-in date with icon
- Duration in months
- Total amount in Kwacha (K)
- View Details and Cancel buttons

#### 4. **Backend API** (Ready to Use)
**File:** `SupabaseClient.java`

**Existing Methods:**
- ✅ `getTenantBookings(String status, ApiCallback callback)` - Fetches bookings for current tenant
- ✅ `updateBookingStatus(String bookingId, String newStatus, ApiCallback callback)` - Updates booking status
- ✅ Automatic user_id retrieval from SharedPreferences

**Database Query:**
```
GET /rest/v1/bookings?tenant_id=eq.{userId}&status=eq.{status}&select=*,boarding_houses(id,title,address)
```

Joins with `boarding_houses` table to get property details in one request.

#### 5. **Booking Model** (Already Exists)
**File:** `models/Booking.java`

**Properties:**
- id, tenantId, boardingHouseId
- moveInDate, endDate, status, totalAmount, createdAt
- propertyName, propertyAddress (from nested object)

---

## How It Works

### User Flow

1. **Tenant navigates to "My Bookings" tab**
   - Fragment loads and calls `loadBookings("all")`

2. **Bookings Display**
   - SupabaseClient queries bookings for current user
   - Results include property details via JOIN
   - BookingAdapter displays in RecyclerView

3. **Filter Bookings**
   - Click "Confirmed", "Pending", or "All" button
   - Fragment reloads with filtered results
   - Empty state updates appropriately

4. **View Booking Receipt**
   - See property name, address, image
   - See booking dates and duration
   - See total amount to pay (in Kwacha)
   - Status badge shows current state

### Status Flow

```
pending  → owner reviews → confirmed  → tenant can check booking details
         → owner rejects  → rejected   → tenant can rebook

confirmed → move-in date → active     → booking in progress
          → end date    → completed   → booking finished
```

---

## API Integration

### getTenantBookings Query

**Request:**
```
GET /rest/v1/bookings?
  tenant_id=eq.550e8400-e29b-41d4-a716-446655440000
  &select=*,boarding_houses(id,title,address)
  &order=created_at.desc
```

**Response:**
```json
[
  {
    "id": "uuid-1",
    "tenant_id": "user-uuid",
    "boarding_house_id": "property-uuid",
    "move_in_date": "2025-01-15",
    "end_date": "2025-07-15",
    "status": "confirmed",
    "total_amount": 9000.00,
    "created_at": "2024-11-05T10:30:00Z",
    "boarding_houses": {
      "id": "property-uuid",
      "title": "Peaceful Lodging House",
      "address": "123 Main St, Ndola, Copperbelt"
    }
  },
  ...
]
```

---

## User Interface Components

### Filter Buttons
- **All**: Shows all bookings (pending + confirmed + rejected)
- **Confirmed**: Only confirmed bookings (receipts)
- **Pending**: Only pending bookings (awaiting owner approval)

### Booking Card Elements
1. **Status Badge** - Color-coded (Pending=Orange, Confirmed=Green)
2. **Property Image** - Placeholder icon (can be enhanced with actual image)
3. **Property Name** - Bold, truncated with ellipsis
4. **Property Address** - Secondary text, abbreviated
5. **Move-in Date** - With calendar icon
6. **Duration** - In months
7. **Total Amount** - In Kwacha currency
8. **Action Buttons**:
   - View Details - Opens booking details dialog/activity
   - Cancel - Allows cancelling pending bookings

---

## Testing Checklist

- [ ] MyBookingsFragment loads bookings on app startup
- [ ] All filter buttons work and update view
- [ ] Empty state shows correct message for each filter
- [ ] Booking cards display all information correctly
- [ ] Currency shows in K (Kwacha)
- [ ] Status badges are properly styled and visible
- [ ] RecyclerView scrolls smoothly with multiple bookings
- [ ] No crashes when bookings list is empty
- [ ] Filter persists when fragment is reloaded
- [ ] Confirmed bookings can serve as receipts

---

## Database Schema

### bookings table
```sql
- id (UUID, primary key)
- tenant_id (UUID, foreign key → auth.users)
- boarding_house_id (UUID, foreign key → boarding_houses)
- move_in_date (DATE)
- end_date (DATE)
- status (TEXT: 'pending', 'confirmed', 'rejected')
- total_amount (NUMERIC)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

### boarding_houses table (needed fields)
```sql
- id (UUID)
- title (TEXT)
- address (TEXT)
- owner_id (UUID)
- name (TEXT) - alternative to title
```

---

## Future Enhancements

1. **Booking Details Dialog**
   - Show full booking information
   - Display tenant contact info
   - Show terms and conditions

2. **Cancel Booking**
   - Implement cancellation logic
   - Refund calculation if applicable
   - Owner notification

3. **Booking History**
   - Show completed bookings
   - Archive older bookings
   - Print receipt functionality

4. **Notifications**
   - Notify when booking is confirmed
   - Reminder before move-in date
   - Payment reminders

5. **Property Images**
   - Replace placeholder with actual property images
   - Load from properties_media table
   - Display in booking card

6. **Payment Integration**
   - Show payment status
   - Payment method display
   - Invoice generation

---

## Code References

- **Fragment:** `MyBookingsFragment.java`
- **Layout:** `fragment_my_bookings.xml`
- **Item Layout:** `item_booking_card.xml`
- **Adapter:** `BookingAdapter.java`
- **Model:** `Booking.java`
- **API Client:** `SupabaseClient.getTenantBookings()`
