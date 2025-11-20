import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.0'

// Auto-detect mobile operator from phone number (Zambia)
const detectOperator = (phone: string): string => {
  const cleanPhone = phone.replace(/\D/g, '');
  if (cleanPhone.startsWith('260')) {
    const localPart = cleanPhone.substring(3);
    if (localPart.startsWith('96') || localPart.startsWith('97') || localPart.startsWith('76') || localPart.startsWith('77')) {
      return 'airtel';
    } else if (localPart.startsWith('95')) {
      return 'mtn';
    }
  } else {
    if (cleanPhone.startsWith('096') || cleanPhone.startsWith('097') || cleanPhone.startsWith('076') || cleanPhone.startsWith('077')) {
      return 'airtel';
    } else if (cleanPhone.startsWith('095')) {
      return 'mtn';
    }
  }
  return 'airtel';
};

Deno.serve(async (req: Request) => {
  // Add CORS headers for webhook
  const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  };

  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    if (req.method !== 'POST') {
      return new Response(JSON.stringify({ error: 'Method not allowed' }), { 
        status: 405, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
      });
    }

    // Skip JWT verification - allow external webhook calls
    // Lenco webhooks don't have Supabase auth tokens
    
    const SUPABASE_URL = Deno.env.get('SUPABASE_URL');
    const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');
    const LENCO_API_KEY = Deno.env.get('LENCO_API_KEY');

    console.log('Environment check:', {
      hasSupabaseUrl: !!SUPABASE_URL,
      hasServiceRole: !!SERVICE_ROLE,
      hasLencoKey: !!LENCO_API_KEY
    });

    const body = await req.json();
    console.log(new Date().toISOString(), 'lenco-webhook received:', JSON.stringify(body));

    // Determine event and reference - be flexible
    const eventType = body.event || body.type || (body.data && body.data.type) || null;
    let reference = null;

    if (body.reference) reference = body.reference;
    if (!reference && body.data && body.data.reference) reference = body.data.reference;
    if (!reference && body.collection && body.collection.reference) reference = body.collection.reference;

    if (!reference) {
      console.warn('Webhook: no reference found in payload');
      return new Response(JSON.stringify({ error: 'No reference found' }), { 
        status: 400, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
      });
    }

    let newStatus = null;
    if (eventType === 'collection.successful' || eventType === 'collection.created' || eventType === 'collection.updated') {
      // If payload indicates success state we'll map to completed, else ignore
      // Try to find a success flag
      const successFlag = (body.data && body.data.success) || (body.collection && body.collection.status === 'successful') || (body.data && body.data.status === 'successful');
      if (successFlag === true || body.collection?.status === 'successful' || body.data?.status === 'successful') {
        newStatus = 'completed';
      }
    } else if (eventType === 'collection.failed') {
      newStatus = 'failed';
    } else if (eventType === 'collection.cancelled') {
      newStatus = 'cancelled';
    }

    // Fallback: infer from collection.status or data.status
    if (!newStatus) {
      const statusField = body.collection?.status || body.data?.status || null;
      if (statusField === 'successful') newStatus = 'completed';
      if (statusField === 'failed') newStatus = 'failed';
      if (statusField === 'cancelled') newStatus = 'cancelled';
    }

    // If not determined, do nothing but OK
    if (!newStatus) {
      console.log('Webhook: could not determine status for reference', reference, 'eventType', eventType);
      return new Response(JSON.stringify({ success: true, message: 'No action' }), { 
        status: 200, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
      });
    }

    if (!SUPABASE_URL || !SERVICE_ROLE) {
      console.error('Supabase not configured');
      return new Response(JSON.stringify({ error: 'Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY' }), { 
        status: 500, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
      });
    }

    const supabaseAdmin = createClient(SUPABASE_URL, SERVICE_ROLE, {
      auth: { autoRefreshToken: false, persistSession: false }
    });

    const updatePayload: any = { 
      payment_status: newStatus,
      payment_date: new Date().toISOString()
    };
    if (newStatus === 'completed') {
      updatePayload.status = 'confirmed';
      updatePayload.payment_amount = body.data?.amount || body.collection?.amount || null;
    }

    const { error } = await supabaseAdmin.from('bookings').update(updatePayload).eq('payment_reference', reference);

    if (error) {
      console.error('Failed to update booking status', error);
      return new Response(JSON.stringify({ error: 'Failed to update booking', detail: error.message }), { 
        status: 500, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
      });
    }

    console.log('Booking updated for reference', reference, 'newStatus', newStatus);

    // If payment is successful, initiate transfer to owner
    if (newStatus === 'completed' && LENCO_API_KEY) {
      try {
        console.log('Payment successful, initiating transfer to owner...');

        // Fetch booking with owner details
        const { data: booking, error: bookingError } = await supabaseAdmin
          .from('bookings')
          .select(`
            id,
            total_amount,
            payment_amount,
            transfer_status,
            owner_id,
            listing_id,
            boarding_houses!inner(
              title,
              owner_id
            )
          `)
          .eq('payment_reference', reference)
          .single();

        if (bookingError || !booking) {
          console.error('Failed to fetch booking for transfer:', bookingError);
          return new Response(JSON.stringify({ success: true, message: 'Payment updated but transfer skipped' }), { 
            status: 200, 
            headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
          });
        }

        // Skip if transfer already processed
        if (booking.transfer_status === 'successful' || booking.transfer_status === 'pending') {
          console.log('Transfer already processed, skipping');
          return new Response(JSON.stringify({ success: true, message: 'Transfer already processed' }), { 
            status: 200, 
            headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
          });
        }

        const ownerId = booking.owner_id || (booking.boarding_houses as any)?.owner_id;
        if (!ownerId) {
          console.error('No owner_id found for booking');
          return new Response(JSON.stringify({ success: true, message: 'Payment updated but no owner found' }), { 
            status: 200, 
            headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
          });
        }

        // Fetch owner details
        const { data: owner, error: ownerError } = await supabaseAdmin
          .from('users')
          .select('id, first_name, last_name, phone, email')
          .eq('id', ownerId)
          .single();

        if (ownerError || !owner || !owner.phone) {
          console.error('Owner not found or missing phone:', ownerError);
          return new Response(JSON.stringify({ success: true, message: 'Payment updated but owner details incomplete' }), { 
            status: 200, 
            headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
          });
        }

        console.log(`Owner found: ${owner.first_name} ${owner.last_name}, Phone: ${owner.phone}`);

        // Calculate transfer amount (apply platform fee)
        const paymentAmount = booking.payment_amount || booking.total_amount || 0;
        const platformFeePercent = 10; // 10% platform fee
        const platformFee = (paymentAmount * platformFeePercent) / 100;
        const transferAmount = paymentAmount - platformFee;

        console.log(`Transfer calculation: Payment=${paymentAmount}, Fee=${platformFee} (${platformFeePercent}%), Transfer=${transferAmount}`);

        if (transferAmount <= 0) {
          console.error('Invalid transfer amount:', transferAmount);
          return new Response(JSON.stringify({ success: true, message: 'Payment updated but invalid transfer amount' }), { 
            status: 200, 
            headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
          });
        }

        // Generate unique transfer reference
        const transferReference = `TRF-${booking.id.substring(0, 8)}-${Date.now()}`;
        const operator = detectOperator(owner.phone);

        const transferPayload = {
          amount: transferAmount,
          reference: transferReference,
          phone: owner.phone,
          operator: operator,
          country: "zm",
          narration: `Booking payment - ${(booking.boarding_houses as any)?.title || booking.id.substring(0, 8)}`
        };

        console.log('Initiating Lenco transfer:', JSON.stringify(transferPayload, null, 2));

        const transferResponse = await fetch('https://api.lenco.co/access/v2/transfers/mobile-money', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${LENCO_API_KEY}`,
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
          body: JSON.stringify(transferPayload),
        });

        const transferResponseText = await transferResponse.text();
        console.log('Transfer response status:', transferResponse.status);
        console.log('Transfer response:', transferResponseText);

        let transferData;
        try {
          transferData = JSON.parse(transferResponseText);
        } catch (e) {
          transferData = { raw: transferResponseText };
        }

        if (!transferResponse.ok) {
          console.error('Transfer failed:', transferData);
          
          // Update booking with transfer failure
          await supabaseAdmin
            .from('bookings')
            .update({
              transfer_status: 'failed',
              transfer_reference: transferReference
            })
            .eq('id', booking.id);

          return new Response(JSON.stringify({ 
            success: true, 
            message: 'Payment confirmed but transfer failed',
            transfer_error: transferData
          }), { 
            status: 200, 
            headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
          });
        }

        // Update booking with transfer details
        await supabaseAdmin
          .from('bookings')
          .update({
            transfer_reference: transferReference,
            transfer_status: 'pending',
            transfer_amount: transferAmount,
            transfer_date: new Date().toISOString()
          })
          .eq('id', booking.id);

        console.log('Transfer initiated successfully:', transferReference);

        // Send notification to owner
        await supabaseAdmin
          .from('notifications')
          .insert({
            user_id: ownerId,
            title: 'Payment Received',
            body: `You received K${transferAmount.toFixed(2)} for booking ${booking.id.substring(0, 8)}`,
            data: {
              type: 'payment',
              booking_id: booking.id,
              amount: transferAmount,
              reference: transferReference
            }
          });

        return new Response(JSON.stringify({ 
          success: true,
          message: 'Payment confirmed and transfer initiated',
          transfer_reference: transferReference,
          transfer_amount: transferAmount
        }), { 
          status: 200, 
          headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
        });

      } catch (transferErr) {
        console.error('Transfer error:', transferErr);
        return new Response(JSON.stringify({ 
          success: true, 
          message: 'Payment updated but transfer failed',
          error: String(transferErr)
        }), { 
          status: 200, 
          headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
        });
      }
    }

    return new Response(JSON.stringify({ success: true }), { 
      status: 200, 
      headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
    });

  } catch (err) {
    console.error('lenco-webhook error:', err);
    return new Response(JSON.stringify({ error: 'Server error', detail: String(err) }), { 
      status: 500, 
      headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
    });
  }
});
