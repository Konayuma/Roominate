// Supabase Edge Function (TypeScript): confirm-user
// Confirms an auth user (sets email_confirm = true) and upserts a profile row in public.users
// Environment variables required:
// - SUPABASE_URL
// - SUPABASE_SERVICE_ROLE_KEY
// - CONFIRM_SECRET (shared secret required in header x-confirm-secret)

export default async function (req: any, res: any) {
  try {
    console.log('confirm-user invoked', { method: req?.method });
    if (req.method !== 'POST') {
      console.log('confirm-user wrong method', { method: req.method });
      return res.status(405).json({ error: 'Method not allowed' });
    }

  const SUPABASE_URL = Deno.env.get('SUPABASE_URL') || '';
  const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
  const CONFIRM_SECRET = Deno.env.get('CONFIRM_SECRET') || '';
  console.log('env loaded', { hasUrl: !!SUPABASE_URL, hasServiceRole: !!SERVICE_ROLE, hasConfirmSecret: !!CONFIRM_SECRET });

    const providedSecret = (req.headers.get('x-confirm-secret') || req.headers.get('x_confirm_secret') || '') as string;
    console.log('provided secret present', { provided: providedSecret ? true : false });
    if (!CONFIRM_SECRET || providedSecret !== CONFIRM_SECRET) {
      console.log('confirm-user unauthorized', { providedSecretPresent: !!providedSecret });
      return res.status(401).json({ error: 'Unauthorized - missing or invalid confirm secret' });
    }

    if (!SUPABASE_URL || !SERVICE_ROLE) {
      return res.status(500).json({ error: 'Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY' });
    }

    // parse body (guarded) and log progress so we can see where the function might hang
    console.log('about to parse body');
    let body: Record<string, any> = {};
    try {
      // give body parsing a short wall-clock timeout
      const bodyPromise = req.json();
      const timeout = new Promise((_, reject) => setTimeout(() => reject(new Error('body parse timeout')), 5000));
      body = await Promise.race([bodyPromise, timeout]) as Record<string, any>;
    } catch (e) {
      console.error('error parsing body', String(e));
      return res.status(400).json({ error: 'Invalid or missing JSON body', detail: String(e) });
    }
    console.log('body parsed', { keys: Object.keys(body || {}) });
    const { user_id, email, first_name, last_name, role, dob, phone } = body as Record<string, any>;

    if (!user_id && !email) {
      return res.status(400).json({ error: 'user_id or email is required' });
    }

    const fetchWithTimeout = async (url: string, opts: RequestInit = {}, ms = 10000) => {
      const controller = new AbortController();
      const signal = controller.signal;
      const fetchPromise = fetch(url, { signal, ...opts });
      const timeoutPromise = new Promise<Response>((_, reject) =>
        setTimeout(() => {
          try { controller.abort(); } catch (e) { /* ignore */ }
          reject(new Error('Timeout'));
        }, ms)
      );
      return Promise.race([fetchPromise, timeoutPromise]) as Promise<Response>;
    };

    let uid = user_id as string | undefined;
    if (!uid) {
      const q = `${SUPABASE_URL}/auth/v1/admin/users?email=eq.${encodeURIComponent(email)}`;
      console.log('fetching user by email', { url: q });
      const resp = await fetchWithTimeout(q, {
        headers: {
          'apikey': SERVICE_ROLE,
          'Authorization': `Bearer ${SERVICE_ROLE}`,
          'Accept': 'application/json'
        }
      }, 8000);
      console.log('admin users fetch completed', { ok: resp.ok, status: resp.status });
      const txt = await resp.text();
      let arr: any[] = [];
      try { arr = JSON.parse(txt); } catch (e) { console.log('admin users parse error', String(e)); }
      if (!resp.ok || !Array.isArray(arr) || arr.length === 0) {
        console.log('user not found by email', { respOk: resp.ok, body: txt });
        return res.status(404).json({ error: 'User not found by email', detail: txt });
      }
      uid = arr[0].id;
      console.log('resolved uid by email', { uid });
    }

    // Patch auth user to set email_confirm = true
    try {
      const patchUrl = `${SUPABASE_URL}/auth/v1/admin/users/${uid}`;
      console.log('patching auth user', { patchUrl });
      const patchResp = await fetchWithTimeout(patchUrl, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'apikey': SERVICE_ROLE,
          'Authorization': `Bearer ${SERVICE_ROLE}`
        },
        body: JSON.stringify({ email_confirm: true })
      }, 8000);

      console.log('patch request sent');
      const patchText = await patchResp.text();
      let patchJson: any = null;
      try { patchJson = JSON.parse(patchText); } catch (e) { patchJson = { raw: patchText }; }
      if (!patchResp.ok) {
        console.log('patch failed', { status: patchResp.status, body: patchJson });
        return res.status(patchResp.status).json({ error: 'Failed to patch auth user', detail: patchJson });
      }
      console.log('patch succeeded', { status: patchResp.status });
    } catch (err) {
      return res.status(502).json({ error: 'Error patching auth user', detail: String(err) });
    }

    // Upsert profile row
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
      console.log('upserting profile', { profileUrl, profile: { id: uid, email, first_name, last_name } });
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

      console.log('profile request completed', { ok: profileResp.ok, status: profileResp.status });
      const profileText = await profileResp.text();
      let profileJson: any = null;
      try { profileJson = JSON.parse(profileText); } catch (e) { profileJson = { raw: profileText }; }
      if (!profileResp.ok) {
        console.log('profile upsert failed', { status: profileResp.status, body: profileJson });
        return res.status(profileResp.status).json({ error: 'Failed to upsert profile', detail: profileJson });
      }

      console.log('profile upsert succeeded');
      return res.status(200).json({ success: true, user_id: uid, profile: profileJson });
    } catch (err) {
      return res.status(502).json({ error: 'Error upserting profile', detail: String(err) });
    }

  } catch (err) {
    console.error('confirm-user error', err);
    return res.status(500).json({ error: 'Server error', detail: String(err) });
  }
}
