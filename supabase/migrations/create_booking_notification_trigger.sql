-- Database Trigger to automatically notify on booking status changes
-- Run this in your Supabase SQL Editor AFTER creating the Edge Function

-- First, enable the pg_net extension if not already enabled
CREATE EXTENSION IF NOT EXISTS pg_net;

-- Create the function that will be called by the trigger
CREATE OR REPLACE FUNCTION notify_booking_status_change()
RETURNS TRIGGER AS $$
DECLARE
  function_url TEXT;
  payload JSONB;
  request_id BIGINT;
BEGIN
  -- Build the Edge Function URL
  function_url := current_setting('app.settings.supabase_url', true) || '/functions/v1/notify-booking-update';
  
  -- If app.settings.supabase_url is not set, use the SUPABASE_URL environment variable
  IF function_url IS NULL OR function_url = '' THEN
    function_url := 'https://your-project-ref.supabase.co/functions/v1/notify-booking-update';
  END IF;

  -- Build the payload
  payload := jsonb_build_object(
    'booking_id', NEW.id,
    'status', NEW.status,
    'owner_id', NEW.owner_id,
    'tenant_id', NEW.tenant_id
  );

  -- Log the notification attempt
  RAISE NOTICE 'Sending notification for booking % with status %', NEW.id, NEW.status;

  -- Call the Edge Function asynchronously using pg_net
  -- Note: Replace 'your-anon-key' with your actual Supabase anon key
  SELECT net.http_post(
    url := function_url,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer ' || current_setting('app.settings.service_role_key', true)
    ),
    body := payload
  ) INTO request_id;

  RAISE NOTICE 'Notification request sent with ID: %', request_id;

  RETURN NEW;
EXCEPTION
  WHEN OTHERS THEN
    -- Log the error but don't fail the booking update
    RAISE WARNING 'Failed to send notification: %', SQLERRM;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger for INSERT (new bookings)
DROP TRIGGER IF EXISTS trigger_notify_booking_created ON public.bookings;
CREATE TRIGGER trigger_notify_booking_created
AFTER INSERT ON public.bookings
FOR EACH ROW
WHEN (NEW.status = 'pending')
EXECUTE FUNCTION notify_booking_status_change();

-- Create trigger for UPDATE (status changes)
DROP TRIGGER IF EXISTS trigger_notify_booking_updated ON public.bookings;
CREATE TRIGGER trigger_notify_booking_updated
AFTER UPDATE OF status ON public.bookings
FOR EACH ROW
WHEN (OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION notify_booking_status_change();

-- Grant necessary permissions
GRANT EXECUTE ON FUNCTION notify_booking_status_change() TO authenticated;
GRANT EXECUTE ON FUNCTION notify_booking_status_change() TO service_role;

COMMENT ON FUNCTION notify_booking_status_change() IS 'Automatically sends notifications via Edge Function when booking status changes';

-- IMPORTANT SETUP STEPS:
-- 1. Deploy the Edge Function first: supabase functions deploy notify-booking-update
-- 2. Set the configuration parameters (run in psql or SQL Editor):
--    ALTER DATABASE postgres SET app.settings.supabase_url = 'https://your-project-ref.supabase.co';
--    ALTER DATABASE postgres SET app.settings.service_role_key = 'your-service-role-key';
-- 3. Reload the configuration:
--    SELECT pg_reload_conf();
