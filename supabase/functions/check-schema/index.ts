import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.0'

Deno.serve(async (req: Request) => {
  try {
    const SUPABASE_URL = Deno.env.get('SUPABASE_URL')
    const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')

    if (!SUPABASE_URL || !SERVICE_ROLE) {
      return new Response(
        JSON.stringify({ error: 'Missing environment variables' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    const supabaseAdmin = createClient(SUPABASE_URL, SERVICE_ROLE, {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    })

    // Query the information_schema to get actual column names
    const { data, error } = await supabaseAdmin
      .from('users')
      .select('*')
      .limit(1)

    if (error) {
      return new Response(
        JSON.stringify({ 
          error: 'Query failed', 
          detail: error.message,
          hint: 'Table might not exist or have different name'
        }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Get column names from the result
    const columns = data && data.length > 0 ? Object.keys(data[0]) : []

    return new Response(
      JSON.stringify({
        success: true,
        table: 'users',
        columns: columns,
        sample_data: data
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )

  } catch (err) {
    return new Response(
      JSON.stringify({ error: 'Server error', detail: String(err) }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
