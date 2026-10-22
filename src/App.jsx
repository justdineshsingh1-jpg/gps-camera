import React, { useState, useRef, useEffect } from 'react';
import html2canvas from 'html2canvas';
import { Settings, RefreshCcw } from 'lucide-react';
import Overlay from './components/Overlay';
import FilterBar from './components/FilterBar';
import SettingsModal from './components/SettingsModal';

function App() {
  const videoRef = useRef(null);
  const [stream, setStream] = useState(null);
  const [location, setLocation] = useState(null);
  const [date, setDate] = useState(new Date());
  const [capturedImage, setCapturedImage] = useState(null);
  const [compassHeading, setCompassHeading] = useState(null);
  const [facingMode, setFacingMode] = useState('environment'); // New state
  
  // Pro Features State
  const [activeFilter, setActiveFilter] = useState('none');
  const [showSettings, setShowSettings] = useState(false);
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
          video: { facingMode: facingMode }
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
  }, [facingMode]); // Rerun when facingMode changes

  // Track location
  useEffect(() => {
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => setLocation(pos),
        (err) => console.log("Quick location failed", err),
        { enableHighAccuracy: false, maximumAge: 60000, timeout: 5000 }
      );

      const watchId = navigator.geolocation.watchPosition(
        (pos) => setLocation(pos),
        (err) => console.error("Error getting location: ", err),
        { enableHighAccuracy: true, maximumAge: 0 }
      );
      return () => navigator.geolocation.clearWatch(watchId);
    }
  }, []);

  // Track compass
  useEffect(() => {
    const handleOrientation = (event) => {
      let heading = null;
      if (event.webkitCompassHeading) {
        // iOS
        heading = event.webkitCompassHeading;
      } else if (event.alpha !== null) {
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

  // Update time every minute
  useEffect(() => {
    const timer = setInterval(() => setDate(new Date()), 60000);
    return () => clearInterval(timer);
  }, []);

  const capturePhoto = async () => {
    if (!videoRef.current) return;
    
    // 1. Draw video frame to canvas
    const video = videoRef.current;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
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
    const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
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

  return (
    <div className="app-container">
      {showSettings && (
        <SettingsModal 
          settings={stampSettings} 
          setSettings={setStampSettings} 
          onClose={() => setShowSettings(false)} 
        />
      )}
      
      <div className="top-bar">
        <button className="settings-btn" onClick={() => setShowSettings(true)}>
          <Settings size={24} />
        </button>
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
