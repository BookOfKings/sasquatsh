-- Add banner_image_url to raffles table for custom raffle banners
ALTER TABLE raffles ADD COLUMN IF NOT EXISTS banner_image_url TEXT;

COMMENT ON COLUMN raffles.banner_image_url IS 'Optional custom banner image URL for the raffle (displayed on home page)';
