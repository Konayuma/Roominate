# Property Loading Flow - Edit Property Activity

## How It Works

When you click on a property card to edit it:

```
PropertyCard Click
    ↓
OwnerHomeFragment.onClick()
    ↓
Intent to EditPropertyActivity with "property_id" extra
    ↓
EditPropertyActivity.onCreate()
    ↓
getPropertyById(propertyId) API call
    ↓
loadPropertyData() in runOnUiThread() 
    ↓
Parse JSON response and extract "data" array
    ↓
populateFormFields() - setText() all input boxes
    ↓
Display on Screen ✓
```

## The Fix

The issue was in the JSON response parsing. The Supabase API response comes wrapped in a structure like:

```json
{
  "data": [
    {
      "id": "16a881e4-1147-4e46-b87a-ade199f53863",
      "name": "wayne",
      "address": "mushili",
      "city": "ndola",
      "province": "Copperbelt",
      "price_per_month": 3000.00,
      "security_deposit": 2000.00,
      "total_rooms": 27,
      "available_rooms": 5,
      "room_type": "Single",
      "furnished": true,
      "private_bathroom": true,
      "electricity_included": true,
      "water_included": true,
      "internet_included": true,
      "contact_person": "wayne",
      "contact_phone": "0736391957",
      "amenities": ["WiFi", "Air Conditioning", "Laundry", "24/7 Security", "Water Heater"],
      "latitude": -13.1234,
      "longitude": 29.6543
    }
  ]
}
```

**Key Fix:** Extract the array first with `response.optJSONArray("data")`, then get the first object with `dataArray.getJSONObject(0)`.

## Code Changes

**File:** `EditPropertyActivity.java` (lines 175-245)

**Before:**
```java
currentProperty = parsePropertyFromJson(response);  // Wrong! response is wrapper, not property
```

**After:**
```java
JSONArray dataArray = response.optJSONArray("data");
if (dataArray == null || dataArray.length() == 0) {
    // Handle error
    return;
}
JSONObject propertyJson = dataArray.getJSONObject(0);
currentProperty = parsePropertyFromJson(propertyJson);  // Correct!
```

## Threading Safety

All UI operations are wrapped in `runOnUiThread(() -> { ... })` to ensure:
- ✅ No "Animators may only be run on Looper threads" errors
- ✅ No "Can't toast on a thread that has not called Looper.prepare()" errors
- ✅ Text setting, visibility changes, animations all run on main thread

## What Gets Populated

When a property loads, these fields are automatically populated:

- ✅ Property Name
- ✅ Description
- ✅ Address, City, Province
- ✅ Monthly Rate & Security Deposit
- ✅ Total Rooms & Available Rooms
- ✅ Room Type (Spinner selection)
- ✅ Furnished, Private Bathroom, Electricity, Water, Internet (Checkboxes)
- ✅ Amenities (Chips - multi-select)
- ✅ Contact Person & Phone
- ✅ Map with geocoded coordinates marker
- ✅ Geocoding Status Display

## Testing

1. Go to Owner Dashboard
2. Click "My Listings"
3. Click any property card
4. Should see all fields auto-populated
5. Map should show the property location
6. You can now edit any field and click "Save Changes"

## Error Handling

If anything goes wrong:
- Network error → Shows error toast and logs details
- Empty response → Shows "Property not found"
- Parse error → Shows "Error loading property"
- Any exception → Caught and logged with full stack trace

All errors appear as Toast messages with detailed information for debugging.
