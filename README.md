# Production-Ready GPS Map Camera (Android)

Native Android GPS Map Camera built with **Kotlin, Jetpack Compose, CameraX, and Media3**. Operates seamlessly in both **Online and 100% Offline (Airplane) Mode**.

---

## Complete Feature Matrix

| Feature | Online Mode | Offline (Airplane) Mode |
|---|---|---|
| **GPS Coordinates (Lat / Long)** | Decimal / DMS toggle | Decimal / DMS toggle |
| **Google Plus Code (Open Location Code)** | Computed on-device (`7JVW52GR+PQ`) | Computed on-device (`7JVW52GR+PQ`) |
| **Address Resolution** | Live reverse Geocoder $\rightarrow$ Room DB cache | Room cache within 100m $\rightarrow$ Coordinates + Plus Code |
| **Map Tile Thumbnail on Stamp** | **Shown** with center red pin (OSM/Static Tile) | **Omitted completely** (Stamp reflows to 100% card width) |
| **On-Device QR Code** | Generated via ZXing offline | Generated via ZXing offline |
| **Photo Capture** | Full-res Canvas pixel stamping + EXIF metadata | Full-res Canvas pixel stamping + EXIF metadata |
| **Video Recording** | Media3 `Transformer` / `OverlayEffect` burned per frame | Media3 `Transformer` / `OverlayEffect` burned per frame |
| **In-App Gallery & Viewer** | MediaStore Scoped Storage viewer & sharing | MediaStore Scoped Storage viewer & sharing |

---

## Architectural Highlights

### 1. Connectivity & Dynamic Layout Reflow
- **Real-Time Detection:** Emits connectivity state changes instantly using `ConnectivityManager.NetworkCallback`.
- **Offline Layout Reflow:** In offline mode (or forced offline), **the map tile thumbnail is completely omitted**. The text block seamlessly expands to occupy the **full 100% width** of the card, leaving **zero empty space, blank gap, or broken placeholder**.
- **Auto-Switching:** When internet connectivity returns, the map tile thumbnail returns automatically on the subsequent photo/video frame.
- **Manual Mode Toggle:** Switch between `AUTO` and `FORCE_OFFLINE` with a single tap on the top status pill.

### 2. High-Precision Offline GPS Engine
- Uses `FusedLocationProviderClient` with a 1-second interval, with automatic low-level fallback to `LocationManager` (`GPS_PROVIDER`) when Play Services is absent or non-functional.
- **On-Device Plus Codes:** Implements Google's Open Location Code algorithm in pure Kotlin (`OpenLocationCode.kt`), calculating codes like `7JVW52GR+PQ` without any network calls.
- **Hardware Compass:** Uses `Sensor.TYPE_ROTATION_VECTOR` / Magnetometer + Accelerometer fallback to calculate real-time heading degrees and cardinal direction (e.g. `142° SE`).

### 3. Layered Geocoding & Room Database
- **Layer 1 (Online):** Native Android `Geocoder` with a strict **2-second timeout** so capture is never blocked.
- **Layer 2 (Offline Cache):** Stores successfully resolved addresses in a local Room database (`cached_addresses`). If offline, queries candidates within ~100m using spatial bounding box + Haversine distance.
- **Layer 3 (Fallback):** Coordinates + Plus Code (`"Plus Code: 7JVW52GR+PQ"`), guaranteeing the address line is **never blank**.

### 4. Media3 Video Recording & Hardware-Accelerated Stamping
- Records high-definition video with stereo audio via CameraX `VideoCapture<Recorder>`.
- Media3 `Transformer` and OpenGL `OverlayEffect` (`BitmapOverlay`) composite the live GPS stamp onto every frame of the final MP4 with hardware H.264/H.265 encoding, holding 30 fps on mid-range devices.
- Supports pause, resume, and duration tracking.

### 5. Scoped Storage Gallery & EXIF Preservation
- Stores photos in `Pictures/GPSMapCamera` and videos in `Movies/GPSMapCamera` via Android's `MediaStore`.
- Injects standard GPS coordinates, altitude, UTC timestamps, and Plus Codes into JPEG headers using `androidx.exifinterface`.
- Built-in Gallery with a 3-column grid, fullscreen viewer, video playback, and standard Android share intent (`Intent.ACTION_SEND`).

---

## Project Structure

```
gps-map-camera-android/
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── java/com/gpsmapcamera/app/
│   │   │       ├── GpsMapCameraApp.kt               # Application class with @HiltAndroidApp
│   │   │       ├── MainActivity.kt                  # Activity & runtime permissions & navigation
│   │   │       ├── di/
│   │   │       │   └── AppModule.kt                 # Hilt dependency injection & Room DB provider
│   │   │       ├── core/
│   │   │       │   ├── camera/
│   │   │       │   │   └── CameraController.kt      # CameraX preview, photo capture, video recording
│   │   │       │   ├── connectivity/
│   │   │       │   │   ├── ConnectivityMode.kt      # AUTO vs FORCE_OFFLINE
│   │   │       │   │   ├── NetworkObserver.kt       # NetworkCallback Flow
│   │   │       │   │   └── NetworkStatus.kt
│   │   │       │   ├── data/
│   │   │       │   │   ├── GalleryRepository.kt     # MediaStore scoped storage query
│   │   │       │   │   ├── MediaStorageManager.kt   # Scoped storage (Pictures & Movies)
│   │   │       │   │   └── local/
│   │   │       │   │       ├── AddressDao.kt        # Room DAO for cached addresses
│   │   │       │   │       ├── AppDatabase.kt       # Room Database
│   │   │       │   │       └── CachedAddress.kt     # Room entity with lat/lon indices
│   │   │       │   ├── geocoder/
│   │   │       │   │   └── AddressRepository.kt     # Layered geocoder (Online -> Room -> Plus Code)
│   │   │       │   ├── location/
│   │   │       │   │   ├── CompassSensor.kt         # SensorManager orientation
│   │   │       │   │   ├── GpsData.kt               # Location data model
│   │   │       │   │   ├── LocationFormattingUtils.kt # DMS, Speed, Alt, GMT Date formatter
│   │   │       │   │   ├── LocationTracker.kt       # Fused + LocationManager fallback
│   │   │       │   │   └── OpenLocationCode.kt      # On-device Plus Code encoder
│   │   │       │   ├── overlay/
│   │   │       │   │   ├── ExifWriter.kt            # ExifInterface GPS injector
│   │   │       │   │   ├── MapTileManager.kt        # Online map tile fetcher with 2s timeout & LruCache
│   │   │       │   │   ├── PhotoStampRenderer.kt    # Full-res Canvas photo compositor
│   │   │       │   │   └── QrCodeGenerator.kt       # ZXing offline QR bitmap generator
│   │   │       │   └── video/
│   │   │       │       └── VideoStampTransformer.kt # Media3 Transformer & OpenGL OverlayEffect
│   │   │       └── ui/
│   │   │           ├── camera/
│   │   │           │   ├── CameraScreen.kt          # Compose Camera preview, Photo/Video modes, UI
│   │   │           │   └── CameraViewModel.kt       # StateFlow UI state holder & recording pipeline
│   │   │           ├── components/
│   │   │           │   ├── LiveStampOverlay.kt      # WYSIWYG live stamp overlay
│   │   │           │   └── StatusIndicators.kt      # Online/Offline pill & GPS lock pill
│   │   │           └── gallery/
│   │   │               └── GalleryScreen.kt         # Scoped storage gallery, viewer & share
│   │   └── test/
│   │       └── java/com/gpsmapcamera/app/
│   │           └── GpsAppUnitTests.kt               # Plus Code & formatting unit tests
├── scripts/
│   └── build_offline_osm_db.py                      # Offline address builder from OSM
├── gradle/
│   └── libs.versions.toml                           # Modern Version Catalog
├── build.gradle.kts                                 # Root Gradle configuration
├── settings.gradle.kts                              # Project settings
└── gradle.properties                                # Build properties
```

---

## Build Instructions

### Requirements
- **JDK:** Java 17 or Java 21
- **Android Studio:** Hedgehog / Iguana / Jellyfish / Ladybug or newer
- **Target SDK:** Android 15 (API 35)
- **Minimum SDK:** Android 8.0 (API 26)

### Building via Android Studio
1. Open Android Studio -> **File** -> **Open** -> select `c:\Users\MIS\Documents\Action Report\gps-map-camera-android`.
2. Allow Gradle sync to complete using the provided `gradle/libs.versions.toml`.
3. Select build variant **debug** or **release**.
4. Connect an Android device (or run an emulator) and click **Run (Shift + F10)**.

### Building via Command Line (Gradle)
```bash
cd gps-map-camera-android
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Verification & Testing Checklist

| Test Scenario | Steps to Execute | Expected Behavior |
|---|---|---|
| **1. Airplane Mode (Offline Photo)** | 1. Enable Airplane mode on phone.<br>2. Open app.<br>3. Observe UI and click photo. | Mode indicator displays `OFFLINE`. **Map tile thumbnail is not shown**. Stamp text block expands to full card width. Address resolves from Room cache or coordinates + Plus Code. Permanent stamp burned into image. |
| **2. Airplane Mode (Offline Video)** | 1. Keep Airplane mode on.<br>2. Switch to VIDEO mode.<br>3. Record 5 seconds and stop. | Live recording timer functions with audio. Media3 Transformer burns offline stamp (full width, no map tile) into every video frame. Saved to `Movies/GPSMapCamera`. |
| **3. Online Mode** | 1. Turn on Wi-Fi or Mobile Data.<br>2. Open app.<br>3. Observe UI and click photo/video. | Mode indicator turns green (`ONLINE`). Map thumbnail with red pin is visible. Stamp is burned with live map tile. |
| **4. Mid-Session Mode Switching** | 1. Start in Airplane mode.<br>2. Disable Airplane mode while camera is open.<br>3. Wait 2 seconds. | Status indicator switches from `OFFLINE` to `ONLINE` instantly. Stamp layout reflows from full-width to map-included layout smoothly. |
| **5. Force Offline Override** | 1. Connect to Wi-Fi.<br>2. Tap the `ONLINE` pill at the top. | Mode changes to `OFFLINE (FORCED)`. App behaves strictly in offline mode without map requests. |
| **6. Gallery & Scoped Storage** | 1. Tap the gallery thumbnail button in camera screen.<br>2. Tap any thumbnail to view fullscreen.<br>3. Tap Share or Play (for video). | Fullscreen viewer shows stamped media. Share intent opens Android share sheet. Video plays smoothly with audio and burned-in stamp. |
