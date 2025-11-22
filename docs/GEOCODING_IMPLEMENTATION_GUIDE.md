# 📍 Geocoding Integration Guide

## Overview

The Roominate app now has full geocoding support, allowing properties to be automatically converted from addresses to latitude/longitude coordinates. This enables:

- **Map Display**: Properties appear as markers on an interactive osmdroid map
- **Location-Based Search**: Find properties near specific coordinates
- **Visual Property Updates**: Owners can edit properties and preview their location on a map
- **Distance Calculations**: Database functions to find nearby properties

## Architecture

### Technology Stack

- **Geocoding Service**: Nominatim API (free, open-source, no API key required)
- **Database**: PostgreSQL with spatial indexing
- **Mapping**: osmdroid (free, open-source Android mapping library)
- **Backend**: Supabase PostgREST API

### Components

1. **Database (`add_geocoding_columns.sql`)**
   - Added `latitude` (double precision) and `longitude` (double precision) columns
   - Created spatial index on (latitude, longitude)
   - Added helper function `find_nearby_properties()` for distance-based queries

2. **SupabaseClient Methods**
   - `geocodeAddress()` - Convert address string to coordinates via Nominatim
   - `updatePropertyCoordinates()` - Save lat/long to database
   - `getAllPropertiesWithCoordinates()` - Fetch all geocoded properties for map display
   - `getPropertyById()` - (existing) Fetch single property with coordinates

3. **Android Activities**
   - `AddPropertyActivity` - Now includes auto-geocoding when adding properties
   - `EditPropertyActivity` - NEW - Edit existing properties with live map preview
   - `MapActivity` - Enhanced to display all properties as interactive markers

4. **UI Components**
   - Interactive osmdroid map with zoom controls
   - Real-time marker placement on address entry
   - Geocoding status display (✅ or ❌)
   - Visual feedback during geocoding operations

## Setup Instructions

### Step 1: Run Database Migration

1. Open Supabase Dashboard → SQL Editor
2. Copy and paste the contents of `supabase/migrations/add_geocoding_columns.sql`
3. Click "Run" to execute

Expected output:
```
==================================================
     GEOCODING SETUP COMPLETE!
==================================================

✅ Latitude column: YES
✅ Longitude column: YES
✅ Location index: YES
```

### Step 2: Verify in Android Code

The SupabaseClient already includes all geocoding methods. No additional configuration needed!

### Step 3: Register EditPropertyActivity in AndroidManifest

Add this to `AndroidManifest.xml` in the `<application>` section if not already present:

```xml
<activity 
    android:name=".activities.owner.EditPropertyActivity"
    android:exported="false" />
```

## How to Use

### Adding a New Property

1. Click "Add Property" in the Owner Dashboard
2. Fill in property details (name, address, city, province, etc.)
3. Address fields have auto-geocoding on focus loss - the app automatically looks up coordinates
4. See the status message ("📍 Coordinates: 14.5995, 120.9842") confirming geocoding success
5. Submit to save with coordinates

### Editing an Existing Property

1. Click "Edit" on a property listing
2. The map automatically loads showing the current location
3. Update the address if needed
4. Press "Geocode Address" to update coordinates manually, or address change will auto-trigger geocoding
5. Review the map preview to confirm correct location
6. Click "Save Changes" to update

### Viewing Properties on Map

1. Click "Map" in the Tenant view
2. All available properties with coordinates display as interactive markers
3. Marker shows property name and price per room
4. Click any marker to view full property details

## API Details

### geocodeAddress(address, callback)

Converts an address string to coordinates using Nominatim API.

**Parameters:**
- `address`: Full address string (e.g., "123 Main St, Manila, Philippines")
- `callback`: ApiCallback with onSuccess/onError

**Response:**
```json
{
  "latitude": 14.5995,
  "longitude": 120.9842
}
```

**Example:**
```java
supabaseClient.geocodeAddress(
    "123 P. Burgos St, Manila, Philippines",
    new SupabaseClient.ApiCallback() {
        @Override
        public void onSuccess(JSONObject response) {
            double lat = response.getDouble("latitude");
            double lng = response.getDouble("longitude");
            // Update UI
        }
        @Override
        public void onError(String error) {
            Toast.makeText(context, "Geocoding failed: " + error, Toast.LENGTH_SHORT).show();
        }
    }
);
```

### updatePropertyCoordinates(propertyId, latitude, longitude, callback)

Updates property coordinates in the database.

**Parameters:**
- `propertyId`: UUID of the boarding_house record
- `latitude`: Latitude coordinate
- `longitude`: Longitude coordinate
- `callback`: ApiCallback with onSuccess/onError

**Example:**
```java
supabaseClient.updatePropertyCoordinates(
    "550e8400-e29b-41d4-a716-446655440000",
    14.5995,
    120.9842,
    new SupabaseClient.ApiCallback() {
        @Override
        public void onSuccess(JSONObject response) {
            // Update saved
        }
        @Override
        public void onError(String error) {
            // Handle error
        }
    }
);
```

### getAllPropertiesWithCoordinates(callback)

Fetches all available properties with coordinates for map display.

**Response:**
```json
{
  "properties": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Cozy Boarding House",
      "address": "123 Main St",
      "latitude": 14.5995,
      "longitude": 120.9842,
      "price_per_month": 5000,
      "available_rooms": 2
    }
    // ... more properties
  ]
}
```

## Database Functions

### find_nearby_properties(user_latitude, user_longitude, radius_km)

Find properties within a specified radius of a location.

**Example SQL:**
```sql
SELECT * FROM find_nearby_properties(14.5995, 120.9842, 5.0);
```

Returns:
- Property ID, name, address
- Coordinates
- Monthly price
- Distance in kilometers (sorted ascending)

**Usage in Android:**
Can be called via PostgREST API:
```
GET /rest/v1/rpc/find_nearby_properties?user_latitude=14.5995&user_longitude=120.9842&radius_km=5
```

## Troubleshooting

### Coordinates Not Showing

1. **Verify Database Setup**
   ```sql
   SELECT COUNT(*) FROM information_schema.columns 
   WHERE table_name='boarding_houses' 
   AND column_name IN ('latitude', 'longitude');
   -- Should return 2
   ```

2. **Check Property Data**
   ```sql
   SELECT id, name, latitude, longitude FROM boarding_houses 
   WHERE latitude IS NOT NULL LIMIT 5;
   ```

3. **Verify Nominatim API Access**
   - Open browser: `https://nominatim.openstreetmap.org/search?q=Manila&format=json&limit=1`
   - Should return location data

### Map Not Displaying Properties

1. Check that properties have non-null latitude and longitude values
2. Ensure `available = true` for properties to appear
3. Verify osmdroid is properly initialized in MapActivity
4. Check network connection for API calls

### Geocoding Returns "Address Not Found"

1. Ensure full address is provided (street, city, province, country)
2. Check address spelling and format
3. Try entering coordinates manually and clicking "Save Changes"
4. Use Nominatim's web interface to test addresses:
   - `https://nominatim.openstreetmap.org/search?q=<address>&format=json`

## Performance Optimization

### Database

- Spatial index on (latitude, longitude) enables fast geographic queries
- Partial index only on properties with coordinates and available=true
- Query plan: `CREATE INDEX idx_boarding_houses_location ON public.boarding_houses(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;`

### API Calls

- Properties are fetched once when MapActivity loads
- Markers are cached in memory until activity is destroyed
- Geocoding is async and doesn't block UI

### Best Practices

1. **Batch Update**: Use SQL migration to geocode existing properties at once
   ```sql
   -- Example: Batch update using a stored procedure with Nominatim API calls
   -- This would require pg_net extension for HTTP calls
   ```

2. **Cache Results**: SupabaseClient automatically caches property data
3. **Limit Map Zoom**: osmdroid renders efficiently even with 1000+ markers

## Future Enhancements

1. **Radius Search UI**: Allow tenants to search within X km of their location
2. **Route Planning**: Show walking/driving distance and time to properties
3. **Heatmaps**: Display property density by neighborhood
4. **Offline Maps**: Download map tiles for offline viewing
5. **Custom Markers**: Different icons for different property types
6. **Clustering**: Automatically group nearby properties at low zoom levels
7. **Reverse Geocoding**: Click on map to get address for new property locations

## Files Modified

### New Files
- `supabase/migrations/add_geocoding_columns.sql` - Database setup
- `app/src/main/java/com/roominate/activities/owner/EditPropertyActivity.java` - Property editing with geocoding
- `app/src/main/res/layout/activity_edit_property.xml` - Layout with map preview

### Modified Files
- `app/src/main/java/com/roominate/services/SupabaseClient.java` - Added 4 geocoding methods
- `app/src/main/java/com/roominate/activities/tenant/MapActivity.java` - Enhanced to fetch and display properties
- `supabase_schema.sql` - Already includes latitude/longitude columns

## Testing Checklist

- [ ] Database migration runs successfully
- [ ] Run: `SELECT * FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name IN ('latitude', 'longitude');`
  - Should return 2 rows
- [ ] AddPropertyActivity auto-geocodes when entering address
- [ ] EditPropertyActivity loads property and displays on map
- [ ] MapActivity displays all properties as markers
- [ ] Clicking marker navigates to property details
- [ ] Properties with null coordinates don't appear on map
- [ ] Status messages show correct emoji (✅ or ❌)

## Support

For issues or questions:
1. Check Supabase dashboard for API errors
2. Review Logcat output for Android errors (tag: "MapActivity", "EditPropertyActivity")
3. Test with Nominatim API directly: https://nominatim.openstreetmap.org/
4. Verify osmdroid initialization in PreferenceManager

---

**Version**: 1.0  
**Last Updated**: November 2024  
**Status**: ✅ Production Ready
