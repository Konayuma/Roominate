// Supabase Edge Function: confirm-user
// Confirms an auth user (sets email_confirm = true) and upserts a profile row in public.users
// Requires env:
// - SUPABASE_URL
// - SUPABASE_SERVICE_ROLE_KEY
// - CONFIRM_SECRET (shared secret required in header x-confirm-secret)

export default async function (req, res) {
  try {
    if (req.method !== 'POST') {
      return res.status(405).json({ error: 'Method not allowed' });
    }

    const SUPABASE_URL = process.env.SUPABASE_URL;
    const SERVICE_ROLE = process.env.SUPABASE_SERVICE_ROLE_KEY;
    const CONFIRM_SECRET = process.env.CONFIRM_SECRET || '';

    // simple secret to avoid public abuse
    const providedSecret = req.headers['x-confirm-secret'] || req.headers['x_confirm_secret'] || '';
    if (!CONFIRM_SECRET || providedSecret !== CONFIRM_SECRET) {
      return res.status(401).json({ error: 'Unauthorized - missing or invalid confirm secret' });
    }

    if (!SUPABASE_URL || !SERVICE_ROLE) {
      return res.status(500).json({ error: 'Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY' });
    }

    const body = await req.json();
    // Accept either user_id or email (prefer user_id)
    const { user_id, email } = body;

    if (!user_id && !email) {
      return res.status(400).json({ error: 'user_id or email is required' });
    }

    // helper fetch with timeout
    const fetchWithTimeout = async (url, opts = {}, ms = 10000) => {
      const controller = new AbortController();
      const signal = controller.signal;
      const fetchP = fetch(url, { signal, ...opts });
      const timeoutP = new Promise((_, reject) => setTimeout(() => { try { controller.abort(); } catch(e){}; reject(new Error('Timeout')); }, ms));
      return Promise.race([fetchP, timeoutP]);
    };

    // resolve user id if only email provided
    let uid = user_id;
    if (!uid) {
      // fetch user by email via admin users list
      const q = `${SUPABASE_URL}/auth/v1/admin/users?email=eq.${encodeURIComponent(email)}`;
      const resp = await fetchWithTimeout(q, {
        headers: {
          'apikey': SERVICE_ROLE,
          'Authorization': `Bearer ${SERVICE_ROLE}`,
          'Accept': 'application/json'
        }
      }, 8000);
      const txt = await resp.text();
      let arr = [];
      try { arr = JSON.parse(txt); } catch(e) { /* ignore */ }
      if (!resp.ok || !Array.isArray(arr) || arr.length === 0) {
        return res.status(404).json({ error: 'User not found by email', detail: txt });
      }
      uid = arr[0].id;
    }

    // Fetch user details to get user_metadata
    let userDetails = null;
    try {
      const userUrl = `${SUPABASE_URL}/auth/v1/admin/users/${uid}`;
      const userResp = await fetchWithTimeout(userUrl, {
        headers: {
          'apikey': SERVICE_ROLE,
          'Authorization': `Bearer ${SERVICE_ROLE}`
        }
      }, 8000);
      const userText = await userResp.text();
      if (userResp.ok) {
        userDetails = JSON.parse(userText);
      } else {
        console.warn('Failed to fetch user details', userResp.status, userText);
      }
    } catch (err) {
      console.warn('Error fetching user details', err);
    }

    // Extract data from user_metadata
    const userMetadata = userDetails?.user_metadata || {};
    const first_name = userMetadata.first_name || null;
    const last_name = userMetadata.last_name || null;
    const role = userMetadata.role || null;
    const dob = userMetadata.dob || null;
    const phone = userMetadata.phone || null;

    // 1) Patch auth user to set email_confirm = true
    try {
      const patchUrl = `${SUPABASE_URL}/auth/v1/admin/users/${uid}`;
      const patchResp = await fetchWithTimeout(patchUrl, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'apikey': SERVICE_ROLE,
          'Authorization': `Bearer ${SERVICE_ROLE}`
        },
        body: JSON.stringify({ email_confirm: true })
      }, 8000);

      const patchText = await patchResp.text();
      let patchJson = null;
      try { patchJson = JSON.parse(patchText); } catch(e) { patchJson = { raw: patchText }; }
      if (!patchResp.ok) {
        return res.status(patchResp.status).json({ error: 'Failed to patch auth user', detail: patchJson });
      }
    } catch (err) {
      return res.status(502).json({ error: 'Error patching auth user', detail: String(err) });
    }

    // 2) Upsert profile row in public.users using service role (bypass RLS)
    try {
      const profile = {
        id: uid,
        email: email || null,
        first_name: first_name || null,
        last_name: last_name || null,
        role: role || null,
        dob: dob || null,
        phone: phone || null
      };

      const profileUrl = `${SUPABASE_URL}/rest/v1/users`;
      const profileResp = await fetchWithTimeout(profileUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'apikey': SERVICE_ROLE,
          'Authorization': `Bearer ${SERVICE_ROLE}`,
          'Prefer': 'resolution=merge-duplicates,return=representation'
        },
        body: JSON.stringify(profile)
      }, 8000);

      const profileText = await profileResp.text();
      let profileJson = null;
      try { profileJson = JSON.parse(profileText); } catch(e) { profileJson = { raw: profileText }; }
      if (!profileResp.ok) {
        return res.status(profileResp.status).json({ error: 'Failed to upsert profile', detail: profileJson });
      }

      return res.status(200).json({ success: true, user_id: uid, profile: profileJson });
    } catch (err) {
      return res.status(502).json({ error: 'Error upserting profile', detail: String(err) });
    }

  } catch (err) {
    console.error('confirm-user error', err);
    return res.status(500).json({ error: 'Server error', detail: String(err) });
  }
}
