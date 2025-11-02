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
    const { email, otp } = await req.json()

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

    // Hash the submitted OTP
    const encoder = new TextEncoder()
    const data = encoder.encode(otp + Deno.env.get('OTP_SALT'))
    const hashBuffer = await crypto.subtle.digest('SHA-256', data)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    const otpHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')

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

    // Optional: Create or update user in auth.users
    // This depends on your flow - you might create the user after password is set
    // For now, just return success

    return new Response(
      JSON.stringify({ 
        success: true, 
        message: 'Email verified successfully',
        email: email
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
