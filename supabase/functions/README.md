# Supabase Edge Functions Setup

## Prerequisites
- Supabase CLI installed: `npm install -g supabase`
- Supabase project created
- Environment variables configured

## Environment Variables

Set these in your Supabase project dashboard (Settings → Edge Functions → Secrets):

```bash
OTP_SALT=your-random-salt-string-here-change-this
RESEND_API_KEY=your-resend-api-key  # if using Resend
SENDGRID_API_KEY=your-sendgrid-key  # if using SendGrid
```

## Deployment

### 1. Link your Supabase project
```bash
supabase link --project-ref your-project-ref
```

### 2. Deploy functions
```bash
supabase functions deploy send-otp
supabase functions deploy verify-otp
```

### 3. Get function URLs
After deployment, your functions will be available at:
- `https://your-project-ref.supabase.co/functions/v1/send-otp`
- `https://your-project-ref.supabase.co/functions/v1/verify-otp`

## Testing Locally

### 1. Start Supabase locally
```bash
supabase start
supabase functions serve
```

### 2. Test send-otp
```bash
curl -X POST http://localhost:54321/functions/v1/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

### 3. Test verify-otp
```bash
curl -X POST http://localhost:54321/functions/v1/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","otp":"123456"}'
```

## Email Configuration

### Option 1: Resend (Recommended for Deno)
1. Sign up at https://resend.com
2. Get API key
3. Uncomment Resend section in `send-otp/index.ts`
4. Set `RESEND_API_KEY` environment variable

### Option 2: SendGrid
1. Sign up at https://sendgrid.com
2. Get API key
3. Uncomment SendGrid section in `send-otp/index.ts`
4. Set `SENDGRID_API_KEY` environment variable

### Option 3: SMTP
For custom SMTP, you'll need to add an SMTP library compatible with Deno.

## Security Notes

1. **Never expose service_role key** - it's only used in Edge Functions
2. **Rate limiting** - send-otp limits 3 requests per 10 minutes per email
3. **OTP expiry** - codes expire after 5 minutes
4. **Attempt limiting** - verify-otp allows max 5 attempts per code
5. **Hashing** - OTPs are hashed before storage using SHA-256 + salt

## Database Setup

Run the SQL schema in Supabase SQL Editor:
```bash
# Copy contents of supabase_schema.sql to Supabase SQL Editor and execute
```

## Troubleshooting

### Function logs
```bash
supabase functions logs send-otp
supabase functions logs verify-otp
```

### Common issues
1. **CORS errors**: Functions include CORS headers by default
2. **Environment variables**: Make sure all required env vars are set
3. **Database permissions**: Service role key bypasses RLS
4. **Email not sending**: Check email provider logs and API keys
