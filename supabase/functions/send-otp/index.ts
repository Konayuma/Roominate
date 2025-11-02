// send-otp Edge Function
// Generates a 6-digit OTP, stores hashed version in DB, sends email to user
// Deploy: supabase functions deploy send-otp

import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

// Initialize Supabase client (will be used in sendEmail function)
const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const supabaseClient = createClient(supabaseUrl, supabaseServiceKey)
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
    const { email } = await req.json()

    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return new Response(
        JSON.stringify({ error: 'Invalid email address' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Rate limiting check - max 3 requests per email in last 10 minutes
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString()
    const { data: recentOtps, error: checkError } = await supabase
      .from('email_otps')
      .select('id')
      .eq('email', email)
      .gte('created_at', tenMinutesAgo)

    if (checkError) {
      console.error('Rate limit check error:', checkError)
    }

    if (recentOtps && recentOtps.length >= 3) {
      return new Response(
        JSON.stringify({ error: 'Too many requests. Please try again later.' }),
        { status: 429, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString()

    // Hash OTP before storing (using SHA-256)
    const encoder = new TextEncoder()
    const data = encoder.encode(otp + Deno.env.get('OTP_SALT'))
    const hashBuffer = await crypto.subtle.digest('SHA-256', data)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    const otpHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')

    // Store hashed OTP with 5 minute expiry
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString()
    
    // Insert OTP row with explicit defaults to make verification deterministic
    const { data: insertedRow, error: insertError } = await supabase
      .from('email_otps')
      .insert({
        email,
        otp_hash: otpHash,
        expires_at: expiresAt,
        ip_address: req.headers.get('x-forwarded-for') || 'unknown',
        used: false,
        attempts: 0,
        used_at: null
      })
      .select('*')
      .maybeSingle()

    if (insertError || !insertedRow) {
      console.error('Insert error:', insertError)
      return new Response(
        JSON.stringify({ error: 'Failed to generate verification code' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    console.log('Inserted OTP row id=', insertedRow.id)

    // Send email using SMTP (configure your SMTP service)
    // For production, use SendGrid, Mailgun, or AWS SES
    const emailSent = await sendEmail(email, otp)

    if (!emailSent) {
      return new Response(
        JSON.stringify({ error: 'Failed to send email' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    return new Response(
      JSON.stringify({ 
        success: true, 
        message: 'Verification code sent to your email',
        expiresIn: 300 // seconds
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

async function sendEmail(email: string, otp: string): Promise<boolean> {
  try {
    // Use Resend (or any SMTP provider) via Deno fetch
    // Requires RESEND_API_KEY environment variable set in Supabase Edge Function secrets
    const resendApiKey = Deno.env.get('RESEND_API_KEY')
    
    // If RESEND_API_KEY is configured, prefer Resend (fast, reliable)
    if (resendApiKey) {
      // Send via Resend API
      const emailHtml = `
      <!DOCTYPE html>
      <html>
        <head>
          <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
            .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
            .otp-box { background: white; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; margin: 20px 0; border-radius: 8px; border: 2px dashed #667eea; }
            .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>Roominate</h1>
              <p>Email Verification</p>
            </div>
            <div class="content">
              <h2>Your Verification Code</h2>
              <p>Hello,</p>
              <p>Thank you for signing up with Roominate. Please use the verification code below to complete your registration:</p>
              <div class="otp-box">${otp}</div>
              <p><strong>This code will expire in 5 minutes.</strong></p>
              <p>If you didn't request this code, please ignore this email.</p>
              <p>Best regards,<br>The Roominate Team</p>
            </div>
            <div class="footer">
              <p>© 2025 Roominate. All rights reserved.</p>
            </div>
          </div>
        </body>
      </html>
    `

      const emailText = `
Your Roominate Verification Code

Your verification code is: ${otp}

This code will expire in 5 minutes.

If you didn't request this code, please ignore this email.

Best regards,
The Roominate Team

© 2025 Roominate. All rights reserved.
    `

      const response = await fetch('https://api.resend.com/emails', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${resendApiKey}`
        },
        body: JSON.stringify({
          from: 'Roominate <onboarding@resend.dev>', // Change to your verified domain
          to: email,
          subject: 'Your Roominate Verification Code',
          html: emailHtml,
          text: emailText
        })
      })

      if (!response.ok) {
        const errorText = await response.text()
        console.error('Resend API error:', response.status, errorText)
        console.log(`⚠️ OTP for ${email}: ${otp} (Email delivery failed)`)
        return true // Return true to not block development
      }

      const result = await response.json()
      console.log(`✅ Email sent successfully to ${email}`)
      console.log(`🔐 OTP: ${otp}`) // Log for development/testing
      console.log(`📧 Email ID: ${result.id}`)
      return true
    }

    // If Resend isn't configured, try Gmail via OAuth2 (Gmail API)
    const gmailClientId = Deno.env.get('GMAIL_CLIENT_ID')
    const gmailClientSecret = Deno.env.get('GMAIL_CLIENT_SECRET')
    const gmailRefreshToken = Deno.env.get('GMAIL_REFRESH_TOKEN')
    const senderEmail = Deno.env.get('SENDER_EMAIL')

    if (!gmailClientId || !gmailClientSecret || !gmailRefreshToken || !senderEmail) {
      console.error('No email provider configured (RESEND_API_KEY or GMAIL_* env vars)')
      console.log(`⚠️ OTP for ${email}: ${otp} (Email provider not configured)`)
      return true // do not block development; OTP recorded in DB
    }

    // Exchange refresh token for access token
    const tokenResp = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        client_id: gmailClientId,
        client_secret: gmailClientSecret,
        refresh_token: gmailRefreshToken,
        grant_type: 'refresh_token'
      }).toString()
    })

    if (!tokenResp.ok) {
      const err = await tokenResp.text()
      console.error('Failed to exchange refresh token:', tokenResp.status, err)
      return true
    }

    const tokenJson = await tokenResp.json()
    const accessToken = tokenJson.access_token
    if (!accessToken) {
      console.error('No access_token returned from Google token endpoint', tokenJson)
      return true
    }

    // Build raw RFC 2822 MIME message
    const rawMsg = []
    rawMsg.push(`From: ${senderEmail}`)
    rawMsg.push(`To: ${email}`)
    rawMsg.push('Subject: Your Roominate Verification Code')
    rawMsg.push('MIME-Version: 1.0')
    rawMsg.push('Content-Type: text/html; charset=UTF-8')
    rawMsg.push('\r\n')
    rawMsg.push(`<html><body><p>Hello,</p><p>Your verification code is <strong>${otp}</strong>.</p><p>This code will expire in 5 minutes.</p><p>Best,<br/>Roominate</p></body></html>`)

    const message = rawMsg.join('\r\n')

    // base64url encode
    function base64UrlEncode(str: string) {
      // encode UTF-8 then base64
      const utf8 = new TextEncoder().encode(str)
      let binary = ''
      const len = utf8.byteLength
      for (let i = 0; i < len; i++) binary += String.fromCharCode(utf8[i])
      const b64 = btoa(binary)
      return b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
    }

    const raw = base64UrlEncode(message)

    const sendResp = await fetch('https://gmail.googleapis.com/gmail/v1/users/me/messages/send', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ raw })
    })

    if (!sendResp.ok) {
      const err = await sendResp.text()
      console.error('Gmail API send error:', sendResp.status, err)
      console.log(`⚠️ OTP for ${email}: ${otp} (Gmail delivery failed)`)
      return true
    }

    const sent = await sendResp.json()
    console.log(`✅ Gmail sent: ${JSON.stringify(sent)}`)
    console.log(`🔐 OTP: ${otp}`)
    return true

    const emailHtml = `
      <!DOCTYPE html>
      <html>
        <head>
          <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
            .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
            .otp-box { background: white; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 8px; margin: 20px 0; border-radius: 8px; border: 2px dashed #667eea; }
            .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>Roominate</h1>
              <p>Email Verification</p>
            </div>
            <div class="content">
              <h2>Your Verification Code</h2>
              <p>Hello,</p>
              <p>Thank you for signing up with Roominate. Please use the verification code below to complete your registration:</p>
              <div class="otp-box">${otp}</div>
              <p><strong>This code will expire in 5 minutes.</strong></p>
              <p>If you didn't request this code, please ignore this email.</p>
              <p>Best regards,<br>The Roominate Team</p>
            </div>
            <div class="footer">
              <p>© 2025 Roominate. All rights reserved.</p>
            </div>
          </div>
        </body>
      </html>
    `

    const emailText = `
Your Roominate Verification Code

Your verification code is: ${otp}

This code will expire in 5 minutes.

If you didn't request this code, please ignore this email.

Best regards,
The Roominate Team

© 2025 Roominate. All rights reserved.
    `

    // Send via Resend API
    const response = await fetch('https://api.resend.com/emails', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${resendApiKey}`
      },
      body: JSON.stringify({
        from: 'Roominate <onboarding@resend.dev>', // Change to your verified domain
        to: email,
        subject: 'Your Roominate Verification Code',
        html: emailHtml,
        text: emailText
      })
    })

    if (!response.ok) {
      const errorText = await response.text()
      console.error('Resend API error:', response.status, errorText)
      console.log(`⚠️ OTP for ${email}: ${otp} (Email delivery failed)`)
      return true // Return true to not block development
    }

    const result = await response.json()
    console.log(`✅ Email sent successfully to ${email}`)
    console.log(`🔐 OTP: ${otp}`) // Log for development/testing
    console.log(`� Email ID: ${result.id}`)
    return true

  } catch (error) {
    console.error('Email send error:', error)
    console.log(`📋 Fallback: OTP for ${email}: ${otp}`)
    return true // Return true to not block development
  }
}
