// Supabase Edge Function: lenco-payment
// Handles server-to-server payment initiation with Lenco.
// Environment variables required:
// - SUPABASE_URL (https://xxx.supabase.co)
// - SUPABASE_ANON_KEY (anon key)
// - LENCO_SECRET_KEY (Lenco API secret key)

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.0'

interface PaymentRequestBody {
  booking_id: string;
  amount: number;
  currency: string;
  email: string;
  phone_number: string;
  first_name: string;
  last_name: string;
}

Deno.serve(async (req: Request) => {
  try {
    console.log(new Date().toISOString(), 'lenco-payment handler start');

    if (req.method !== 'POST') {
      return new Response(JSON.stringify({ error: 'Method not allowed' }), {
        status: 405,
        headers: { 'Content-Type': 'application/json' },
      });
    }

    const body = await req.json() as PaymentRequestBody;
    const {
      booking_id,
      amount,
      currency,
      email,
      phone_number,
      first_name,
      last_name,
    } = body;

    if (!booking_id || !amount || !currency || !email || !phone_number) {
      return new Response(
        JSON.stringify({ error: 'Missing required payment details' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      );
    }

    const LENCO_API_KEY = Deno.env.get('LENCO_API_KEY');
    if (!LENCO_API_KEY) {
      console.error('Missing LENCO_API_KEY environment variable');
      return new Response(
        JSON.stringify({ error: 'Payment processor not configured' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      );
    }

    // Auto-detect mobile operator from phone number prefix (Zambia)
    const detectOperator = (phone: string): string => {
      const cleanPhone = phone.replace(/\D/g, ''); // Remove non-digits
      // Zambia: Airtel starts with 096, 097, 076, 077; MTN starts with 095, 096
      if (cleanPhone.startsWith('260')) {
        // International format
        const localPart = cleanPhone.substring(3);
        if (localPart.startsWith('96') || localPart.startsWith('97') || localPart.startsWith('76') || localPart.startsWith('77')) {
          return 'airtel';
        } else if (localPart.startsWith('95') || localPart.startsWith('96')) {
          return 'mtn';
        }
      } else {
        // Local format
        if (cleanPhone.startsWith('096') || cleanPhone.startsWith('097') || cleanPhone.startsWith('076') || cleanPhone.startsWith('077')) {
          return 'airtel';
        } else if (cleanPhone.startsWith('095')) {
          return 'mtn';
        }
      }
      return 'airtel'; // Default to Airtel if unable to detect
    };

    const operator = detectOperator(phone_number);
    console.log(`Detected operator: ${operator} for phone: ${phone_number}`);

    // Generate a unique reference for this transaction (only alphanumeric, -, ., _ allowed)
    const reference = `RMN-${booking_id.replace(/[^a-zA-Z0-9]/g, '')}-${Date.now()}`;

    console.log(new Date().toISOString(), `Initiating Lenco mobile money payment for booking ${booking_id} with reference ${reference}`);

    // Lenco mobile money API payload structure
    const lencoPayload = {
      amount: amount,
      reference: reference,
      phone: phone_number,
      operator: operator,
      country: "zm",
      bearer: "merchant"
    };

    console.log('Lenco API Request:', JSON.stringify(lencoPayload, null, 2));
    console.log('Authorization Header:', `Bearer ${LENCO_API_KEY.substring(0, 20)}...`);

    const lencoResponse = await fetch('https://api.lenco.co/access/v2/collections/mobile-money', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${LENCO_API_KEY}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(lencoPayload),
    });

    const responseText = await lencoResponse.text();
    console.log('Lenco API Response Status:', lencoResponse.status);
    console.log('Lenco API Raw Response:', responseText);
    
    let lencoData;
    try {
      lencoData = JSON.parse(responseText);
    } catch (parseError) {
      console.error('Failed to parse Lenco response as JSON:', parseError);
      lencoData = { raw: responseText };
    }
    
    console.log('Lenco API Parsed Response:', JSON.stringify(lencoData, null, 2));

    if (!lencoResponse.ok) {
      console.error('Lenco API error:', lencoData);
      return new Response(
        JSON.stringify({ 
          error: 'Failed to initiate payment', 
          details: lencoData,
          status_code: lencoResponse.status,
          endpoint: 'https://api.lenco.co/access/v2/collections/mobile-money'
        }),
        { status: lencoResponse.status, headers: { 'Content-Type': 'application/json' } }
      );
    }

    // Update the booking with the payment reference
    const SUPABASE_URL = Deno.env.get('SUPABASE_URL');
    const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');

    if (SUPABASE_URL && SUPABASE_SERVICE_ROLE_KEY) {
        const supabaseAdmin = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);
        const { error: updateError } = await supabaseAdmin
            .from('bookings')
          .update({ payment_reference: reference, payment_status: 'pending' })
          .eq('id', booking_id);

        if (updateError) {
            console.error(`Failed to update booking ${booking_id} with reference:`, updateError);
            // Don't fail the whole request, but log the error
        } else {
            console.log(`Successfully updated booking ${booking_id} with payment reference ${reference}`);
        }
    }


    console.log(new Date().toISOString(), 'Lenco mobile money payment initiated successfully');

    // Check if payment requires offline authorization
    const paymentStatus = lencoData?.data?.status || 'unknown';
    const requiresAuthorization = paymentStatus === 'pay-offline';

    return new Response(JSON.stringify({ 
      success: true, 
      reference: reference, 
      data: lencoData,
      requires_authorization: requiresAuthorization,
      payment_status: paymentStatus,
      message: requiresAuthorization ? 'Please check your phone and authorize the payment' : 'Payment initiated'
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });

  } catch (err) {
    console.error('lenco-payment error:', err);
    return new Response(
      JSON.stringify({ error: 'Server error', detail: String(err) }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    );
  }
});
