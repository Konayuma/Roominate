-- Create notifications table
-- Run this in your Supabase SQL Editor

CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('booking_update', 'new_message', 'review', 'property_update', 'general')),
    related_id UUID, -- id of the related booking/message/review/property
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Add indexes for fast queries
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON public.notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON public.notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON public.notifications(user_id, is_read) WHERE is_read = false;

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

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Service role can insert notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can delete own notifications" ON public.notifications;

-- RLS policies: users can only see their own notifications
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

-- Allow Edge Functions to insert notifications (they use service_role)
CREATE POLICY "Service role can insert notifications"
    ON public.notifications FOR INSERT
    WITH CHECK (true);

-- Grant necessary permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON public.notifications TO authenticated;
GRANT ALL ON public.notifications TO service_role;

-- Create a function to get unread count (optional, but useful)
CREATE OR REPLACE FUNCTION get_unread_notifications_count(user_uuid UUID)
RETURNS INTEGER AS $$
BEGIN
    RETURN (
        SELECT COUNT(*)::INTEGER
        FROM public.notifications
        WHERE user_id = user_uuid AND is_read = false
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission on the function
GRANT EXECUTE ON FUNCTION get_unread_notifications_count(UUID) TO authenticated;

COMMENT ON TABLE public.notifications IS 'Stores user notifications for bookings, messages, reviews, and other events';
COMMENT ON COLUMN public.notifications.type IS 'Type of notification: booking_update, new_message, review, property_update, general';
COMMENT ON COLUMN public.notifications.related_id IS 'UUID of the related entity (booking, message, property, etc.)';
