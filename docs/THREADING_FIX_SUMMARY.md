# Threading Fix for EditPropertyActivity

## Issue
The app was crashing with the error:
```
java.lang.NullPointerException: Can't toast on a thread that has not called Looper.prepare()
android.animation.AndroidRuntimeException: Animators may only be run on Looper threads
```

## Root Cause
All callback methods from `SupabaseClient.ApiCallback` execute on the **OkHttp Dispatcher thread** (a background thread), not the main UI thread. When attempting to:
- Set text on EditText fields
- Show Toast messages  
- Update View visibility
- Run animations (via Material Design TextInputLayout)

These UI operations throw exceptions because they must run on the main thread.

## Solution
Wrapped all UI operations in `runOnUiThread()` lambda blocks in three callback locations:

### 1. **loadPropertyData()** - Lines 174-245
- Wrapped entire `onSuccess()` body with `runOnUiThread()`
- Wrapped entire `onError()` body with `runOnUiThread()`
- Ensures all `setText()`, `setVisibility()`, and `Toast` calls run on main thread

### 2. **geocodeAddressFromForm()** - Lines 295-323  
- Wrapped entire `onSuccess()` body with `runOnUiThread()`
- Wrapped entire `onError()` body with `runOnUiThread()`
- Protects map update operations and geocoding status updates

### 3. **savePropertyChanges()** - Lines 425-445
- Wrapped entire `onSuccess()` body with `runOnUiThread()`
- Wrapped entire `onError()` body with `runOnUiThread()`
- Ensures property save completion and error messages display correctly

## Pattern
```java
supabaseClient.methodName(params, new SupabaseClient.ApiCallback() {
    @Override
    public void onSuccess(JSONObject response) {
        runOnUiThread(() -> {
            // All UI operations here
            progressBar.setVisibility(View.GONE);
            Toast.makeText(EditPropertyActivity.this, "Success!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            // All UI operations here
            progressBar.setVisibility(View.GONE);
            Toast.makeText(EditPropertyActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
        });
    }
});
```

## Testing
After this fix, the EditPropertyActivity should:
1. Load property data without crashing
2. Display geocoded coordinates properly
3. Show success/error messages on property save
4. Execute all animations smoothly on the main thread
