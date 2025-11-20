-- Add payment tracking fields to bookings table
ALTER TABLE public.bookings 
ADD COLUMN IF NOT EXISTS payment_reference text,
ADD COLUMN IF NOT EXISTS payment_status text DEFAULT 'pending',
ADD COLUMN IF NOT EXISTS payment_method text,
ADD COLUMN IF NOT EXISTS payment_amount numeric(10,2),
ADD COLUMN IF NOT EXISTS payment_date timestamptz,
ADD COLUMN IF NOT EXISTS transfer_reference text,
ADD COLUMN IF NOT EXISTS transfer_status text DEFAULT 'pending',
ADD COLUMN IF NOT EXISTS transfer_amount numeric(10,2),
ADD COLUMN IF NOT EXISTS transfer_date timestamptz,
ADD COLUMN IF NOT EXISTS owner_id uuid;

-- Create index for payment lookups
CREATE INDEX IF NOT EXISTS idx_bookings_payment_reference ON public.bookings (payment_reference);
CREATE INDEX IF NOT EXISTS idx_bookings_payment_status ON public.bookings (payment_status);
CREATE INDEX IF NOT EXISTS idx_bookings_owner ON public.bookings (owner_id);

-- Add owner_id from boarding_houses via trigger
CREATE OR REPLACE FUNCTION sync_booking_owner()
RETURNS TRIGGER AS $$
BEGIN
  -- Set owner_id from the boarding_house owner when booking is created or updated
  IF NEW.listing_id IS NOT NULL THEN
    SELECT owner_id INTO NEW.owner_id
    FROM public.boarding_houses
    WHERE id = NEW.listing_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to sync owner_id
DROP TRIGGER IF EXISTS trigger_sync_booking_owner ON public.bookings;
CREATE TRIGGER trigger_sync_booking_owner
  BEFORE INSERT OR UPDATE OF listing_id ON public.bookings
  FOR EACH ROW
  EXECUTE FUNCTION sync_booking_owner();

-- Backfill owner_id for existing bookings
UPDATE public.bookings b
SET owner_id = bh.owner_id
FROM public.boarding_houses bh
WHERE b.listing_id = bh.id
AND b.owner_id IS NULL;
