export function getOSMTileUrl(lat, lon, zoom = 15) {
  if (!lat || !lon) return null;
  
  const x = Math.floor((lon + 180) / 360 * Math.pow(2, zoom));
  const y = Math.floor((1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2 * Math.pow(2, zoom));
  
  return `https://tile.openstreetmap.org/${zoom}/${x}/${y}.png`;
}
