-- Zip codes table for radius-based location searching
-- Contains US zip codes with latitude/longitude for distance calculations

CREATE TABLE IF NOT EXISTS zip_codes (
  zip VARCHAR(10) PRIMARY KEY,
  city VARCHAR(100) NOT NULL,
  state VARCHAR(2) NOT NULL,
  latitude DECIMAL(10, 7) NOT NULL,
  longitude DECIMAL(10, 7) NOT NULL,
  timezone VARCHAR(50)
);

-- Indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_zip_codes_state ON zip_codes(state);
CREATE INDEX IF NOT EXISTS idx_zip_codes_city_state ON zip_codes(city, state);
CREATE INDEX IF NOT EXISTS idx_zip_codes_coords ON zip_codes(latitude, longitude);

-- Function to calculate distance between two points using Haversine formula
-- Returns distance in miles
CREATE OR REPLACE FUNCTION calculate_distance_miles(
  lat1 DECIMAL,
  lon1 DECIMAL,
  lat2 DECIMAL,
  lon2 DECIMAL
) RETURNS DECIMAL AS $$
DECLARE
  R CONSTANT DECIMAL := 3959; -- Earth's radius in miles
  dlat DECIMAL;
  dlon DECIMAL;
  a DECIMAL;
  c DECIMAL;
BEGIN
  dlat := RADIANS(lat2 - lat1);
  dlon := RADIANS(lon2 - lon1);
  a := SIN(dlat/2) * SIN(dlat/2) + COS(RADIANS(lat1)) * COS(RADIANS(lat2)) * SIN(dlon/2) * SIN(dlon/2);
  c := 2 * ATAN2(SQRT(a), SQRT(1-a));
  RETURN R * c;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to find all zip codes within a radius of a given zip code
CREATE OR REPLACE FUNCTION get_zips_within_radius(
  center_zip VARCHAR(10),
  radius_miles DECIMAL
) RETURNS TABLE(zip VARCHAR(10), distance_miles DECIMAL) AS $$
DECLARE
  center_lat DECIMAL;
  center_lon DECIMAL;
BEGIN
  -- Get center coordinates
  SELECT latitude, longitude INTO center_lat, center_lon
  FROM zip_codes WHERE zip_codes.zip = center_zip;

  IF center_lat IS NULL THEN
    RETURN;
  END IF;

  -- Return all zips within radius
  RETURN QUERY
  SELECT z.zip, calculate_distance_miles(center_lat, center_lon, z.latitude, z.longitude) as distance_miles
  FROM zip_codes z
  WHERE calculate_distance_miles(center_lat, center_lon, z.latitude, z.longitude) <= radius_miles
  ORDER BY distance_miles;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON TABLE zip_codes IS 'US zip codes with coordinates for radius-based event searching';
COMMENT ON FUNCTION calculate_distance_miles IS 'Calculate distance between two lat/long points using Haversine formula';
COMMENT ON FUNCTION get_zips_within_radius IS 'Find all zip codes within X miles of a given zip code';
