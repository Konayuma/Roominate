# Edit Listing Feature Implementation Guide

## Overview
Owners can now click on their property cards in MyListingsFragment to edit and update those listings directly.

## Completed Changes

### 1. MyListingsFragment - Enable Navigation to Edit
**File:** `MyListingsFragment.java`
- ✅ Added `import android.content.Intent;`
- ✅ Updated PropertyAdapter callback to launch EditPropertyActivity with property ID:
```java
adapter = new PropertyAdapter(getContext(), properties, property -> {
    Intent intent = new Intent(getContext(), EditPropertyActivity.class);
    intent.putExtra("property_id", property.getId());
    startActivity(intent);
});
```

### 2. SupabaseClient - New updateProperty Method
**File:** `SupabaseClient.java` (Lines ~2360)
- ✅ Added `updateProperty(String propertyId, JSONObject updateData, ApiCallback callback)` method
- ✅ Performs PATCH request to `/rest/v1/boarding_houses?id=eq.{propertyId}`
- ✅ Updates ALL fields in one operation (name, description, address, price, amenities, etc.)
- ✅ Proper error handling and logging

### 3. EditPropertyActivity - Enhanced Save Functionality
**File:** `EditPropertyActivity.java` (Line ~436)
- ✅ Updated `savePropertyChanges()` to use new `updateProperty()` method
- ✅ Now saves all property fields: name, description, address, city, province, coordinates, pricing, rooms, amenities, contact info, utilities, etc.
- ✅ Wrapped callback in `runOnUiThread()` for proper UI updates

## Pending Implementation - Image Upload

### What's Needed
The EditPropertyActivity currently:
- ✅ Can load and display existing property data
- ✅ Can geocode addresses
- ✅ Can save all property changes

But still needs:
- ❌ Image picker UI
- ❌ Image upload functionality
- ❌ Delete/manage existing images

### Implementation Approach

#### Option A: Minimal (Recommended for MVP)
Just add a note that images are managed separately (e.g., via separate activity or future update).

#### Option B: Full Implementation
Add image handling similar to AddPropertyActivity:

1. **Add to layout** `activity_edit_property.xml`:
   - RecyclerView for image preview (horizontal)
   - MaterialButton to add/pick images
   - Optional: Camera button

2. **Add to EditPropertyActivity.java**:
   ```java
   // Variables
   private RecyclerView imagesRecyclerView;
   private MaterialButton addImageButton;
   private List<Uri> selectedImageUris = new ArrayList<>();
   private ImagePreviewAdapter imagePreviewAdapter;
   private ActivityResultLauncher<Intent> imagePickerLauncher;

   // Methods (copied from AddPropertyActivity)
   private void setupImagePicker() { ... }
   private void setupListeners() { ... } // Update to include image picker
   private void removeImage(int position) { ... }
   ```

3. **Add to SupabaseClient.java**:
   ```java
   public void uploadPropertyImages(String propertyId, List<Uri> imageUris, ApiCallback callback) {
       // Upload to Supabase Storage
       // Update properties_media table with image URLs
   }
   ```

4. **Update savePropertyChanges()** to also handle image uploads:
   ```java
   // After property fields are saved
   if (!selectedImageUris.isEmpty()) {
       supabaseClient.uploadPropertyImages(propertyId, selectedImageUris, callback);
   }
   ```

## Testing Checklist

- [ ] MyListingsFragment loads listings for owner
- [ ] Clicking property card navigates to EditPropertyActivity
- [ ] Property data loads correctly into form
- [ ] Can edit all text fields
- [ ] Can geocode address (Ndola example: "123 Street, Ndola, Copperbelt, Zambia")
- [ ] Currency displays in K (Kwacha)
- [ ] Save button updates all fields in database
- [ ] Navigation back to MyListingsFragment shows updated data

## Future Enhancements

1. **Image Management**
   - Upload new images
   - Delete existing images
   - Reorder images (set primary image)
   - Show existing images in a carousel during edit

2. **Batch Updates**
   - Update multiple properties at once
   - Bulk price changes
   - Availability status toggle

3. **History & Versions**
   - Track what changed and when
   - Ability to revert changes
   - Change notifications to tenants

## Database Schema Notes

The `boarding_houses` table schema supports all editable fields:
- name, description, address, city, province
- latitude, longitude (from geocoding)
- price_per_month, security_deposit
- total_rooms, available_rooms
- room_type, furnished, private_bathroom
- electricity_included, water_included, internet_included
- contact_person, contact_phone
- amenities (JSON array)

Images are stored separately in:
- `properties_media` table - stores image URLs with property_id reference
- `Storage` bucket - stores actual image files

## Code References

- **AddPropertyActivity.java** - Has complete image picker implementation
- **ImagePreviewAdapter.java** - RecyclerView adapter for displaying selected images
- **SupabaseClient.java** - Centralized API client for all backend calls
