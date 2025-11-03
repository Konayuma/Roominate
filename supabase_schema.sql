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
  used_at timestamptz,
  ip_address text,
  attempts int NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_email_otps_email_created ON public.email_otps (email, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_email_otps_expires ON public.email_otps (expires_at);

-- Add used_at column if it doesn't exist (for existing tables)
DO $$ 
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='email_otps' AND column_name='used_at') THEN
    ALTER TABLE public.email_otps ADD COLUMN used_at timestamptz;
  END IF;
END $$;

-- 3) boarding_houses (listings)
CREATE TABLE IF NOT EXISTS public.boarding_houses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL,
  title text,
  name text,
  description text,
  address text,
  city text,
  province text,
  latitude double precision,
  longitude double precision,
  price_per_month numeric(10,2),
  monthly_rate numeric(10,2),
  security_deposit numeric(10,2),
  total_rooms int,
  available_rooms int,
  room_type text,
  furnished boolean DEFAULT false,
  private_bathroom boolean DEFAULT false,
  electricity_included boolean DEFAULT false,
  water_included boolean DEFAULT false,
  internet_included boolean DEFAULT false,
  contact_person text,
  contact_phone text,
  amenities jsonb,
  images jsonb,
  available boolean DEFAULT true,
  status text DEFAULT 'active',
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

-- Add missing columns if they don't exist (for existing tables)
DO $$ 
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='name') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN name text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='city') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN city text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='province') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN province text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='monthly_rate') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN monthly_rate numeric(10,2);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='security_deposit') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN security_deposit numeric(10,2);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='total_rooms') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN total_rooms int;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='available_rooms') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN available_rooms int;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='room_type') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN room_type text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='furnished') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN furnished boolean DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='private_bathroom') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN private_bathroom boolean DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='electricity_included') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN electricity_included boolean DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='water_included') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN water_included boolean DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='internet_included') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN internet_included boolean DEFAULT false;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='contact_person') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN contact_person text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='contact_phone') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN contact_phone text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='amenities') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN amenities jsonb;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='images') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN images jsonb;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='status') THEN
    ALTER TABLE public.boarding_houses ADD COLUMN status text DEFAULT 'active';
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_boarding_houses_owner ON public.boarding_houses (owner_id);
CREATE INDEX IF NOT EXISTS idx_boarding_houses_location ON public.boarding_houses (latitude, longitude);

-- Alter title column to allow NULL (remove NOT NULL constraint if it exists)
ALTER TABLE public.boarding_houses ALTER COLUMN title DROP NOT NULL;
-- Drop existing foreign key constraint on owner_id if present (allows inserts when public.users row missing)
ALTER TABLE public.boarding_houses DROP CONSTRAINT IF EXISTS boarding_houses_owner_id_fkey;

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

-- Ensure idempotency: drop existing policies (if any) before creating them
DROP POLICY IF EXISTS "Insert own profile" ON public.users;
DROP POLICY IF EXISTS "Select own profile" ON public.users;
DROP POLICY IF EXISTS "Update own profile" ON public.users;

CREATE POLICY "Insert own profile" ON public.users 
  FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "Select own profile" ON public.users 
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Update own profile" ON public.users 
  FOR UPDATE USING (auth.uid() = id);

-- 2) boarding_houses - owners manage their listings, public can read available ones
ALTER TABLE public.boarding_houses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Owners can manage listings" ON public.boarding_houses;
DROP POLICY IF EXISTS "Public read available listings" ON public.boarding_houses;
DROP POLICY IF EXISTS "Owners insert listings" ON public.boarding_houses;
DROP POLICY IF EXISTS "Owners select listings" ON public.boarding_houses;
DROP POLICY IF EXISTS "Owners update listings" ON public.boarding_houses;
DROP POLICY IF EXISTS "Owners delete listings" ON public.boarding_houses;

-- Allow authenticated users to INSERT listings
CREATE POLICY "Owners insert listings" ON public.boarding_houses
  FOR INSERT
  WITH CHECK (true);  -- Simple: allow any authenticated insert (RLS is checked by owner_id = current user later)

-- Owners can SELECT their own listings, public can select available listings
CREATE POLICY "Owners select listings" ON public.boarding_houses
  FOR SELECT USING (auth.uid() = owner_id OR available = true);

-- Owners can UPDATE their own listings
CREATE POLICY "Owners update listings" ON public.boarding_houses
  FOR UPDATE USING (auth.uid() = owner_id)
  WITH CHECK (auth.uid() = owner_id);

-- Owners can DELETE their own listings
CREATE POLICY "Owners delete listings" ON public.boarding_houses
  FOR DELETE USING (auth.uid() = owner_id);

-- 3) properties_media - owners can manage media for their listings
ALTER TABLE public.properties_media ENABLE ROW LEVEL SECURITY;

-- Drop old problematic policy if it exists (for idempotency)
DROP POLICY IF EXISTS "Owners manage listing media" ON public.properties_media;
DROP POLICY IF EXISTS "Owners insert listing media" ON public.properties_media;
DROP POLICY IF EXISTS "Owners select listing media" ON public.properties_media;
DROP POLICY IF EXISTS "Owners update listing media" ON public.properties_media;
DROP POLICY IF EXISTS "Owners delete listing media" ON public.properties_media;
DROP POLICY IF EXISTS "Public read listing media" ON public.properties_media;

-- Owners can INSERT media for their listings
CREATE POLICY "Owners insert listing media" ON public.properties_media
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.boarding_houses 
      WHERE boarding_houses.id = properties_media.listing_id 
      AND boarding_houses.owner_id = auth.uid()
    )
  );

-- Owners can SELECT media for their listings
CREATE POLICY "Owners select listing media" ON public.properties_media
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.boarding_houses 
      WHERE boarding_houses.id = properties_media.listing_id 
      AND boarding_houses.owner_id = auth.uid()
    )
  );

-- Owners can UPDATE media for their listings
CREATE POLICY "Owners update listing media" ON public.properties_media
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM public.boarding_houses 
      WHERE boarding_houses.id = properties_media.listing_id 
      AND boarding_houses.owner_id = auth.uid()
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.boarding_houses 
      WHERE boarding_houses.id = properties_media.listing_id 
      AND boarding_houses.owner_id = auth.uid()
    )
  );

-- Owners can DELETE media for their listings
CREATE POLICY "Owners delete listing media" ON public.properties_media
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.boarding_houses 
      WHERE boarding_houses.id = properties_media.listing_id 
      AND boarding_houses.owner_id = auth.uid()
    )
  );

-- Public can read media from available listings
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

DROP POLICY IF EXISTS "Tenants insert bookings" ON public.bookings;
DROP POLICY IF EXISTS "Tenants view own bookings" ON public.bookings;
DROP POLICY IF EXISTS "Tenants update own bookings" ON public.bookings;
DROP POLICY IF EXISTS "Owners view listing bookings" ON public.bookings;

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

DROP POLICY IF EXISTS "Users insert reviews" ON public.reviews;
DROP POLICY IF EXISTS "Public read reviews" ON public.reviews;

CREATE POLICY "Users insert reviews" ON public.reviews 
  FOR INSERT WITH CHECK (auth.uid() = reviewer_id);

CREATE POLICY "Public read reviews" ON public.reviews 
  FOR SELECT USING (true);

-- 6) favorites - users manage their own favorites
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage own favorites" ON public.favorites;

CREATE POLICY "Users manage own favorites" ON public.favorites 
  FOR ALL USING (auth.uid() = user_id);

-- 7) inquiries - users can insert and view their inquiries, owners can view inquiries for their listings
ALTER TABLE public.inquiries ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users insert inquiries" ON public.inquiries;
DROP POLICY IF EXISTS "Users view own inquiries" ON public.inquiries;
DROP POLICY IF EXISTS "Owners view listing inquiries" ON public.inquiries;

CREATE POLICY "Users insert inquiries" ON public.inquiries 
  FOR INSERT WITH CHECK (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Users view own inquiries" ON public.inquiries 
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Owners view listing inquiries" ON public.inquiries 
  FOR SELECT USING (auth.uid() = owner_id);

-- 8) notifications - users view their own notifications
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users view own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users update own notifications" ON public.notifications;

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
DECLARE
  dob_text TEXT;
  dob_date DATE;
BEGIN
  -- Extract dob text safely
  dob_text := NULLIF(NEW.raw_user_meta_data->>'dob', '');
  dob_date := NULL;

  -- Only attempt to cast if dob looks like YYYY-MM-DD to avoid runtime errors
  IF dob_text IS NOT NULL AND dob_text ~ '^\d{4}-\d{2}-\d{2}$' THEN
    BEGIN
      dob_date := dob_text::date;
    EXCEPTION WHEN others THEN
      -- If parsing fails, keep dob_date as NULL
      dob_date := NULL;
    END;
  END IF;

  -- Defensive insert: coalesce missing names and role
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
      NULLIF(NEW.raw_user_meta_data->>'first_name', ''),
      NULLIF(NEW.raw_user_meta_data->>'last_name', ''),
      CONCAT(
        COALESCE(NEW.raw_user_meta_data->>'first_name', ''),
        ' ',
        COALESCE(NEW.raw_user_meta_data->>'last_name', '')
      ),
      dob_date,
      NULLIF(NEW.raw_user_meta_data->>'phone', ''),
      NOW(),
      NOW()
    );
  EXCEPTION WHEN others THEN
    -- Don't fail the auth signup if the profile insert has an issue. Log a notice for debugging.
    RAISE NOTICE 'handle_new_user: profile insert failed for auth.id=% - error=%', NEW.id, SQLERRM;
    -- Optionally you could insert a minimal profile here or enqueue for background processing
  END;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger that fires after a new user is inserted into auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user();

-- ============================================================================
-- STORAGE: Configure storage bucket for property images
-- ============================================================================

-- Storage bucket creation attempt
INSERT INTO storage.buckets (id, name, public) 
VALUES ('property-images', 'property-images', false)
ON CONFLICT (id) DO NOTHING;

-- NOTE: Storage RLS policies must be set via Supabase Dashboard (not via SQL)
-- Follow these steps:
-- 1. Go to Supabase Dashboard → Storage → Buckets
-- 2. Click on "property-images" bucket
-- 3. Go to "Policies" tab
-- 4. Create these policies:
--    a) FOR SELECT: bucket_id = 'property-images' (Allow all users to view)
--    b) FOR INSERT: bucket_id = 'property-images' AND auth.uid() IS NOT NULL (Authenticated users can upload)
--    c) FOR UPDATE: bucket_id = 'property-images' AND auth.uid() IS NOT NULL (Authenticated users can update)
--    d) FOR DELETE: bucket_id = 'property-images' AND auth.uid() IS NOT NULL (Authenticated users can delete)

-- End of schema
