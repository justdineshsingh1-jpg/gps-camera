import { getOfflineAddress } from './offlineGeocoding';

// Cache for recent geocoding results
const geocodeCache = new Map();

function getCacheKey(lat, lon) {
  // Round to ~11 meters (4 decimal places)
  return `${lat.toFixed(4)},${lon.toFixed(4)}`;
}

export async function fetchExactAddress(lat, lon) {
  if (!lat || !lon) return null;

  const key = getCacheKey(lat, lon);
  if (geocodeCache.has(key)) {
    return geocodeCache.get(key);
  }

  // 1. Try OpenStreetMap Nominatim reverse geocoding
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);
    
    const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}&addressdetails=1`;
    const res = await fetch(url, {
      signal: controller.signal,
      headers: {
        'Accept': 'application/json'
      }
    });
    clearTimeout(timeoutId);

    if (res.ok) {
      const data = await res.json();
      const addr = data.address || {};

      const locality = addr.suburb || addr.neighbourhood || addr.residential || addr.city_district || addr.quarter || '';
      const city = addr.city || addr.town || addr.village || addr.county || '';
      const state = addr.state || '';
      const country = addr.country || 'India';
      const postcode = addr.postcode || '';
      const road = addr.road || addr.street || addr.pedestrian || addr.footway || '';
      const house = addr.house_number || addr.building || addr.amenity || '';

      const titleParts = [locality, city, state].filter(Boolean);
      const uniqueTitle = Array.from(new Set(titleParts)).join(', ') || city || state || country;

      const fullParts = [house, road, locality, city, state, postcode, country].filter(Boolean);
      const uniqueFull = Array.from(new Set(fullParts)).join(', ');

      const isIndia = (addr.country_code === 'in' || country.toLowerCase().includes('india'));
      const flag = isIndia ? '🇮🇳' : '📍';

      const result = {
        title: uniqueTitle,
        address: uniqueFull,
        flag: flag,
        source: 'osm'
      };

      geocodeCache.set(key, result);
      return result;
    }
  } catch (err) {
    console.warn('Nominatim geocode timed out or failed, trying Photon fallback...', err);
  }

  // 2. Try Photon Komoot API fallback (CORS enabled, fast, OSM-based)
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);

    const url = `https://photon.komoot.io/reverse?lat=${lat}&lon=${lon}`;
    const res = await fetch(url, {
      signal: controller.signal
    });
    clearTimeout(timeoutId);

    if (res.ok) {
      const data = await res.json();
      const features = data.features || [];
      if (features.length > 0) {
        const p = features[0].properties || {};
        const name = p.name || '';
        const street = p.street || '';
        const district = p.district || p.suburb || '';
        const city = p.city || p.town || '';
        const state = p.state || '';
        const country = p.country || 'India';
        const postcode = p.postcode || '';

        const titleParts = [district, city, state].filter(Boolean);
        const uniqueTitle = Array.from(new Set(titleParts)).join(', ') || name || city || state;

        const fullParts = [name, street, district, city, state, postcode, country].filter(Boolean);
        const uniqueFull = Array.from(new Set(fullParts)).join(', ');

        const isIndia = (p.countrycode === 'IN' || country.toLowerCase().includes('india'));
        const flag = isIndia ? '🇮🇳' : '📍';

        const result = {
          title: uniqueTitle,
          address: uniqueFull,
          flag: flag,
          source: 'photon'
        };

        geocodeCache.set(key, result);
        return result;
      }
    }
  } catch (err) {
    console.warn('Photon fallback geocode failed, using offline fallback...', err);
  }

  // 3. Fallback to offline database if completely offline
  const offlineResult = getOfflineAddress(lat, lon);
  if (offlineResult) {
    const isIndia = offlineResult.country === 'India';
    return {
      title: `${offlineResult.city}, ${offlineResult.state}`,
      address: `${offlineResult.formatted}`,
      flag: isIndia ? '🇮🇳' : '📍',
      source: 'offline'
    };
  }

  return {
    title: 'Current GPS Location',
    address: `Lat: ${lat.toFixed(6)}, Lon: ${lon.toFixed(6)}`,
    flag: '📍',
    source: 'coords'
  };
}
