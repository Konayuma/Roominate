-- Supabase schema for Roominate
-- Paste into Supabase SQL editor

-- NOTE: Supabase provides auth.users table. We create a separate public.users profile table that references auth.users(id).
-- Adjust types/constraints to your needs. This file also includes index recommendations and commented RLS policy examples.

-- Extension needed for gen_random_uuid
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) users (profile) - extended profile information
CREATE TABLE IF NOT EXISTS public.users (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  role text NOT NULL DEFAULT 'tenant', -- 'tenant' | 'owner' | 'admin'
  first_name text,
  last_name text,
  display_name text,
  date_of_birth date,
  phone text,
  avatar_url text,
  bio text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_users_role ON public.users (role);

-- 2) email_otps - one-time verification codes (store hashed values only)
CREATE TABLE IF NOT EXISTS public.email_otps (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text NOT NULL,
  otp_hash text NOT NULL,
  expires_at timestamptz NOT NULL,
  used boolean NOT NULL DEFAULT false,
  ip_address text,
  attempts int NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_email_otps_email_created ON public.email_otps (email, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_email_otps_expires ON public.email_otps (expires_at);

-- 3) boarding_houses (listings)
CREATE TABLE IF NOT EXISTS public.boarding_houses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  title text NOT NULL,
  description text,
  address text,
  latitude double precision,
  longitude double precision,
  price_per_month numeric(10,2),
  available boolean DEFAULT true,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_boarding_houses_owner ON public.boarding_houses (owner_id);
CREATE INDEX IF NOT EXISTS idx_boarding_houses_location ON public.boarding_houses (latitude, longitude);

-- 4) properties_media (images / files attached to listings)
CREATE TABLE IF NOT EXISTS public.properties_media (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id uuid NOT NULL REFERENCES public.boarding_houses(id) ON DELETE CASCADE,
  url text NOT NULL,
  filename text,
  mime_type text,
  ordering int DEFAULT 0,
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_media_listing ON public.properties_media (listing_id);

-- 5) bookings
CREATE TABLE IF NOT EXISTS public.bookings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id uuid NOT NULL REFERENCES public.boarding_houses(id) ON DELETE CASCADE,
  tenant_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  start_date date NOT NULL,
  end_date date,
  total_amount numeric(10,2),
  status text NOT NULL DEFAULT 'pending', -- pending | confirmed | cancelled | completed
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_bookings_listing ON public.bookings (listing_id);
CREATE INDEX IF NOT EXISTS idx_bookings_tenant ON public.bookings (tenant_id);

-- 6) reviews
CREATE TABLE IF NOT EXISTS public.reviews (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id uuid NOT NULL REFERENCES public.boarding_houses(id) ON DELETE CASCADE,
  reviewer_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  rating smallint NOT NULL CHECK (rating >= 1 AND rating <= 5),
  comment text,
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reviews_listing ON public.reviews (listing_id);

-- 7) favorites
CREATE TABLE IF NOT EXISTS public.favorites (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  listing_id uuid NOT NULL REFERENCES public.boarding_houses(id) ON DELETE CASCADE,
  created_at timestamptz DEFAULT now(),
  UNIQUE (user_id, listing_id)
);

CREATE INDEX IF NOT EXISTS idx_favorites_user ON public.favorites (user_id);

-- 8) inquiries
CREATE TABLE IF NOT EXISTS public.inquiries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id uuid NOT NULL REFERENCES public.boarding_houses(id) ON DELETE CASCADE,
  user_id uuid REFERENCES public.users(id), -- nullable for guests
  owner_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  message text NOT NULL,
  email text,
  phone text,
  status text DEFAULT 'open', -- open | answered | closed
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_inquiries_listing ON public.inquiries (listing_id);

-- 9) notifications
CREATE TABLE IF NOT EXISTS public.notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  title text,
  body text,
  data jsonb,
  read boolean DEFAULT false,
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON public.notifications (user_id);

-- 10) audit_logs (optional)
CREATE TABLE IF NOT EXISTS public.audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id uuid REFERENCES public.users(id),
  action text NOT NULL,
  resource text,
  metadata jsonb,
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_actor ON public.audit_logs (actor_id);

-- ========== ROW LEVEL SECURITY POLICIES ==========

-- 1) users table - allow users to manage their own profile
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Insert own profile" ON public.users 
  FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "Select own profile" ON public.users 
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Update own profile" ON public.users 
  FOR UPDATE USING (auth.uid() = id);

-- 2) boarding_houses - owners manage their listings, public can read available ones
ALTER TABLE public.boarding_houses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Owners can manage listings" ON public.boarding_houses
  FOR ALL USING (auth.uid() = owner_id);

CREATE POLICY "Public read available listings" ON public.boarding_houses
  FOR SELECT USING (available = true);

-- 3) properties_media - owners can manage media for their listings
ALTER TABLE public.properties_media ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Owners manage listing media" ON public.properties_media
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM public.boarding_houses 
      WHERE boarding_houses.id = properties_media.listing_id 
      AND boarding_houses.owner_id = auth.uid()
    )
  );

CREATE POLICY "Public read listing media" ON public.properties_media
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.boarding_houses 
      WHERE boarding_houses.id = properties_media.listing_id 
      AND boarding_houses.available = true
    )
  );

-- 4) bookings - tenants manage their bookings, owners can view bookings for their listings
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Tenants insert bookings" ON public.bookings 
  FOR INSERT WITH CHECK (auth.uid() = tenant_id);

CREATE POLICY "Tenants view own bookings" ON public.bookings 
  FOR SELECT USING (auth.uid() = tenant_id);

CREATE POLICY "Tenants update own bookings" ON public.bookings 
  FOR UPDATE USING (auth.uid() = tenant_id);

CREATE POLICY "Owners view listing bookings" ON public.bookings 
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.boarding_houses 
      WHERE boarding_houses.id = bookings.listing_id 
      AND boarding_houses.owner_id = auth.uid()
    )
  );

-- 5) reviews - users can insert reviews, anyone can read
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users insert reviews" ON public.reviews 
  FOR INSERT WITH CHECK (auth.uid() = reviewer_id);

CREATE POLICY "Public read reviews" ON public.reviews 
  FOR SELECT USING (true);

-- 6) favorites - users manage their own favorites
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users manage own favorites" ON public.favorites 
  FOR ALL USING (auth.uid() = user_id);

-- 7) inquiries - users can insert and view their inquiries, owners can view inquiries for their listings
ALTER TABLE public.inquiries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users insert inquiries" ON public.inquiries 
  FOR INSERT WITH CHECK (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Users view own inquiries" ON public.inquiries 
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Owners view listing inquiries" ON public.inquiries 
  FOR SELECT USING (auth.uid() = owner_id);

-- 8) notifications - users view their own notifications
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users view own notifications" ON public.notifications 
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users update own notifications" ON public.notifications 
  FOR UPDATE USING (auth.uid() = user_id);

-- 9) email_otps - no RLS (managed by Edge Functions with service_role key)
-- Edge Functions bypass RLS when using service_role key

-- 10) audit_logs - no client access (admin/service only)
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
-- No policies - only service_role can access

-- ============================================================================
-- TRIGGER: Auto-create user profile when auth.users record is created
-- ============================================================================

-- Function to handle new user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.users (
    id,
    role,
    first_name,
    last_name,
    display_name,
    date_of_birth,
    phone,
    created_at,
    updated_at
  )
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'role', 'tenant'),
    NEW.raw_user_meta_data->>'first_name',
    NEW.raw_user_meta_data->>'last_name',
    CONCAT(
      COALESCE(NEW.raw_user_meta_data->>'first_name', ''),
      ' ',
      COALESCE(NEW.raw_user_meta_data->>'last_name', '')
    ),
    (NEW.raw_user_meta_data->>'dob')::date,
    NEW.raw_user_meta_data->>'phone',
    NOW(),
    NOW()
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger that fires after a new user is inserted into auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user();

-- End of schema
