-- Quick Setup Script for Supabase Notifications
-- Copy and paste this entire script into Supabase SQL Editor
-- Replace 'your-project-ref' and 'your-service-role-key' with your actual values

-- ============================================
-- STEP 1: CREATE NOTIFICATIONS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('booking_update', 'new_message', 'review', 'property_update', 'general')),
    related_id UUID,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON public.notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON public.notifications(created_at DESC);
-- Create partial index for unread notifications (only after table exists)
DROP INDEX IF EXISTS idx_notifications_user_unread;
CREATE INDEX idx_notifications_user_unread ON public.notifications(user_id, is_read) WHERE is_read = false;

-- Add updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON public.notifications
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- RLS policies
DROP POLICY IF EXISTS "Users can view own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Service role can insert notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can delete own notifications" ON public.notifications;

CREATE POLICY "Users can view own notifications"
    ON public.notifications FOR SELECT
    USING (
        user_id IN (
            SELECT id FROM public.users WHERE id = auth.uid()
        )
    );

CREATE POLICY "Users can update own notifications"
    ON public.notifications FOR UPDATE
    USING (
        user_id IN (
            SELECT id FROM public.users WHERE id = auth.uid()
        )
    );

CREATE POLICY "Users can delete own notifications"
    ON public.notifications FOR DELETE
    USING (
        user_id IN (
            SELECT id FROM public.users WHERE id = auth.uid()
        )
    );

CREATE POLICY "Service role can insert notifications"
    ON public.notifications FOR INSERT
    WITH CHECK (true);

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON public.notifications TO authenticated;
GRANT ALL ON public.notifications TO service_role;

-- ============================================
-- STEP 2: CONFIGURE DATABASE SETTINGS
-- ============================================

-- ⚠️ IMPORTANT: Replace these values with your actual credentials!
ALTER DATABASE postgres SET app.settings.supabase_url = 'https://your-project-ref.supabase.co';
ALTER DATABASE postgres SET app.settings.service_role_key = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...your-service-role-key';

-- Reload configuration
SELECT pg_reload_conf();

-- ============================================
-- STEP 3: ENABLE PG_NET EXTENSION
-- ============================================

CREATE EXTENSION IF NOT EXISTS pg_net;

-- ============================================
-- STEP 4: CREATE NOTIFICATION TRIGGER FUNCTION
-- ============================================

CREATE OR REPLACE FUNCTION notify_booking_status_change()
RETURNS TRIGGER AS $$
DECLARE
  function_url TEXT;
  payload JSONB;
  request_id BIGINT;
BEGIN
  -- Build the Edge Function URL
  function_url := current_setting('app.settings.supabase_url', true) || '/functions/v1/notify-booking-update';

  -- Build the payload
  payload := jsonb_build_object(
    'booking_id', NEW.id,
    'status', NEW.status,
    'owner_id', NEW.owner_id,
    'tenant_id', NEW.tenant_id
  );

  -- Call the Edge Function asynchronously
  SELECT net.http_post(
    url := function_url,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer ' || current_setting('app.settings.service_role_key', true)
    ),
    body := payload
  ) INTO request_id;

  RETURN NEW;
EXCEPTION
  WHEN OTHERS THEN
    RAISE WARNING 'Failed to send notification: %', SQLERRM;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- STEP 5: CREATE TRIGGERS
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

-- Grant permissions
GRANT EXECUTE ON FUNCTION notify_booking_status_change() TO authenticated;
GRANT EXECUTE ON FUNCTION notify_booking_status_change() TO service_role;

-- ============================================
-- STEP 6: VERIFICATION
-- ============================================

-- Check if table was created
SELECT 
    'notifications table' as check_item,
    CASE WHEN EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'notifications') 
        THEN '✅ Created' 
        ELSE '❌ Not found' 
    END as status;

-- Check if indexes exist
SELECT 
    'notifications indexes' as check_item,
    COUNT(*)::text || ' indexes created' as status
FROM pg_indexes 
WHERE tablename = 'notifications';

-- Check if RLS is enabled
SELECT 
    'RLS enabled' as check_item,
    CASE WHEN relrowsecurity 
        THEN '✅ Enabled' 
        ELSE '❌ Disabled' 
    END as status
FROM pg_class 
WHERE relname = 'notifications';

-- Check if triggers exist
SELECT 
    'Triggers' as check_item,
    COUNT(*)::text || ' triggers created' as status
FROM pg_trigger 
WHERE tgname LIKE '%notify_booking%';

-- Check if settings are configured
SELECT 
    'Database settings' as check_item,
    CASE 
        WHEN current_setting('app.settings.supabase_url', true) IS NOT NULL 
            AND current_setting('app.settings.supabase_url', true) != 'https://your-project-ref.supabase.co'
        THEN '✅ Configured' 
        ELSE '⚠️  Not configured - please update the settings!' 
    END as status;

-- ============================================
-- SUCCESS MESSAGE
-- ============================================

DO $$
BEGIN
    RAISE NOTICE '==================================================';
    RAISE NOTICE '✅ Notifications system setup complete!';
    RAISE NOTICE '==================================================';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '1. Deploy Edge Function: supabase functions deploy notify-booking-update';
    RAISE NOTICE '2. Update database settings if you see warning above';
    RAISE NOTICE '3. Test by creating a booking in your app';
    RAISE NOTICE '==================================================';
END $$;
