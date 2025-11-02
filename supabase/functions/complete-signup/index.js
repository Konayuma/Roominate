// Supabase Edge Function: complete-signup
// Verifies that an OTP was previously marked used for the email and then creates
// a Supabase auth user using the service_role key (admin endpoint).
// Environment variables required:
// - SUPABASE_URL (https://xxx.supabase.co)
// - SUPABASE_SERVICE_ROLE_KEY (service role key)

export default async function (req, res) {
  // Simplified flow: validate OTP, mark it used, and insert a pending signup row
  // into `pending_signups`. The Android app can then complete signup using the
  // normal client-side auth (supabase auth signUp) since the email was verified.
  try {
    console.log(new Date().toISOString(), 'complete-signup handler start');

    if (req.method !== 'POST') {
      return res.status(405).json({ error: 'Method not allowed' });
    }

    const SUPABASE_URL = process.env.SUPABASE_URL;
    const SERVICE_ROLE = process.env.SUPABASE_SERVICE_ROLE_KEY;

    if (!SUPABASE_URL || !SERVICE_ROLE) {
      return res.status(500).json({ error: 'Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY' });
    }

    const body = await req.json();
    const { email, otp, first_name, last_name, role = 'tenant', dob, phone } = body;

    if (!email) {
      return res.status(400).json({ error: 'email is required' });
    }

    const providedOtp = typeof otp === 'string' && otp.trim().length > 0 ? otp.trim() : null;

    // lightweight fetch with timeout helper
    const fetchWithTimeout = async (url, opts = {}, ms = 10000) => {
      const controller = new AbortController();
      const signal = controller.signal;
      const fetchPromise = fetch(url, { signal, ...opts });
      const timeoutPromise = new Promise((_, reject) => {
        const t = setTimeout(() => {
          try { controller.abort(); } catch (e) { }
          reject(new Error('Timeout'));
        }, ms);
        fetchPromise.finally(() => clearTimeout(t)).catch(() => clearTimeout(t));
      });
      return Promise.race([fetchPromise, timeoutPromise]);
    };

    // 1) Fetch latest OTP row
    console.log(new Date().toISOString(), 'Fetching latest OTP for', email);
    let otpResp;
    try {
      otpResp = await fetchWithTimeout(
        `${SUPABASE_URL}/rest/v1/email_otps?email=eq.${encodeURIComponent(email)}&select=*,created_at&order=created_at.desc&limit=1`,
        {
          headers: {
            apikey: SERVICE_ROLE,
            Authorization: `Bearer ${SERVICE_ROLE}`,
            Accept: 'application/json'
          }
        },
        10000
      );
    } catch (err) {
      console.error('OTP fetch error', err);
      return res.status(502).json({ error: 'Failed to fetch OTP record', detail: String(err) });
    }

    const otpText = await otpResp.text();
    let otpRows = [];
    try { otpRows = JSON.parse(otpText); } catch (e) { console.warn('OTP parse failed', e); }

    if (!otpResp.ok || !Array.isArray(otpRows) || otpRows.length === 0) {
      return res.status(400).json({ error: 'No OTP record found for this email. Verify OTP first.' });
    }

    const otpRow = otpRows[0];

    // Validate OTP (if provided) or require used=true
    const createdAt = otpRow.created_at ? new Date(otpRow.created_at) : null;
    const now = new Date();
    const ageMs = createdAt ? (now - createdAt) : Number.MAX_SAFE_INTEGER;
    const oneHourMs = 1000 * 60 * 60;

    if (providedOtp) {
      const salt = process.env.OTP_SALT || '';
      const encoder = new TextEncoder();
      const data = encoder.encode(providedOtp + salt);
      const hashBuffer = await crypto.subtle.digest('SHA-256', data);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      const providedHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

      if (new Date(otpRow.expires_at) < new Date()) {
        return res.status(400).json({ error: 'Verification code has expired' });
      }
      if ((otpRow.attempts || 0) >= 5) {
        return res.status(400).json({ error: 'Too many failed attempts' });
      }

      if (otpRow.otp_hash !== providedHash) {
        // best-effort increment attempts
        try {
          await fetchWithTimeout(`${SUPABASE_URL}/rest/v1/email_otps?id=eq.${otpRow.id}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json', apikey: SERVICE_ROLE, Authorization: `Bearer ${SERVICE_ROLE}` },
            body: JSON.stringify({ attempts: (otpRow.attempts || 0) + 1 })
          }, 5000);
        } catch (e) { console.warn('Failed to increment attempts', e); }
        return res.status(400).json({ error: 'Invalid verification code' });
      }
    } else {
      const used = otpRow.used === true || otpRow.used === 'true';
      if (!used || ageMs > oneHourMs) {
        return res.status(400).json({ error: 'OTP not verified recently. Verify with OTP first.' });
      }
    }

    // Mark OTP used (best-effort)
    try {
      await fetchWithTimeout(`${SUPABASE_URL}/rest/v1/email_otps?id=eq.${otpRow.id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', apikey: SERVICE_ROLE, Authorization: `Bearer ${SERVICE_ROLE}` },
        body: JSON.stringify({ used: true, used_at: new Date().toISOString() })
      }, 5000);
    } catch (e) { console.warn('Failed to mark OTP used', e); }

    // Insert into pending_signups so the client can complete signup via client-side auth
    const pending = {
      email: otpRow.email || email,
      first_name: first_name || null,
      last_name: last_name || null,
      role: role || 'tenant',
      dob: dob || null,
      phone: phone || null,
      otp_id: otpRow.id,
      created_at: new Date().toISOString()
    };

    console.log(new Date().toISOString(), 'Inserting pending_signups for', pending.email);
    let insertResp;
    try {
      insertResp = await fetchWithTimeout(`${SUPABASE_URL}/rest/v1/pending_signups`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          apikey: SERVICE_ROLE,
          Authorization: `Bearer ${SERVICE_ROLE}`,
          Prefer: 'return=representation'
        },
        body: JSON.stringify(pending)
      }, 8000);
    } catch (e) {
      console.error('Failed to insert pending_signups', e);
      return res.status(502).json({ error: 'Failed to save pending signup', detail: String(e) });
    }

    const insertText = await insertResp.text();
    let insertJson = null;
    try { insertJson = JSON.parse(insertText); } catch (e) { insertJson = { raw: insertText }; }

    if (!insertResp.ok) {
      console.error('pending_signups insert non-OK', insertResp.status, insertJson);
      return res.status(500).json({ error: 'Failed to create pending signup. Ensure table `pending_signups` exists and service role has access.', detail: insertJson });
    }

    return res.status(200).json({ success: true, pending: insertJson });
  } catch (err) {
    console.error('complete-signup simplified error', err);
    return res.status(500).json({ error: 'Server error', detail: String(err) });
  }
}
