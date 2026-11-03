#!/usr/bin/env python3
"""
Offline Regional Address Database Builder for GPS Map Camera.
Builds an optimized SQLite database with an R-Tree index from OpenStreetMap (OSM) extracts
or city-centroid datasets for instant sub-millisecond offline reverse geocoding.
"""

import sqlite3
import sys
import os
import json
import csv

def create_database(db_path="offline_addresses.db"):
    if os.path.exists(db_path):
        os.remove(db_path)

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Create main address table
    cursor.execute("""
        CREATE TABLE addresses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            city TEXT,
            district TEXT,
            state TEXT,
            country TEXT,
            postcode TEXT,
            lat REAL,
            lon REAL
        );
    """)

    # Create spatial R-Tree index for ultra-fast spatial bounding-box lookups
    cursor.execute("""
        CREATE VIRTUAL TABLE addresses_index USING rtree(
            id,
            minX, maxX,
            minY, maxY
        );
    """)

    conn.commit()
    return conn

def insert_locations(conn, records):
    cursor = conn.cursor()
    cursor.execute("BEGIN TRANSACTION;")

    for rec in records:
        city = rec.get("city", "")
        district = rec.get("district", "")
        state = rec.get("state", "")
        country = rec.get("country", "")
        postcode = rec.get("postcode", "")
        lat = float(rec["lat"])
        lon = float(rec["lon"])

        cursor.execute("""
            INSERT INTO addresses (city, district, state, country, postcode, lat, lon)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """, (city, district, state, country, postcode, lat, lon))

        row_id = cursor.lastrowid

        # Insert into R-Tree index
        cursor.execute("""
            INSERT INTO addresses_index (id, minX, maxX, minY, maxY)
            VALUES (?, ?, ?, ?, ?);
        """, (row_id, lon, lon, lat, lat))

    conn.commit()

def test_nearest_lookup(conn, lat, lon, delta=0.05):
    """
    Simulates Android Room / SQLite query for offline reverse geocoding:
    finds nearest city/locality within a bounding box.
    """
    cursor = conn.cursor()
    cursor.execute("""
        SELECT a.city, a.district, a.state, a.country, a.lat, a.lon,
               ((a.lat - ?) * (a.lat - ?) + (a.lon - ?) * (a.lon - ?)) AS distance_sq
        FROM addresses a
        JOIN addresses_index idx ON a.id = idx.id
        WHERE idx.minX >= ? AND idx.maxX <= ?
          AND idx.minY >= ? AND idx.maxY <= ?
        ORDER BY distance_sq ASC
        LIMIT 1;
    """, (lat, lat, lon, lon, lon - delta, lon + delta, lat - delta, lat + delta))

    row = cursor.fetchone()
    return row

if __name__ == "__main__":
    db_file = "offline_addresses.db"
    print(f"Creating offline address database: {db_file}...")
    connection = create_database(db_file)

    sample_cities = [
        {"city": "Mumbai", "district": "Mumbai Suburban", "state": "Maharashtra", "country": "India", "postcode": "400001", "lat": 19.0760, "lon": 72.8777},
        {"city": "Delhi", "district": "New Delhi", "state": "Delhi", "country": "India", "postcode": "110001", "lat": 28.6139, "lon": 77.2090},
        {"city": "Bengaluru", "district": "Bengaluru Urban", "state": "Karnataka", "country": "India", "postcode": "560001", "lat": 12.9716, "lon": 77.5946},
        {"city": "New York", "district": "New York County", "state": "NY", "country": "United States", "postcode": "10001", "lat": 40.7128, "lon": -74.0060},
        {"city": "London", "district": "Greater London", "state": "England", "country": "United Kingdom", "postcode": "EC1A", "lat": 51.5074, "lon": -0.1278}
    ]

    insert_locations(connection, sample_cities)
    print(f"Inserted {len(sample_cities)} records.")

    # Test query near Mumbai
    result = test_nearest_lookup(connection, 19.0755, 72.8770)
    print(f"Test lookup at (19.0755, 72.8770) -> Nearest: {result}")
    connection.close()
    print("Done! Database can be packed into app's assets/offline_addresses.db.")
