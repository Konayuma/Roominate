-- Simplified Supabase Notifications Setup
-- Run this in Supabase SQL Editor
-- No configuration needed - just run it!

-- ============================================
-- STEP 1: CREATE NOTIFICATIONS TABLE
-- ============================================

-- Drop table if it exists (for clean reinstall)
-- DROP TABLE IF EXISTS public.notifications CASCADE;

CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('booking_update', 'new_message', 'review', 'property_update', 'general')),
    related_id UUID,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Verify table structure
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'notifications' 
        AND column_name = 'is_read'
    ) THEN
        RAISE EXCEPTION 'Table notifications exists but is_read column is missing!';
    END IF;
END $$;

-- Add foreign key if users table exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'users' AND schemaname = 'public') THEN
        BEGIN
            ALTER TABLE public.notifications 
            ADD CONSTRAINT notifications_user_id_fkey 
            FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
        EXCEPTION
            WHEN duplicate_object THEN 
                NULL; -- Constraint already exists, ignore
        END;
    END IF;
END $$;

-- ============================================
-- STEP 2: CREATE INDEXES
-- ============================================

-- Drop existing indexes first
DROP INDEX IF EXISTS idx_notifications_user_id;
DROP INDEX IF EXISTS idx_notifications_is_read;
DROP INDEX IF EXISTS idx_notifications_created_at;
DROP INDEX IF EXISTS idx_notifications_user_unread;

-- Create indexes
CREATE INDEX idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX idx_notifications_is_read ON public.notifications(is_read);
CREATE INDEX idx_notifications_created_at ON public.notifications(created_at DESC);
CREATE INDEX idx_notifications_user_unread ON public.notifications(user_id, is_read) WHERE is_read = false;

-- ============================================
-- STEP 3: UPDATED_AT TRIGGER
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_notifications_updated_at ON public.notifications;
CREATE TRIGGER update_notifications_updated_at 
BEFORE UPDATE ON public.notifications
FOR EACH ROW 
EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- STEP 4: ENABLE RLS AND CREATE POLICIES
-- ============================================

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Drop existing policies
DROP POLICY IF EXISTS "Users can view own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can delete own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Service role can insert notifications" ON public.notifications;

-- Create RLS policies
CREATE POLICY "Users can view own notifications"
    ON public.notifications FOR SELECT
    USING (user_id::text = auth.uid()::text);

CREATE POLICY "Users can update own notifications"
    ON public.notifications FOR UPDATE
    USING (user_id::text = auth.uid()::text);

CREATE POLICY "Users can delete own notifications"
    ON public.notifications FOR DELETE
    USING (user_id::text = auth.uid()::text);

CREATE POLICY "Service role can insert notifications"
    ON public.notifications FOR INSERT
    WITH CHECK (true);

-- ============================================
-- STEP 5: GRANT PERMISSIONS
-- ============================================

GRANT SELECT, INSERT, UPDATE, DELETE ON public.notifications TO authenticated;
GRANT ALL ON public.notifications TO service_role;

-- ============================================
-- STEP 6: CREATE HELPER FUNCTION
-- ============================================

CREATE OR REPLACE FUNCTION get_unread_notifications_count(user_uuid UUID)
RETURNS INTEGER AS $$
DECLARE
    unread_count INTEGER;
BEGIN
    SELECT COUNT(*)::INTEGER INTO unread_count
    FROM public.notifications
    WHERE user_id = user_uuid AND is_read = false;
    
    RETURN unread_count;
EXCEPTION
    WHEN undefined_table THEN
        RETURN 0;
    WHEN undefined_column THEN
        RETURN 0;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION get_unread_notifications_count(UUID) TO authenticated;

-- ============================================
-- STEP 7: ENABLE PG_NET (for triggers)
-- ============================================

CREATE EXTENSION IF NOT EXISTS pg_net;

-- ============================================
-- STEP 8: CREATE BOOKING NOTIFICATION FUNCTION
-- ============================================

CREATE OR REPLACE FUNCTION notify_booking_status_change()
RETURNS TRIGGER AS $$
DECLARE
  function_url TEXT;
  payload JSONB;
  request_id BIGINT;
BEGIN
  -- Get the Supabase URL from settings or use default
  function_url := coalesce(
    current_setting('app.settings.supabase_url', true),
    'https://your-project-ref.supabase.co'
  ) || '/functions/v1/notify-booking-update';

  -- Build the payload
  payload := jsonb_build_object(
    'booking_id', NEW.id,
    'status', NEW.status,
    'owner_id', NEW.owner_id,
    'tenant_id', NEW.tenant_id
  );

  -- Call the Edge Function asynchronously
  BEGIN
    SELECT net.http_post(
      url := function_url,
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer ' || coalesce(
          current_setting('app.settings.service_role_key', true),
          'your-service-role-key'
        )
      ),
      body := payload
    ) INTO request_id;
    
    RAISE NOTICE 'Notification request sent with ID: %', request_id;
  EXCEPTION
    WHEN OTHERS THEN
      RAISE WARNING 'Failed to send notification: %', SQLERRM;
  END;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- STEP 9: CREATE TRIGGERS
-- ============================================

-- Trigger for new bookings
DROP TRIGGER IF EXISTS trigger_notify_booking_created ON public.bookings;
CREATE TRIGGER trigger_notify_booking_created
AFTER INSERT ON public.bookings
FOR EACH ROW
WHEN (NEW.status = 'pending')
EXECUTE FUNCTION notify_booking_status_change();

-- Trigger for booking status updates
DROP TRIGGER IF EXISTS trigger_notify_booking_updated ON public.bookings;
CREATE TRIGGER trigger_notify_booking_updated
AFTER UPDATE OF status ON public.bookings
FOR EACH ROW
WHEN (OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION notify_booking_status_change();

GRANT EXECUTE ON FUNCTION notify_booking_status_change() TO authenticated;
GRANT EXECUTE ON FUNCTION notify_booking_status_change() TO service_role;

-- ============================================
-- STEP 10: CONFIGURE DATABASE SETTINGS (OPTIONAL)
-- ============================================

-- Run these commands separately after the main script:
-- ALTER DATABASE postgres SET app.settings.supabase_url = 'https://your-project-ref.supabase.co';
-- ALTER DATABASE postgres SET app.settings.service_role_key = 'your-service-role-key';
-- SELECT pg_reload_conf();

-- ============================================
-- VERIFICATION & SUCCESS MESSAGE
-- ============================================

DO $$
DECLARE
    table_exists BOOLEAN;
    index_count INTEGER;
    rls_enabled BOOLEAN;
    trigger_count INTEGER;
BEGIN
    -- Check table
    SELECT EXISTS (
        SELECT 1 FROM pg_tables 
        WHERE tablename = 'notifications' AND schemaname = 'public'
    ) INTO table_exists;
    
    -- Check indexes
    SELECT COUNT(*) INTO index_count
    FROM pg_indexes 
    WHERE tablename = 'notifications' AND schemaname = 'public';
    
    -- Check RLS
    SELECT relrowsecurity INTO rls_enabled
    FROM pg_class 
    WHERE relname = 'notifications';
    
    -- Check triggers
    SELECT COUNT(*) INTO trigger_count
    FROM pg_trigger 
    WHERE tgname LIKE '%notify_booking%';
    
    -- Display results
    RAISE NOTICE '';
    RAISE NOTICE '==================================================';
    RAISE NOTICE '         NOTIFICATIONS SETUP COMPLETE!';
    RAISE NOTICE '==================================================';
    RAISE NOTICE '';
    RAISE NOTICE '✅ Table created: %', CASE WHEN table_exists THEN 'YES' ELSE 'NO' END;
    RAISE NOTICE '✅ Indexes created: %', index_count;
    RAISE NOTICE '✅ RLS enabled: %', CASE WHEN rls_enabled THEN 'YES' ELSE 'NO' END;
    RAISE NOTICE '✅ Triggers created: %', trigger_count;
    RAISE NOTICE '';
    RAISE NOTICE '==================================================';
    RAISE NOTICE 'NEXT STEPS:';
    RAISE NOTICE '==================================================';
    RAISE NOTICE '1. Deploy Edge Function:';
    RAISE NOTICE '   supabase functions deploy notify-booking-update';
    RAISE NOTICE '';
    RAISE NOTICE '2. Configure database settings (run separately):';
    RAISE NOTICE '   ALTER DATABASE postgres SET app.settings.supabase_url = ''https://your-project.supabase.co'';';
    RAISE NOTICE '   ALTER DATABASE postgres SET app.settings.service_role_key = ''your-key'';';
    RAISE NOTICE '   SELECT pg_reload_conf();';
    RAISE NOTICE '';
    RAISE NOTICE '3. Test by creating a booking in your app!';
    RAISE NOTICE '==================================================';
    RAISE NOTICE '';
END $$;
