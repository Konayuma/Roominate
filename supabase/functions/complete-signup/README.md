Complete-signup Edge Function

Purpose
- After OTP verification, create a confirmed Supabase Auth user using the service_role key and attach user_metadata.
- This function requires that the OTP was already verified and that the corresponding `public.email_otps` row has `used = true` and is recent (the function checks the latest used OTP within the last hour).

Environment variables (set in Supabase or your server environment)
- SUPABASE_URL: https://<project>.supabase.co
- SUPABASE_SERVICE_ROLE_KEY: your Supabase service_role key (KEEP SECRET)

How it works
1. The function expects a POST JSON payload:
   {
     "email": "user@example.com",
     "password": "UserPassword123!",
     "first_name": "First",
     "last_name": "Last",
     "role": "tenant",
     "dob": "2000-01-01",
     "phone": "0123456789"
   }
2. It queries `public.email_otps` (via the REST API) for the latest OTP record for `email`.
3. It requires that the row has `used = true` and was created within the last hour.
4. If verified, it calls the Supabase admin endpoint `/auth/v1/admin/users` with the service_role key to create the user and mark email confirmed.

Deployment (Supabase Functions)
- Copy `index.js` into a new function in Supabase Functions (or using the CLI), set the required environment variables, and deploy.

Notes and next steps
- Ensure your existing `verify-otp` Edge Function (the one that checks OTPs) marks the `public.email_otps.used` column to true when the OTP is validated. The `complete-signup` function depends on that signal.
- If you prefer an atomic verify+create function (verify OTP and create user in the same call), merge your verify logic into this function to check the OTP value and then proceed to create the user.
- Keep the service_role key secret; do NOT call `/auth/v1/admin/users` directly from the client.

Testing
1. Verify an OTP using your current verify flow (which should mark used=true).
2. Call this function (POST) with the same email and the password the user entered.
3. On success the function returns { success: true, user: <created user> }.
4. Then sign-in from the client should succeed.
