// verify-otp Edge Function
// Verifies the 6-digit OTP submitted by user
// Deploy: supabase functions deploy verify-otp

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { crypto } from "https://deno.land/std@0.177.0/crypto/mod.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
  // Accept optional password to allow setting the account password during verification.
  // WARNING: sending cleartext passwords to server-side functions has security implications.
  // Only use this when you trust the transport and the function is protected. Prefer client-side signup after verify when possible.
  const { email, otp, password } = await req.json()

    if (!email || !otp) {
      return new Response(
        JSON.stringify({ error: 'Email and OTP are required' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    if (!/^\d{6}$/.test(otp)) {
      return new Response(
        JSON.stringify({ error: 'Invalid OTP format' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // Ensure OTP_SALT is configured and normalize inputs before hashing
    const otpSalt = Deno.env.get('OTP_SALT')
    if (!otpSalt) {
      console.error('Missing OTP_SALT environment variable')
      return new Response(JSON.stringify({ error: 'Server OTP configuration missing' }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // Hash the submitted OTP (normalize strings to avoid whitespace/suffix issues)
    const encoder = new TextEncoder();
    const otpStr = String(otp).trim();
    const saltStr = String(otpSalt).trim();
    const data = encoder.encode(otpStr + saltStr);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const otpHash = hashArray.map((b)=>b.toString(16).padStart(2, '0')).join('');

    console.log('Submitted OTP:', otp, '(length:', otpStr.length, ')')
    console.log('OTP type:', typeof otp)
    console.log('OTP salt length:', saltStr.length)
    console.log('OTP + Salt to hash:', `"${otpStr}${saltStr}"`)
    console.log('Generated hash:', otpHash)

    // Find the latest unused OTP record for this email
    const { data: otpRecords, error: fetchError } = await supabase
      .from('email_otps')
      .select('*')
      .eq('email', email)
      .eq('used', false)
      .order('created_at', { ascending: false })
      .limit(1)

    if (fetchError) {
      console.error('Fetch error:', fetchError)
      return new Response(
        JSON.stringify({ error: 'Verification failed' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    if (!otpRecords || otpRecords.length === 0) {
      return new Response(
        JSON.stringify({ error: 'Invalid or expired verification code' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const record = otpRecords[0]

    console.log('DB OTP hash:', record.otp_hash)
    console.log('Expires at:', record.expires_at)
    console.log('Current time:', new Date().toISOString())
    console.log('Attempts:', record.attempts)

    // Check if expired
    if (new Date(record.expires_at) < new Date()) {
      return new Response(
        JSON.stringify({ error: 'Verification code has expired' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Check if too many attempts
    if (record.attempts >= 5) {
      return new Response(
        JSON.stringify({ error: 'Too many failed attempts. Request a new code.' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Verify OTP hash
    if (record.otp_hash !== otpHash) {
      // Increment attempt counter
      await supabase
        .from('email_otps')
        .update({ attempts: record.attempts + 1 })
        .eq('id', record.id)

      return new Response(
        JSON.stringify({ 
          error: 'Invalid verification code',
          attemptsRemaining: 5 - (record.attempts + 1)
        }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // OTP is valid - mark as used (write used=true and used_at). We'll attempt the update and retry once
    const usedPayload = { used: true, used_at: new Date().toISOString() }
    let updatedRow: any = null
    let updateError: any = null

    for (let attempt = 0; attempt < 2; attempt++) {
      const { data: updatedData, error } = await supabase
        .from('email_otps')
        .update(usedPayload)
        .eq('id', record.id)
        .select('*')
        .maybeSingle()

      updateError = error
      updatedRow = updatedData

      if (!updateError) break
      console.warn('Update attempt', attempt + 1, 'failed:', updateError)
    }

    if (updateError) {
      console.error('Update error after retries:', updateError)
      // Do a final check to see if the row is already marked used (to avoid false negatives)
      try {
        const { data: checkRows, error: checkErr } = await supabase
          .from('email_otps')
          .select('used')
          .eq('id', record.id)
          .maybeSingle()

        if (checkErr || !checkRows || checkRows.used !== true) {
          return new Response(
            JSON.stringify({ error: 'Verification failed' }),
            { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
          )
        }
        // If checkRows.used is true, proceed
      } catch (err) {
        console.error('Final check failed:', err)
        return new Response(
          JSON.stringify({ error: 'Verification failed' }),
          { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )
      }
    }

    // Attempt to mark the user as confirmed in Supabase Auth via admin API
    // Optionally set the user's password if a `password` was provided in the request.
    // This requires the Service Role key to be present in the function secrets.
    let confirmed = false
    let passwordSet = false
    try {
      const serviceRole = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
      const projectUrl = Deno.env.get('SUPABASE_URL')
      if (!serviceRole || !projectUrl) {
        console.warn('Service role key or SUPABASE_URL not configured; skipping admin confirm')
      } else {
        // Try to find the user by email using admin endpoint
        const lookupUrl = `${projectUrl.replace(/\/$/, '')}/auth/v1/admin/users?email=${encodeURIComponent(email)}`
        const lookupResp = await fetch(lookupUrl, {
          method: 'GET',
          headers: {
            'apikey': serviceRole,
            'Authorization': `Bearer ${serviceRole}`,
            'Content-Type': 'application/json'
          }
        })

        if (lookupResp.ok) {
          const lookupJson = await lookupResp.json()
          // lookupJson may be an array or single object depending on API; handle both
          const userObj = Array.isArray(lookupJson) ? lookupJson[0] : lookupJson
          if (userObj && userObj.id) {
            const userId = userObj.id
            const now = new Date().toISOString()

            // Merge existing user_metadata and set email_verified = true
            const existingMeta = userObj.user_metadata ?? {}
            const newMeta = { ...existingMeta, email_verified: true }

            // Build patch body. If password provided, include it (do not log)
            const patchBody: any = { email_confirmed_at: now, user_metadata: newMeta }
            if (password && typeof password === 'string' && password.length > 0) {
              patchBody.password = password
            }

            const patchUrl = `${projectUrl.replace(/\/$/, '')}/auth/v1/admin/users/${userId}`
            const patchResp = await fetch(patchUrl, {
              method: 'PATCH',
              headers: {
                'apikey': serviceRole,
                'Authorization': `Bearer ${serviceRole}`,
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(patchBody)
            })

            if (patchResp.ok) {
              confirmed = true
              if (patchBody.password) passwordSet = true
              console.log('User', userId, 'marked confirmed via admin API')
            } else {
              const txt = await patchResp.text()
              console.error('Failed to confirm user via admin API:', patchResp.status, txt)
            }
          } else {
            console.warn('No user object returned from admin lookup for email', email)
          }
        } else {
          const txt = await lookupResp.text()
          console.error('User lookup via admin API failed:', lookupResp.status, txt)
        }
      }
    } catch (err) {
      console.error('Admin confirm error:', err)
    }

    return new Response(
      JSON.stringify({ 
        success: true, 
        message: 'Email verified successfully',
        email: email,
        confirmed,
        password_set: passwordSet
      }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error' }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
