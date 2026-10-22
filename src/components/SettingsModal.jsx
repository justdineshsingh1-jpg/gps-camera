import React from 'react';
import { X } from 'lucide-react';

export default function SettingsModal({ settings, setSettings, onClose }) {
  
  const toggleSetting = (key) => {
    setSettings(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const OptionToggle = ({ label, settingKey }) => (
    <div className="setting-item">
      <span>{label}</span>
      <label className="switch">
        <input 
          type="checkbox" 
          checked={settings[settingKey]} 
          onChange={() => toggleSetting(settingKey)} 
        />
        <span className="slider"></span>
      </label>
    </div>
  );

  return (
    <div className="settings-modal">
      <div className="settings-header">
        <h2>Stamp Templates & Settings</h2>
        <button className="settings-close" onClick={onClose}>
          <X size={28} />
        </button>
      </div>
      
      <div className="settings-body">
        <OptionToggle label="Show Map Graphic" settingKey="showMap" />
        <OptionToggle label="Show Full Address" settingKey="showAddress" />
        <OptionToggle label="Show Lat / Long Coordinates" settingKey="showCoords" />
        <OptionToggle label="Show Date & Time" settingKey="showDateTime" />
        <OptionToggle label="Show Compass Heading" settingKey="showCompass" />
        <OptionToggle label="Show QR Code" settingKey="showQRCode" />
      </div>
    </div>
  );
}
