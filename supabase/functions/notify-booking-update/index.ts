// Supabase Edge Function: notify-booking-update
// Deploy this to: supabase/functions/notify-booking-update/index.ts

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { booking_id, status, owner_id, tenant_id } = await req.json()

    console.log('Received notification request:', { booking_id, status, owner_id, tenant_id })

    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    
    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    const notifications = []

    // Create notification for tenant
    if (tenant_id) {
      let title = ''
      let message = ''
      
      switch (status) {
        case 'confirmed':
          title = 'Booking Confirmed ✅'
          message = 'Great news! Your booking has been confirmed by the owner.'
          break
        case 'completed':
          title = 'Booking Completed'
          message = 'Your booking has been marked as completed. We hope you enjoyed your stay!'
          break
        case 'cancelled':
          title = 'Booking Cancelled'
          message = 'Your booking has been cancelled.'
          break
        case 'rejected':
          title = 'Booking Request Declined'
          message = 'Unfortunately, the owner has declined your booking request.'
          break
        default:
          title = 'Booking Updated'
          message = `Your booking status is now: ${status}`
      }

      notifications.push({
        user_id: tenant_id,
        title,
        message,
        type: 'booking_update',
        related_id: booking_id,
        is_read: false
      })
    }

    // Create notification for owner if booking is created
    if (owner_id && status === 'pending') {
      notifications.push({
        user_id: owner_id,
        title: 'New Booking Request 📋',
        message: 'You have received a new booking request. Please review and respond.',
        type: 'booking_update',
        related_id: booking_id,
        is_read: false
      })
    }

    // Insert all notifications
    if (notifications.length > 0) {
      const { data, error } = await supabase
        .from('notifications')
        .insert(notifications)

      if (error) {
        console.error('Error inserting notifications:', error)
        throw error
      }

      console.log('Successfully created notifications:', data)
    }

    return new Response(
      JSON.stringify({ 
        success: true, 
        notifications_created: notifications.length 
      }), 
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200
      }
    )

  } catch (error) {
    console.error('Error in notify-booking-update:', error)
    return new Response(
      JSON.stringify({ error: error.message }), 
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500
      }
    )
  }
})
