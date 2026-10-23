import React, { useState, useRef, useEffect, useCallback } from 'react';
import html2canvas from 'html2canvas';
import { Settings, RefreshCcw, LocateFixed } from 'lucide-react';
import Overlay from './components/Overlay';
import FilterBar from './components/FilterBar';
import SettingsModal from './components/SettingsModal';
import GpsPrecisionModal from './components/GpsPrecisionModal';

function App() {
  const videoRef = useRef(null);
  const [stream, setStream] = useState(null);
  const [location, setLocation] = useState(null);
  const [gpsAccuracy, setGpsAccuracy] = useState(null);
  const [isCalibrating, setIsCalibrating] = useState(false);
  const [date, setDate] = useState(new Date());
  const [capturedImage, setCapturedImage] = useState(null);
  const [compassHeading, setCompassHeading] = useState(null);
  const [facingMode, setFacingMode] = useState('environment');
  
  // Pro Features State
  const [activeFilter, setActiveFilter] = useState('none');
  const [showSettings, setShowSettings] = useState(false);
  const [showGpsModal, setShowGpsModal] = useState(false);
  const [stampSettings, setStampSettings] = useState({
    showMap: true,
    showAddress: true,
    showCoords: true,
    showDateTime: true,
    showCompass: true,
    showQRCode: true
  });
  
  // Start camera
  useEffect(() => {
    async function startCamera() {
      try {
        const mediaStream = await navigator.mediaDevices.getUserMedia({
          video: { 
            facingMode: facingMode,
            width: { ideal: 1920 },
            height: { ideal: 1080 }
          }
        });
        setStream(mediaStream);
        if (videoRef.current) {
          videoRef.current.srcObject = mediaStream;
        }
      } catch (err) {
        console.error("Error accessing camera: ", err);
        alert("Camera permission denied or unavailable.");
      }
    }
    startCamera();
    
    return () => {
      if (stream) {
        stream.getTracks().forEach(track => track.stop());
      }
    };
  }, [facingMode]);

  // High Precision GPS Position Filter
  const handleLocationUpdate = useCallback((pos) => {
    const acc = pos.coords.accuracy;
    setGpsAccuracy(Math.round(acc));
    setIsCalibrating(false);

    setLocation((prev) => {
      if (!prev) return pos;
      // If accuracy is high (< 20m), always accept
      if (acc <= 20) return pos;
      // If new reading is tighter than previous
      if (acc <= (prev.coords.accuracy || 9999)) return pos;
      // If moved > 10m
      const dLat = Math.abs(pos.coords.latitude - prev.coords.latitude);
      const dLon = Math.abs(pos.coords.longitude - prev.coords.longitude);
      if (dLat > 0.0001 || dLon > 0.0001) return pos;
      return prev;
    });
  }, []);

  // Force high accuracy GPS refresh
  const calibrateGps = useCallback(() => {
    if (!('geolocation' in navigator)) return;
    setIsCalibrating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => handleLocationUpdate(pos),
      (err) => {
        console.warn('GPS Calibration warning:', err);
        setIsCalibrating(false);
      },
      { enableHighAccuracy: true, maximumAge: 0, timeout: 15000 }
    );
  }, [handleLocationUpdate]);

  // Track location continuously with high precision
  useEffect(() => {
    if ('geolocation' in navigator) {
      // 1. Immediate high accuracy satellite lock
      calibrateGps();

      // 2. High precision continuous tracking
      const watchId = navigator.geolocation.watchPosition(
        (pos) => handleLocationUpdate(pos),
        (err) => console.error("Error watching location: ", err),
        { enableHighAccuracy: true, maximumAge: 0, timeout: 20000 }
      );

      return () => navigator.geolocation.clearWatch(watchId);
    }
  }, [calibrateGps, handleLocationUpdate]);

  // iOS Compass Permission & Orientation tracking
  const requestCompassPermission = async () => {
    if (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function') {
      try {
        const response = await DeviceOrientationEvent.requestPermission();
        if (response === 'granted') {
          console.log('DeviceOrientation permission granted');
        }
      } catch (e) {
        console.error('Compass permission error:', e);
      }
    }
  };

  useEffect(() => {
    const handleOrientation = (event) => {
      let heading = null;
      if (event.webkitCompassHeading !== undefined && event.webkitCompassHeading !== null) {
        // iOS Safari
        heading = event.webkitCompassHeading;
      } else if (event.alpha !== null && event.alpha !== undefined) {
        // Android
        heading = 360 - event.alpha;
      }
      if (heading !== null) {
        setCompassHeading(heading);
      }
    };

    if (window.DeviceOrientationAbsoluteEvent) {
      window.addEventListener("deviceorientationabsolute", handleOrientation);
    } else if (window.DeviceOrientationEvent) {
      window.addEventListener("deviceorientation", handleOrientation);
    }
    
    return () => {
      window.removeEventListener("deviceorientationabsolute", handleOrientation);
      window.removeEventListener("deviceorientation", handleOrientation);
    };
  }, []);

  // Update time every 10 seconds
  useEffect(() => {
    const timer = setInterval(() => setDate(new Date()), 10000);
    return () => clearInterval(timer);
  }, []);

  const capturePhoto = async () => {
    if (!videoRef.current) return;
    
    // Request compass permission if on iOS Safari during first tap
    requestCompassPermission();

    // 1. Draw video frame to canvas
    const video = videoRef.current;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 1920;
    canvas.height = video.videoHeight || 1080;
    const ctx = canvas.getContext('2d');
    
    // Apply CSS filter to the canvas context BEFORE drawing the video
    if (activeFilter !== 'none') {
      ctx.filter = activeFilter;
    }
    
    if (facingMode === 'user') {
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1);
    }
    
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    
    if (facingMode === 'user') {
      // Reset transform
      ctx.setTransform(1, 0, 0, 1, 0, 0);
    }
    
    ctx.filter = 'none'; // reset filter for overlay
    
    // 2. Capture the HTML overlay using html2canvas
    const overlayElement = document.getElementById('gps-overlay');
    if (overlayElement) {
      try {
        const overlayCanvas = await html2canvas(overlayElement, {
          backgroundColor: null,
          scale: 2
        });
        
        const overlayWidth = overlayCanvas.width;
        const overlayHeight = overlayCanvas.height;
        const targetWidth = canvas.width * 0.92;
        const scale = targetWidth / overlayWidth;
        const targetHeight = overlayHeight * scale;
        
        const x = (canvas.width - targetWidth) / 2;
        const y = canvas.height - targetHeight - (20 * scale);
        
        ctx.drawImage(overlayCanvas, x, y, targetWidth, targetHeight);
      } catch (err) {
        console.error("Error rendering overlay: ", err);
      }
    }
    
    // 3. Convert to image
    const dataUrl = canvas.toDataURL('image/jpeg', 0.92);
    setCapturedImage(dataUrl);
  };

  if (capturedImage) {
    return (
      <div className="result-container">
        <img src={capturedImage} alt="Captured" className="result-image" />
        <div className="result-actions">
          <button className="btn btn-secondary" onClick={() => setCapturedImage(null)}>Retake</button>
          <button className="btn btn-primary" onClick={() => {
            const link = document.createElement('a');
            link.href = capturedImage;
            link.download = `GPS_Photo_${Date.now()}.jpg`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
          }}>Save Photo</button>
        </div>
      </div>
    );
  }

  const isHighAcc = gpsAccuracy !== null && gpsAccuracy <= 15;
  const isMedAcc = gpsAccuracy !== null && gpsAccuracy > 15 && gpsAccuracy <= 50;

  return (
    <div className="app-container" onClick={requestCompassPermission}>
      {showSettings && (
        <SettingsModal 
          settings={stampSettings} 
          setSettings={setStampSettings} 
          onClose={() => setShowSettings(false)} 
        />
      )}

      {showGpsModal && (
        <GpsPrecisionModal 
          accuracy={gpsAccuracy}
          onCalibrate={calibrateGps}
          onClose={() => setShowGpsModal(false)}
        />
      )}
      
      {/* Top Bar with GPS Accuracy Badge */}
      <div className="top-bar">
        <div 
          className="gps-badge"
          onClick={(e) => {
            e.stopPropagation();
            setShowGpsModal(true);
          }}
          title="Tap for 99% GPS Accuracy Guide"
        >
          <div 
            className="gps-dot" 
            style={{ 
              backgroundColor: isHighAcc ? '#4ade80' : isMedAcc ? '#facc15' : '#f87171' 
            }} 
          />
          <span>
            {isCalibrating 
              ? 'Calibrating GPS...' 
              : gpsAccuracy !== null 
                ? (isHighAcc ? `99% GPS (±${gpsAccuracy}m)` : `GPS (±${gpsAccuracy}m)`)
                : 'Acquiring Satellites...'}
          </span>
        </div>

        <div className="top-bar-actions">
          <button 
            className="settings-btn" 
            onClick={(e) => {
              e.stopPropagation();
              calibrateGps();
            }}
            title="Calibrate GPS"
          >
            <LocateFixed size={20} color={isCalibrating ? '#4ade80' : '#fff'} />
          </button>

          <button 
            className="settings-btn" 
            onClick={(e) => {
              e.stopPropagation();
              setShowSettings(true);
            }}
            title="Stamp Settings"
          >
            <Settings size={20} />
          </button>
        </div>
      </div>

      <div className="camera-container">
        <video 
          ref={videoRef} 
          autoPlay 
          playsInline 
          muted 
          className="video-stream"
          style={{ filter: activeFilter, transform: facingMode === 'user' ? 'scaleX(-1)' : 'none' }}
        />
        <Overlay 
          location={location} 
          date={date} 
          settings={stampSettings} 
          compassHeading={compassHeading} 
        />
      </div>
      
      <div className="controls-container">
        <FilterBar activeFilter={activeFilter} setActiveFilter={setActiveFilter} />
        
        <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
          <button className="settings-btn" onClick={() => setFacingMode(prev => prev === 'environment' ? 'user' : 'environment')}>
            <RefreshCcw size={24} />
          </button>
          
          <button className="capture-btn" onClick={capturePhoto}>
            <div className="capture-inner"></div>
          </button>
          
          <div style={{ width: 44 }}></div> {/* Spacer for symmetry */}
        </div>
      </div>
    </div>
  );
}

export default App;
