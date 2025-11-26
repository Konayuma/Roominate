-- Add occupation column to profiles table
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS occupation text;
