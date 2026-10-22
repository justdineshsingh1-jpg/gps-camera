import cities from './cities.json';

// Calculates distance in km between two lat/long points using Haversine formula
function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
  var R = 6371; // Radius of the earth in km
  var dLat = deg2rad(lat2-lat1);  // deg2rad below
  var dLon = deg2rad(lon2-lon1); 
  var a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
    Math.sin(dLon/2) * Math.sin(dLon/2)
    ; 
  var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
  var d = R * c; // Distance in km
  return d;
}

function deg2rad(deg) {
  return deg * (Math.PI/180)
}

export function getOfflineAddress(lat, lon) {
  if (!lat || !lon) return null;
  
  let nearestCity = null;
  let minDistance = Infinity;

  for (const city of cities) {
    const distance = getDistanceFromLatLonInKm(lat, lon, city.lat, city.lon);
    if (distance < minDistance) {
      minDistance = distance;
      nearestCity = city;
    }
  }

  if (nearestCity) {
    return {
      city: nearestCity.name,
      state: nearestCity.state,
      country: nearestCity.country,
      formatted: `${nearestCity.name}, ${nearestCity.state}, ${nearestCity.country}`,
      distanceStr: `Approx ${minDistance.toFixed(1)} km away`
    };
  }
  
  return null;
}
