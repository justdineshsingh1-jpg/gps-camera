package com.gpsmapcamera.app.core.overlay

import com.gpsmapcamera.app.core.location.GpsData

/** Pure helpers for the live (WYSIWYG) preview stamp; kept Android-free so they are unit-testable. */
object StampPreview {

    /**
     * Only the fields the stamp prints, with the time cut to the minute (the stamp shows hh:mm),
     * so the preview re-renders when something visible changes and not on every sensor tick.
     */
    fun displayData(gps: GpsData): GpsData = GpsData(
        latitude = gps.latitude,
        longitude = gps.longitude,
        plusCode = gps.plusCode,
        timestamp = gps.timestamp - (gps.timestamp % 60_000L),
        isLocked = gps.isLocked,
        addressLine = gps.addressLine,
        city = gps.city,
        state = gps.state,
        country = gps.country
    )
}
