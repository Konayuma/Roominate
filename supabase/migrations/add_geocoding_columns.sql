-- Add geocoding columns to boarding_houses table
-- Run this in Supabase SQL Editor

-- Ensure latitude and longitude columns exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='latitude') THEN
        ALTER TABLE public.boarding_houses ADD COLUMN latitude double precision;
        RAISE NOTICE 'Added latitude coalumn';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='boarding_houses' AND column_name='longitude') THEN
        ALTER TABLE public.boarding_houses ADD COLUMN longitude double precision;
        RAISE NOTICE 'Added longitude column';
    END IF;
END $$;

-- Create index on coordinates for faster geospatial queries
DROP INDEX IF EXISTS idx_boarding_houses_location;
CREATE INDEX idx_boarding_houses_location ON public.boarding_houses(latitude, longitude) 
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Create helper function to find nearby properties
CREATE OR REPLACE FUNCTION find_nearby_properties(
    user_latitude double precision,
    user_longitude double precision,
    radius_km double precision DEFAULT 5.0
)
RETURNS TABLE (
    id uuid,
    name text,
    address text,
    latitude double precision,
    longitude double precision,
    price_per_month numeric,
    distance_km double precision
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        bh.id,
        bh.name,
        bh.address,
        bh.latitude,
        bh.longitude,
        bh.price_per_month,
        -- Calculate distance using Haversine formula (approximation)
        SQRT(
            POWER(latitude - user_latitude, 2) + 
            POWER(longitude - user_longitude, 2)
        ) * 111.139 AS distance_km
    FROM public.boarding_houses bh
    WHERE 
        bh.latitude IS NOT NULL 
        AND bh.longitude IS NOT NULL
        AND bh.available = true
        AND SQRT(
            POWER(bh.latitude - user_latitude, 2) + 
            POWER(bh.longitude - user_longitude, 2)
        ) * 111.139 <= radius_km
    ORDER BY distance_km ASC;
END;
$$ LANGUAGE plpgsql STABLE;

GRANT EXECUTE ON FUNCTION find_nearby_properties(double precision, double precision, double precision) TO authenticated;

-- ============================================
-- VERIFICATION
-- ============================================

DO $$
DECLARE
    lat_exists BOOLEAN;
    lng_exists BOOLEAN;
    index_count INTEGER;
BEGIN
    -- Check columns
    SELECT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='boarding_houses' AND column_name='latitude') INTO lat_exists;
    SELECT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='boarding_houses' AND column_name='longitude') INTO lng_exists;
    
    -- Check index
    SELECT COUNT(*) INTO index_count FROM pg_indexes 
    WHERE tablename='boarding_houses' AND indexname='idx_boarding_houses_location';
    
    RAISE NOTICE '';
    RAISE NOTICE '==================================================';
    RAISE NOTICE '     GEOCODING SETUP COMPLETE!';
    RAISE NOTICE '==================================================';
    RAISE NOTICE '';
    RAISE NOTICE '✅ Latitude column: %', CASE WHEN lat_exists THEN 'YES' ELSE 'NO' END;
    RAISE NOTICE '✅ Longitude column: %', CASE WHEN lng_exists THEN 'YES' ELSE 'NO' END;
    RAISE NOTICE '✅ Location index: %', CASE WHEN index_count > 0 THEN 'YES' ELSE 'NO' END;
    RAISE NOTICE '';
    RAISE NOTICE '==================================================';
END $$;
