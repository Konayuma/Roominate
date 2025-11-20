// Supabase Edge Function: complete-signup
// Creates a Supabase auth user using the service_role key (admin endpoint).
// Environment variables required:
// - SUPABASE_URL (https://xxx.supabase.co)
// - SUPABASE_SERVICE_ROLE_KEY (service role key)

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.0'

interface SignupRequest {
  email: string
  password: string
  first_name?: string
  last_name?: string
  role?: 'tenant' | 'owner' | 'admin'
  dob?: string
  phone?: string
}

interface UserProfile {
  id: string
  email: string
  first_name: string | null
  last_name: string | null
  role: string
  dob: string | null  // Column is named 'dob' in deployed schema
  phone: string | null
}

Deno.serve(async (req: Request) => {
  try {
    console.log(new Date().toISOString(), 'complete-signup handler start')

    if (req.method !== 'POST') {
      return new Response(
        JSON.stringify({ error: 'Method not allowed' }),
        { status: 405, headers: { 'Content-Type': 'application/json' } }
      )
    }

    const SUPABASE_URL = Deno.env.get('SUPABASE_URL')
    const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')

    if (!SUPABASE_URL || !SERVICE_ROLE) {
      return new Response(
        JSON.stringify({ error: 'Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    const body = await req.json() as SignupRequest
    const { email, password, first_name, last_name, role = 'tenant', dob, phone } = body

    if (!email || !password) {
      return new Response(
        JSON.stringify({ error: 'email and password are required' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    console.log(new Date().toISOString(), 'Creating user:', email)

    // Create admin client
    const supabaseAdmin = createClient(SUPABASE_URL, SERVICE_ROLE, {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    })

    // 1. Create the auth user
    console.log(new Date().toISOString(), 'Creating auth user...')
    const { data: authData, error: authError } = await supabaseAdmin.auth.admin.createUser({
      email: email,
      password: password,
      email_confirm: true, // Auto-confirm the email
      user_metadata: {
        first_name: first_name || '',
        last_name: last_name || '',
        role: role || 'tenant'
      }
    })

    if (authError) {
      console.error('Auth user creation failed:', authError)
      return new Response(
        JSON.stringify({ error: 'Failed to create user', detail: authError.message }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    if (!authData.user) {
      return new Response(
        JSON.stringify({ error: 'Failed to create user', detail: 'No user data returned' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    console.log(new Date().toISOString(), 'Auth user created:', authData.user.id)

    // 2. Create profile in public.profiles table
    // Note: Deployed database has 'profiles' table with: id, email, first_name, last_name, role, dob, phone
    console.log(new Date().toISOString(), 'Creating profile in public.profiles...')
    const { data: profileResult, error: profileError } = await supabaseAdmin
      .from('profiles')  // Table name: public.profiles (NOT users!)
      .insert({
        id: authData.user.id,  // Primary key references auth.users(id)
        email: email,
        first_name: first_name || null,
        last_name: last_name || null,
        role: role || 'tenant',
        dob: dob || null,  // Column is named 'dob' not 'date_of_birth'
        phone: phone || null
      })
      .select()

    if (profileError) {
      console.error('Profile creation failed:', profileError)
      // Try to clean up the auth user if profile creation failed
      try {
        await supabaseAdmin.auth.admin.deleteUser(authData.user.id)
        console.log('Cleaned up auth user after profile failure')
      } catch (cleanupErr) {
        console.error('Failed to clean up auth user:', cleanupErr)
      }
      return new Response(
        JSON.stringify({ error: 'Failed to create profile', detail: profileError.message }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    console.log(new Date().toISOString(), 'User created successfully:', authData.user.id)

    // 3. Sign in the user to get session tokens
    console.log(new Date().toISOString(), 'Signing in user to get session...')
    const { data: signInData, error: signInError } = await supabaseAdmin.auth.signInWithPassword({
      email: email,
      password: password
    })

    if (signInError) {
      console.warn('Sign-in after creation failed:', signInError.message)
      // User is created but sign-in failed - return without session
      return new Response(
        JSON.stringify({
          success: true,
          user: {
            id: authData.user.id,
            email: authData.user.email
          },
          profile: profileResult && profileResult.length > 0 ? profileResult[0] : null,
          session: null,
          warning: 'User created but auto sign-in failed'
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    }

    console.log(new Date().toISOString(), 'User signed in successfully')

    return new Response(
      JSON.stringify({
        success: true,
        user: {
          id: authData.user.id,
          email: authData.user.email
        },
        profile: profileResult && profileResult.length > 0 ? profileResult[0] : null,
        session: signInData.session ? {
          access_token: signInData.session.access_token,
          refresh_token: signInData.session.refresh_token,
          expires_at: signInData.session.expires_at
        } : null
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )

  } catch (err) {
    console.error('complete-signup error:', err)
    return new Response(
      JSON.stringify({ error: 'Server error', detail: String(err) }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
