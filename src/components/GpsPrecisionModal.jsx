import React from 'react';
import { X, CheckCircle2, AlertCircle, Compass, Wifi, MapPin } from 'lucide-react';

export default function GpsPrecisionModal({ accuracy, onCalibrate, onClose }) {
  const isHighPrecision = accuracy !== null && accuracy <= 15;
  const isMediumPrecision = accuracy !== null && accuracy > 15 && accuracy <= 50;

  return (
    <div className="settings-modal" style={{ zIndex: 100 }}>
      <div className="settings-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <MapPin size={22} color={isHighPrecision ? '#4ade80' : isMediumPrecision ? '#facc15' : '#f87171'} />
          <h2 style={{ fontSize: '18px', margin: 0 }}>99% GPS Accuracy Guide</h2>
        </div>
        <button className="settings-close" onClick={onClose}>
          <X size={26} />
        </button>
      </div>

      <div className="settings-body" style={{ maxHeight: '75vh', overflowY: 'auto', padding: '16px' }}>
        {/* Status Card */}
        <div style={{
          background: isHighPrecision ? 'rgba(74, 222, 128, 0.15)' : isMediumPrecision ? 'rgba(250, 204, 21, 0.15)' : 'rgba(248, 113, 113, 0.15)',
          border: `1px solid ${isHighPrecision ? '#4ade80' : isMediumPrecision ? '#facc15' : '#f87171'}`,
          borderRadius: '10px',
          padding: '12px 14px',
          marginBottom: '16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <div>
            <div style={{ fontWeight: 600, fontSize: '14px', color: '#fff' }}>
              Current Status: {isHighPrecision ? '🟢 High Precision Locked' : isMediumPrecision ? '🟡 Acquiring Satellites...' : '🔴 Coarse Location'}
            </div>
            <div style={{ fontSize: '12px', color: '#ccc', marginTop: '3px' }}>
              {accuracy !== null ? `Accuracy margin: ±${accuracy} meters` : 'Searching for GPS signal...'}
            </div>
          </div>
          <button 
            onClick={onCalibrate}
            style={{
              background: '#fff',
              color: '#000',
              border: 'none',
              padding: '6px 12px',
              borderRadius: '20px',
              fontWeight: 600,
              fontSize: '12px',
              cursor: 'pointer'
            }}
          >
            Re-Calibrate
          </button>
        </div>

        {/* iPhone Checklist */}
        <h3 style={{ fontSize: '14px', color: '#93c5fd', marginBottom: '10px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
          iPhone (iOS) 99% Precision Steps:
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start', background: 'rgba(255,255,255,0.05)', padding: '10px', borderRadius: '8px' }}>
            <CheckCircle2 size={18} color="#4ade80" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div style={{ fontSize: '13px', lineHeight: 1.4 }}>
              <strong>1. Enable "Precise Location" in iOS:</strong>
              <div style={{ color: '#bbb', fontSize: '12px', marginTop: '2px' }}>
                Open iPhone <strong>Settings</strong> ⚙️ ➡️ <strong>Safari</strong> ➡️ <strong>Location</strong> ➡️ ensure <strong>"Precise Location"</strong> is switched <strong>ON</strong> (Green).
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start', background: 'rgba(255,255,255,0.05)', padding: '10px', borderRadius: '8px' }}>
            <Wifi size={18} color="#60a5fa" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div style={{ fontSize: '13px', lineHeight: 1.4 }}>
              <strong>2. Turn ON Wi-Fi (Even if disconnected):</strong>
              <div style={{ color: '#bbb', fontSize: '12px', marginTop: '2px' }}>
                Apple uses Wi-Fi signals for instant <strong>A-GPS triangulation</strong>, pinpointing your exact building within seconds.
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start', background: 'rgba(255,255,255,0.05)', padding: '10px', borderRadius: '8px' }}>
            <Compass size={18} color="#c084fc" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div style={{ fontSize: '13px', lineHeight: 1.4 }}>
              <strong>3. Clear Sky View:</strong>
              <div style={{ color: '#bbb', fontSize: '12px', marginTop: '2px' }}>
                GPS satellites require an open view. If indoors, move near a window or outdoors to get sub-5m satellite lock.
              </div>
            </div>
          </div>
        </div>

        <button 
          onClick={onClose}
          style={{
            width: '100%',
            marginTop: '20px',
            padding: '12px',
            background: '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: '10px',
            fontWeight: 600,
            cursor: 'pointer',
            fontSize: '14px'
          }}
        >
          Got It, Continue to Camera
        </button>
      </div>
    </div>
  );
}
