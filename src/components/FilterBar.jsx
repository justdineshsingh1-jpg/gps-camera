import React from 'react';

const filters = [
  { name: 'Normal', value: 'none' },
  { name: 'Mono', value: 'grayscale(100%)' },
  { name: 'Sepia', value: 'sepia(100%)' },
  { name: 'Vintage', value: 'sepia(50%) contrast(120%) hue-rotate(-10deg)' },
  { name: 'Cool', value: 'hue-rotate(180deg)' },
  { name: 'Invert', value: 'invert(100%)' },
];

export default function FilterBar({ activeFilter, setActiveFilter }) {
  return (
    <div className="filter-bar">
      {filters.map(f => (
        <button
          key={f.name}
          className={`filter-btn ${activeFilter === f.value ? 'active' : ''}`}
          onClick={() => setActiveFilter(f.value)}
        >
          {f.name}
        </button>
      ))}
    </div>
  );
}
