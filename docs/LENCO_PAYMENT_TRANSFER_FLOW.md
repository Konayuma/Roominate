# Lenco Payment and Transfer Flow

## Overview
Complete implementation of payment collection from tenants and automatic transfer to property owners using Lenco API.

## Architecture

```
Tenant Payment → Lenco Collection → Webhook → Transfer to Owner
     (Mobile Money)     (Platform)    (Automated)   (Mobile Money)
```

## Database Schema Updates

### New Columns in `bookings` Table

```sql
-- Payment tracking
payment_reference text          -- Unique reference from Lenco collection
payment_status text             -- pending | successful | failed | cancelled
payment_method text             -- mobile-money
payment_amount numeric(10,2)    -- Actual amount paid
payment_date timestamptz        -- When payment was confirmed

-- Transfer tracking
transfer_reference text         -- Unique reference for transfer to owner
transfer_status text            -- pending | successful | failed
transfer_amount numeric(10,2)   -- Amount transferred (after platform fee)
transfer_date timestamptz       -- When transfer was initiated

-- Owner reference
owner_id uuid                   -- Property owner for quick lookup
```

## Payment Flow

### 1. Tenant Initiates Payment

**Endpoint:** `lenco-payment` Edge Function

```typescript
POST /functions/v1/lenco-payment
{
  "booking_id": "uuid",
  "amount": 500,
  "currency": "ZMW",
  "email": "tenant@example.com",
  "phone_number": "0961234567",
  "first_name": "John",
  "last_name": "Doe"
}
```

**Process:**
1. Validates booking and payment details
2. Auto-detects mobile operator (Airtel/MTN) from phone prefix
3. Generates unique payment reference: `RMN-{booking_id}-{timestamp}`
4. Calls Lenco Mobile Money Collection API
5. Updates booking with `payment_reference` and `payment_status: 'pending'`
6. Returns payment status to client

**Lenco API Call:**
```http
POST https://api.lenco.co/access/v2/collections/mobile-money
Authorization: Bearer {LENCO_API_KEY}
Content-Type: application/json

{
  "amount": 500,
  "reference": "RMN-abc123-1234567890",
  "phone": "0961234567",
  "operator": "airtel",
  "country": "zm",
  "bearer": "merchant"
}
```

**Response:**
```json
{
  "success": true,
  "reference": "RMN-abc123-1234567890",
  "requires_authorization": true,
  "payment_status": "pay-offline",
  "message": "Please check your phone and authorize the payment"
}
```

### 2. Lenco Webhook Notification

**Endpoint:** `lenco-webhook` Edge Function  
**URL:** `https://[project-ref].supabase.co/functions/v1/lenco-webhook`

Configure this URL in your Lenco Dashboard under Webhooks.

**Webhook Payload Examples:**

```json
// Successful payment
{
  "event": "collection.successful",
  "data": {
    "reference": "RMN-abc123-1234567890",
    "amount": 500,
    "status": "successful",
    "customer": {
      "phone": "0961234567"
    }
  }
}

// Failed payment
{
  "event": "collection.failed",
  "data": {
    "reference": "RMN-abc123-1234567890",
    "status": "failed"
  }
}
```

**Process:**
1. Receives webhook from Lenco
2. Extracts payment reference and status
3. Updates booking payment status
4. **If payment successful:**
   - Fetches booking and owner details
   - Calculates platform fee (10%)
   - Initiates transfer to owner
   - Sends notification to owner

### 3. Transfer to Owner

**Triggered automatically** by successful webhook.

**Lenco Transfer API Call:**
```http
POST https://api.lenco.co/access/v2/transfers/mobile-money
Authorization: Bearer {LENCO_API_KEY}
Content-Type: application/json

{
  "amount": 450,
  "reference": "TRF-abc12345-1234567890",
  "phone": "0971234567",
  "operator": "airtel",
  "country": "zm",
  "narration": "Booking payment - Property Title"
}
```

**Transfer Calculation:**
```
Payment Amount: K500
Platform Fee (10%): K50
Transfer to Owner: K450
```

**Process:**
1. Fetches owner phone from `users` table via `owner_id`
2. Auto-detects owner's mobile operator
3. Calculates transfer amount (payment - platform fee)
4. Generates unique transfer reference: `TRF-{booking_id}-{timestamp}`
5. Calls Lenco Mobile Money Transfer API
6. Updates booking with transfer details
7. Creates notification for owner

## Platform Fee Configuration

Currently set at **10%** in the webhook handler:

```typescript
const platformFeePercent = 10; // 10% platform fee
const platformFee = (paymentAmount * platformFeePercent) / 100;
const transferAmount = paymentAmount - platformFee;
```

**To change the fee:**
1. Update `platformFeePercent` in `lenco-webhook/index.ts`
2. Redeploy the function

## Mobile Operator Detection

Automatically detects Zambian mobile operators from phone number prefixes:

```typescript
Airtel: 096, 097, 076, 077
MTN:    095, 096
```

Supports both local (096...) and international (260...) formats.

## Required Environment Variables

### Lenco Credentials
```bash
LENCO_API_KEY=your_lenco_api_key_here
LENCO_WEBHOOK_SECRET=optional_for_signature_verification
```

### Supabase Credentials
```bash
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SERVICE_ROLE_KEY=your_service_role_key_here
```

## Deployment Steps

### 1. Run Database Migration

```bash
supabase migration new add_payment_tracking
# Copy content from migrations/add_payment_tracking.sql
supabase db push
```

Or manually run the SQL:

```bash
psql -h db.your-project.supabase.co -U postgres -d postgres -f supabase/migrations/add_payment_tracking.sql
```

### 2. Deploy Edge Functions

```bash
# Deploy payment function
supabase functions deploy lenco-payment --no-verify-jwt

# Deploy webhook function
supabase functions deploy lenco-webhook --no-verify-jwt
```

### 3. Set Environment Variables

```bash
# Set Lenco API key
supabase secrets set LENCO_API_KEY=your_lenco_api_key

# Supabase credentials (usually already set)
supabase secrets set SUPABASE_URL=https://your-project.supabase.co
supabase secrets set SUPABASE_SERVICE_ROLE_KEY=your_service_role_key
```

### 4. Configure Lenco Webhook

1. Log into Lenco Dashboard
2. Go to Settings → Webhooks
3. Add new webhook URL:
   ```
   https://your-project.supabase.co/functions/v1/lenco-webhook
   ```
4. Select events to listen to:
   - `collection.successful`
   - `collection.failed`
   - `collection.cancelled`

## Testing

### Test Payment Flow

```bash
# 1. Initiate payment
curl -X POST https://your-project.supabase.co/functions/v1/lenco-payment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ANON_KEY" \
  -d '{
    "booking_id": "test-booking-123",
    "amount": 100,
    "currency": "ZMW",
    "email": "test@example.com",
    "phone_number": "0961234567",
    "first_name": "Test",
    "last_name": "User"
  }'

# 2. Check booking status
# Query bookings table for payment_reference

# 3. Simulate webhook (for testing)
curl -X POST https://your-project.supabase.co/functions/v1/lenco-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "event": "collection.successful",
    "data": {
      "reference": "RMN-test-booking-123-1234567890",
      "amount": 100,
      "status": "successful"
    }
  }'

# 4. Check transfer status
# Query bookings table for transfer_reference and transfer_status
```

### Verify in Database

```sql
-- Check payment status
SELECT 
  id,
  payment_reference,
  payment_status,
  payment_amount,
  payment_date,
  transfer_reference,
  transfer_status,
  transfer_amount,
  transfer_date
FROM bookings
WHERE payment_reference = 'RMN-test-booking-123-1234567890';

-- Check notifications
SELECT * FROM notifications
WHERE data->>'type' = 'payment'
ORDER BY created_at DESC
LIMIT 5;
```

## Monitoring

### View Function Logs

```bash
# Payment function logs
supabase functions logs lenco-payment

# Webhook function logs
supabase functions logs lenco-webhook
```

### Key Log Messages

**Payment Initiation:**
```
Initiating Lenco mobile money payment for booking {id} with reference {ref}
Detected operator: airtel for phone: 0961234567
Lenco API Response Status: 200
```

**Webhook Processing:**
```
lenco-webhook received: {...}
Booking updated for reference {ref} newStatus completed
Payment successful, initiating transfer to owner...
Owner found: John Doe, Phone: 0971234567
Transfer calculation: Payment=500, Fee=50 (10%), Transfer=450
Transfer initiated successfully: TRF-abc-123
```

## Error Handling

### Payment Failures
- **Network Error:** Retry payment initiation
- **Invalid Phone:** Check phone format (096... or 260...)
- **Insufficient Balance:** User needs to fund mobile money account

### Transfer Failures
- **Owner Phone Missing:** Update owner profile with phone number
- **Invalid Operator:** Check phone number format
- **Lenco API Error:** Check logs, may need manual transfer

### Webhook Issues
- **Duplicate Webhooks:** System checks transfer_status to prevent double-processing
- **Missing Reference:** Check Lenco payload format
- **Database Update Failed:** Check RLS policies and service role key

## Security Considerations

1. **Webhook Verification:** 
   - Implement signature verification using `LENCO_WEBHOOK_SECRET`
   - Verify webhook source IP (Lenco IP ranges)

2. **Amount Validation:**
   - Verify webhook amount matches booking amount
   - Prevent amount manipulation

3. **Idempotency:**
   - Check `transfer_status` before processing
   - Prevents duplicate transfers

4. **Access Control:**
   - Edge functions use service role key
   - Bypass RLS for system operations

## API Reference

### Lenco Mobile Money Collection
**Documentation:** https://lenco-api.readme.io/v2.0/reference/accept-mobile-money-payments

**Endpoint:** `POST https://api.lenco.co/access/v2/collections/mobile-money`

**Required Fields:**
- `amount` - Amount in base currency (Kwacha)
- `reference` - Unique transaction reference
- `phone` - Customer phone number
- `operator` - Mobile operator (airtel/mtn)
- `country` - Country code (zm for Zambia)
- `bearer` - Who pays fees (merchant/customer)

### Lenco Mobile Money Transfer
**Documentation:** https://lenco-api.readme.io/v2.0/reference/initiate-transfer-to-mobile-money

**Endpoint:** `POST https://api.lenco.co/access/v2/transfers/mobile-money`

**Required Fields:**
- `amount` - Amount to transfer
- `reference` - Unique transfer reference
- `phone` - Recipient phone number
- `operator` - Mobile operator (airtel/mtn)
- `country` - Country code (zm)
- `narration` - Transfer description (optional)

## Support

### Common Issues

**Q: Payment collected but no transfer initiated**  
A: Check webhook logs. Ensure owner has phone number in profile. Verify LENCO_API_KEY is set.

**Q: Transfer fails with "invalid phone number"**  
A: Verify owner's phone format. Should be 096... or 260... format.

**Q: Platform fee not applied**  
A: Check `platformFeePercent` in webhook function. Redeploy if changed.

**Q: Duplicate transfers**  
A: System checks `transfer_status`. If issue persists, check webhook deduplication.

## Roadmap

- [ ] Configurable platform fees per property/owner
- [ ] Support for multiple currencies
- [ ] Escrow/hold period before transfer
- [ ] Refund handling
- [ ] Transfer retry mechanism
- [ ] Admin dashboard for payment monitoring
- [ ] Automatic reconciliation reports
