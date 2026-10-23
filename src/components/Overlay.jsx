import React, { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { MapPin, Camera } from 'lucide-react';
import { fetchExactAddress } from '../utils/reverseGeocoding';
import { getOSMTileUrl } from '../utils/mapTile';

export default function Overlay({ location, date, settings, compassHeading }) {
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState('');
  const [addressData, setAddressData] = useState({
    title: 'Acquiring GPS location...',
    address: 'Resolving exact street address...',
    flag: '📍'
  });
  const [mapUrl, setMapUrl] = useState(null);

  const lat = location?.coords?.latitude;
  const lon = location?.coords?.longitude;
  const accuracy = location?.coords?.accuracy ? Math.round(location.coords.accuracy) : null;
  const altitude = location?.coords?.altitude ? Math.round(location.coords.altitude) : null;

  useEffect(() => {
    if (lat && lon) {
      if (settings.showMap) {
        setMapUrl(getOSMTileUrl(lat, lon, 16));
      }

      if (settings.showQRCode) {
        const url = `https://maps.google.com/?q=${lat},${lon}`;
        QRCode.toDataURL(url, { width: 80, margin: 1 }, (err, dataUrl) => {
          if (!err) setQrCodeDataUrl(dataUrl);
        });
      }

      if (settings.showAddress) {
        let isMounted = true;
        fetchExactAddress(lat, lon).then((res) => {
          if (isMounted && res) {
            setAddressData(res);
          }
        });
        return () => {
          isMounted = false;
        };
      }
    }
  }, [lat, lon, settings.showQRCode, settings.showAddress, settings.showMap]);

  const formatDate = (date) => {
    if (!date) return '';
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const d = new Date(date);
    const dayName = days[d.getDay()];
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    
    let hours = d.getHours();
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12;
    hours = hours ? hours : 12;
    
    return `${dayName}, ${dd}/${mm}/${yyyy} ${hours}:${minutes} ${ampm}`;
  };

  const getCompassDirection = (heading) => {
    if (heading === null || heading === undefined) return 'N/A';
    const val = Math.floor((heading / 22.5) + 0.5);
    const arr = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"];
    return arr[(val % 16)];
  };

  return (
    <div className="overlay-container" id="gps-overlay">
      <div className="badge-container">
        <Camera size={12} /> GPS Map Camera
      </div>
      
      {settings.showMap && (
        <div className="overlay-left">
          {mapUrl ? (
            <div style={{position: 'relative', width: '100%', height: '100%'}}>
              <img src={mapUrl} alt="Map Tile" crossOrigin="anonymous" style={{width: '100%', height: '100%', objectFit: 'cover'}} />
              <MapPin className="map-pin" fill="#ea4335" stroke="#fff" strokeWidth={1} style={{zIndex: 2}} />
            </div>
          ) : (
            <div className="offline-map-placeholder">
              <MapPin className="map-pin" fill="#ea4335" stroke="#fff" strokeWidth={1} />
            </div>
          )}
        </div>
      )}
      
      <div className="overlay-middle">
        {settings.showAddress && (
          <>
            <div className="overlay-title">{addressData.title}</div>
            <div className="overlay-address">
              {addressData.flag} {addressData.address}
            </div>
          </>
        )}
        
        {settings.showCoords && (
          <div className="overlay-coords">
            Lat {lat ? lat.toFixed(6) : '...'}° Long {lon ? lon.toFixed(6) : '...'}°
            {accuracy !== null && ` | Acc ±${accuracy}m`}
            {altitude !== null && ` | Alt ${altitude}m`}
          </div>
        )}
        
        {settings.showDateTime && (
          <div className="overlay-coords">
            {formatDate(date)}
          </div>
        )}

        {settings.showCompass && compassHeading !== null && compassHeading !== undefined && (
          <div className="overlay-compass">
            Compass: {Math.round(compassHeading)}° {getCompassDirection(compassHeading)}
          </div>
        )}
      </div>
      
      {settings.showQRCode && qrCodeDataUrl && (
        <div className="overlay-right">
          <div className="qr-container">
            <img src={qrCodeDataUrl} alt="QR Code" width="70" height="70" />
          </div>
        </div>
      )}
    </div>
  );
}
