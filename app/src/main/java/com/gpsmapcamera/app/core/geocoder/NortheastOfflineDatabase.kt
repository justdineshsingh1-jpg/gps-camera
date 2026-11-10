package com.gpsmapcamera.app.core.geocoder

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NortheastPlace(
    val name: String,
    val district: String,
    val state: String,
    val pincode: String,
    val latitude: Double,
    val longitude: Double
)

object NortheastOfflineDatabase {

    private val places = listOf(
        // --- ASSAM : Guwahati Metropolitan ---
        NortheastPlace("Dispur", "Kamrup Metropolitan", "Assam", "781006", 26.1420, 91.7898),
        NortheastPlace("Sarumotoria", "Kamrup Metropolitan", "Assam", "781036", 26.1472, 91.7888),
        NortheastPlace("Bhetapara", "Kamrup Metropolitan", "Assam", "781028", 26.1173, 91.7865),
        NortheastPlace("Hatigaon", "Kamrup Metropolitan", "Assam", "781038", 26.1345, 91.7891),
        NortheastPlace("Khanapara", "Kamrup Metropolitan", "Assam", "781022", 26.1264, 91.8219),
        NortheastPlace("Six Mile", "Kamrup Metropolitan", "Assam", "781022", 26.1369, 91.8082),
        NortheastPlace("Beltola", "Kamrup Metropolitan", "Assam", "781028", 26.1287, 91.7942),
        NortheastPlace("Panbazar", "Kamrup Metropolitan", "Assam", "781001", 26.1859, 91.7485),
        NortheastPlace("Chandmari", "Kamrup Metropolitan", "Assam", "781003", 26.1892, 91.7766),
        NortheastPlace("Paltan Bazaar", "Kamrup Metropolitan", "Assam", "781008", 26.1802, 91.7512),
        NortheastPlace("Ganeshguri", "Kamrup Metropolitan", "Assam", "781005", 26.1492, 91.7865),
        NortheastPlace("Jalukbari", "Kamrup Metropolitan", "Assam", "781014", 26.1458, 91.6627),
        NortheastPlace("Maligaon", "Kamrup Metropolitan", "Assam", "781011", 26.1601, 91.7012),
        NortheastPlace("Borjhar Airport", "Kamrup", "Assam", "781015", 26.1061, 91.5859),
        NortheastPlace("North Guwahati", "Kamrup", "Assam", "781030", 26.1966, 91.7214),
        NortheastPlace("Hajo", "Kamrup", "Assam", "781102", 26.2486, 91.5269),
        NortheastPlace("Mirza", "Kamrup", "Assam", "781125", 26.0886, 91.5222),
        NortheastPlace("Baihata Chariali", "Kamrup", "Assam", "781381", 26.3475, 91.7136),
        NortheastPlace("Rangia", "Kamrup", "Assam", "781354", 26.4719, 91.6289),

        // --- ASSAM : Central & Upper Assam ---
        NortheastPlace("Jorabat", "Kamrup / Ri-Bhoi", "Assam", "781023", 26.1032, 91.8974),
        NortheastPlace("Jagiroad", "Morigaon", "Assam", "782410", 26.1215, 92.2152),
        NortheastPlace("Morigaon", "Morigaon", "Assam", "782105", 26.2519, 92.3421),
        NortheastPlace("Nagaon", "Nagaon", "Assam", "782001", 26.3466, 92.6840),
        NortheastPlace("Raha", "Nagaon", "Assam", "782103", 26.2289, 92.5203),
        NortheastPlace("Kaliabor", "Nagaon", "Assam", "782137", 26.5492, 93.0232),
        NortheastPlace("Kohora, Kaziranga National Park", "Golaghat", "Assam", "785609", 26.5862, 93.4116),
        NortheastPlace("Bagori, Kaziranga", "Nagaon", "Assam", "782136", 26.5684, 93.3082),
        NortheastPlace("Bokakhat", "Golaghat", "Assam", "785612", 26.6190, 93.5937),
        NortheastPlace("Golaghat", "Golaghat", "Assam", "785621", 26.5239, 93.9634),
        NortheastPlace("Dergaon", "Golaghat", "Assam", "785614", 26.7022, 93.9745),
        NortheastPlace("Jorhat", "Jorhat", "Assam", "785001", 26.7509, 94.2037),
        NortheastPlace("Mariani", "Jorhat", "Assam", "785634", 26.6667, 94.3333),
        NortheastPlace("Teok", "Jorhat", "Assam", "785112", 26.8488, 94.4259),
        NortheastPlace("Majuli", "Majuli", "Assam", "785104", 26.9602, 94.2127),
        NortheastPlace("Sivasagar", "Sivasagar", "Assam", "785640", 26.9826, 94.6425),
        NortheastPlace("Nazira", "Sivasagar", "Assam", "785685", 26.9167, 94.7333),
        NortheastPlace("Sonari", "Charaideo", "Assam", "785690", 27.0167, 95.0333),
        NortheastPlace("Moranhat", "Charaideo", "Assam", "785670", 27.1884, 94.9287),
        NortheastPlace("Dibrugarh", "Dibrugarh", "Assam", "786001", 27.4728, 94.9120),
        NortheastPlace("Duliajan", "Dibrugarh", "Assam", "786602", 27.3592, 95.3182),
        NortheastPlace("Naharkatia", "Dibrugarh", "Assam", "786610", 27.2833, 95.3333),
        NortheastPlace("Tinsukia", "Tinsukia", "Assam", "786125", 27.4922, 95.3468),
        NortheastPlace("Digboi", "Tinsukia", "Assam", "786171", 27.3820, 95.6267),
        NortheastPlace("Margherita", "Tinsukia", "Assam", "786181", 27.2882, 95.6811),
        NortheastPlace("Ledo", "Tinsukia", "Assam", "786182", 27.3015, 95.7389),
        NortheastPlace("Doomdooma", "Tinsukia", "Assam", "786151", 27.5683, 95.5433),
        NortheastPlace("Sadiya / Dhola", "Tinsukia", "Assam", "786158", 27.8333, 95.6667),

        // --- ASSAM : North Bank & Hills ---
        NortheastPlace("Tezpur", "Sonitpur", "Assam", "784001", 26.6528, 92.7926),
        NortheastPlace("Dhekiajuli", "Sonitpur", "Assam", "784110", 26.7000, 92.5000),
        NortheastPlace("Biswanath Chariali", "Biswanath", "Assam", "784176", 26.7333, 93.1500),
        NortheastPlace("Gohpur", "Biswanath", "Assam", "784168", 26.8833, 93.6333),
        NortheastPlace("North Lakhimpur", "Lakhimpur", "Assam", "787001", 27.2344, 94.1038),
        NortheastPlace("Bihpuria", "Lakhimpur", "Assam", "784161", 27.0333, 93.9000),
        NortheastPlace("Dhemaji", "Dhemaji", "Assam", "787057", 27.4816, 94.5772),
        NortheastPlace("Silapathar", "Dhemaji", "Assam", "787059", 27.5889, 94.7214),
        NortheastPlace("Jonai", "Dhemaji", "Assam", "787060", 27.8333, 95.1667),
        NortheastPlace("Mangaldai", "Darrang", "Assam", "784125", 26.4358, 92.0305),
        NortheastPlace("Udalguri", "Udalguri", "Assam", "784509", 26.7456, 92.0967),
        NortheastPlace("Nalbari", "Nalbari", "Assam", "781335", 26.4447, 91.4391),
        NortheastPlace("Barpeta", "Barpeta", "Assam", "781301", 26.3214, 91.0069),
        NortheastPlace("Barpeta Road", "Barpeta", "Assam", "781315", 26.5000, 90.9667),
        NortheastPlace("Bongaigaon", "Bongaigaon", "Assam", "783380", 26.5024, 90.5532),
        NortheastPlace("Kokrajhar", "Kokrajhar", "Assam", "783370", 26.4014, 90.2714),
        NortheastPlace("Goalpara", "Goalpara", "Assam", "783101", 26.1724, 90.6247),
        NortheastPlace("Dhubri", "Dhubri", "Assam", "783301", 26.0207, 89.9744),

        // --- ASSAM : Hills & Barak Valley ---
        NortheastPlace("Diphu", "Karbi Anglong", "Assam", "782460", 25.8458, 93.4339),
        NortheastPlace("Bokajan", "Karbi Anglong", "Assam", "782480", 26.0167, 93.7833),
        NortheastPlace("Haflong Hill Station", "Dima Hasao", "Assam", "788819", 25.1764, 93.0181),
        NortheastPlace("Jatinga", "Dima Hasao", "Assam", "788819", 25.1232, 93.0421),
        NortheastPlace("Umrangso", "Dima Hasao", "Assam", "788931", 25.5167, 92.7833),
        NortheastPlace("Silchar", "Cachar", "Assam", "788001", 24.8333, 92.7789),
        NortheastPlace("Karimganj", "Karimganj", "Assam", "788710", 24.8649, 92.3593),
        NortheastPlace("Hailakandi", "Hailakandi", "Assam", "788151", 24.6833, 92.5667),

        // --- MEGHALAYA : Khasi, Jaintia & Garo Hills ---
        NortheastPlace("Shillong Police Bazar", "East Khasi Hills", "Meghalaya", "793001", 25.5788, 91.8831),
        NortheastPlace("Laitumkhrah, Shillong", "East Khasi Hills", "Meghalaya", "793003", 25.5701, 91.8967),
        NortheastPlace("Mawlai, Shillong", "East Khasi Hills", "Meghalaya", "793008", 25.5969, 91.8745),
        NortheastPlace("Shillong Peak", "East Khasi Hills", "Meghalaya", "793005", 25.5348, 91.8542),
        NortheastPlace("Elephant Falls", "East Khasi Hills", "Meghalaya", "793009", 25.5369, 91.8239),
        NortheastPlace("Umiam Lake / Barapani", "Ri-Bhoi", "Meghalaya", "793103", 25.6601, 91.9056),
        NortheastPlace("Umsning", "Ri-Bhoi", "Meghalaya", "793105", 25.7500, 91.8833),
        NortheastPlace("Nongpoh", "Ri-Bhoi", "Meghalaya", "793102", 25.9038, 91.8797),
        NortheastPlace("Byrnihat", "Ri-Bhoi", "Meghalaya", "793101", 26.0592, 91.8654),
        NortheastPlace("Cherrapunji (Sohra)", "East Khasi Hills", "Meghalaya", "793108", 25.2702, 91.7323),
        NortheastPlace("Nohkalikai Falls", "East Khasi Hills", "Meghalaya", "793108", 25.2757, 91.6845),
        NortheastPlace("Mawsynram", "East Khasi Hills", "Meghalaya", "793113", 25.2974, 91.5839),
        NortheastPlace("Dawki", "West Jaintia Hills", "Meghalaya", "793109", 25.1873, 92.0163),
        NortheastPlace("Mawlynnong", "East Khasi Hills", "Meghalaya", "793110", 25.2017, 91.9167),
        NortheastPlace("Laitlum Canyons", "East Khasi Hills", "Meghalaya", "793015", 25.4745, 91.9082),
        NortheastPlace("Jowai", "West Jaintia Hills", "Meghalaya", "793150", 25.4489, 92.2036),
        NortheastPlace("Khliehriat", "East Jaintia Hills", "Meghalaya", "793200", 25.3524, 92.3684),
        NortheastPlace("Nongstoin", "West Khasi Hills", "Meghalaya", "793119", 25.5208, 91.2694),
        NortheastPlace("Mairang", "Eastern West Khasi Hills", "Meghalaya", "793120", 25.5604, 91.6369),
        NortheastPlace("Tura", "West Garo Hills", "Meghalaya", "794001", 25.5144, 90.2032),
        NortheastPlace("Williamnagar", "East Garo Hills", "Meghalaya", "794111", 25.5978, 90.6172),
        NortheastPlace("Baghmara", "South Garo Hills", "Meghalaya", "794102", 25.1972, 90.6389),

        // --- ARUNACHAL PRADESH : Mountain Valleys & Passes ---
        NortheastPlace("Itanagar", "Papum Pare", "Arunachal Pradesh", "791111", 27.0844, 93.6053),
        NortheastPlace("Naharlagun", "Papum Pare", "Arunachal Pradesh", "791110", 27.1064, 93.6936),
        NortheastPlace("Banderdewa", "Papum Pare", "Arunachal Pradesh", "791123", 27.1422, 93.8189),
        NortheastPlace("Tawang", "Tawang", "Arunachal Pradesh", "790104", 27.5861, 91.8594),
        NortheastPlace("Sela Pass", "Tawang", "Arunachal Pradesh", "790105", 27.5042, 92.1039),
        NortheastPlace("Dirang", "West Kameng", "Arunachal Pradesh", "790101", 27.3592, 92.2412),
        NortheastPlace("Bomdila", "West Kameng", "Arunachal Pradesh", "790001", 27.2645, 92.4231),
        NortheastPlace("Bhalukpong", "West Kameng", "Arunachal Pradesh", "790114", 27.0145, 92.6469),
        NortheastPlace("Ziro Valley", "Lower Subansiri", "Arunachal Pradesh", "791120", 27.5962, 93.8295),
        NortheastPlace("Daporijo", "Upper Subansiri", "Arunachal Pradesh", "791122", 27.9892, 94.2201),
        NortheastPlace("Aalo (Along)", "West Siang", "Arunachal Pradesh", "791001", 28.1692, 94.8028),
        NortheastPlace("Pasighat", "East Siang", "Arunachal Pradesh", "791102", 28.0665, 95.3268),
        NortheastPlace("Roing", "Lower Dibang Valley", "Arunachal Pradesh", "792110", 28.1408, 95.8364),
        NortheastPlace("Tezu", "Lohit", "Arunachal Pradesh", "792001", 27.9192, 96.1664),
        NortheastPlace("Namsai", "Namsai", "Arunachal Pradesh", "792103", 27.6711, 95.8672),
        NortheastPlace("Changlang", "Changlang", "Arunachal Pradesh", "792120", 27.1264, 95.7333),
        NortheastPlace("Khonsa", "Tirap", "Arunachal Pradesh", "792130", 27.0175, 95.5089),

        // --- NAGALAND : Naga Hills ---
        NortheastPlace("Kohima", "Kohima", "Nagaland", "797001", 25.6751, 94.1086),
        NortheastPlace("Kisama Heritage Village", "Kohima", "Nagaland", "797005", 25.6083, 94.1167),
        NortheastPlace("Dzukou Valley", "Kohima", "Nagaland", "797002", 25.5601, 94.0672),
        NortheastPlace("Dimapur", "Dimapur", "Nagaland", "797112", 25.9095, 93.7266),
        NortheastPlace("Chümoukedima", "Chümoukedima", "Nagaland", "797103", 25.8014, 93.7712),
        NortheastPlace("Mokokchung", "Mokokchung", "Nagaland", "798601", 26.3245, 94.5186),
        NortheastPlace("Wokha", "Wokha", "Nagaland", "797111", 26.0989, 94.2612),
        NortheastPlace("Zunheboto", "Zunheboto", "Nagaland", "798620", 25.9667, 94.5167),
        NortheastPlace("Phek", "Phek", "Nagaland", "797107", 25.6833, 94.4833),
        NortheastPlace("Pfütsero", "Phek", "Nagaland", "797107", 25.5683, 94.3167),
        NortheastPlace("Mon", "Mon", "Nagaland", "798621", 26.7456, 95.0617),
        NortheastPlace("Tuensang", "Tuensang", "Nagaland", "798612", 26.2833, 94.8333),

        // --- MANIPUR ---
        NortheastPlace("Imphal", "Imphal West", "Manipur", "795001", 24.8170, 93.9368),
        NortheastPlace("Kangla, Imphal", "Imphal West", "Manipur", "795001", 24.8089, 93.9421),
        NortheastPlace("Moirang (Loktak Lake)", "Bishnupur", "Manipur", "795133", 24.4989, 93.7712),
        NortheastPlace("Bishnupur", "Bishnupur", "Manipur", "795126", 24.6322, 93.7612),
        NortheastPlace("Thoubal", "Thoubal", "Manipur", "795138", 24.6369, 93.9964),
        NortheastPlace("Churachandpur (Lamka)", "Churachandpur", "Manipur", "795128", 24.3333, 93.6833),
        NortheastPlace("Ukhrul", "Ukhrul", "Manipur", "795142", 25.1167, 94.3667),
        NortheastPlace("Senapati", "Senapati", "Manipur", "795106", 25.2667, 94.0167),
        NortheastPlace("Moreh Border", "Tengnoupal", "Manipur", "795131", 24.2503, 94.3056),

        // --- MIZORAM ---
        NortheastPlace("Aizawl", "Aizawl", "Mizoram", "796001", 23.7271, 92.7176),
        NortheastPlace("Lengpui Airport", "Mamit", "Mizoram", "796422", 23.8408, 92.6247),
        NortheastPlace("Lunglei", "Lunglei", "Mizoram", "796701", 22.8833, 92.7333),
        NortheastPlace("Champhai", "Champhai", "Mizoram", "796321", 23.4756, 93.3283),
        NortheastPlace("Kolasib", "Kolasib", "Mizoram", "796081", 24.2244, 92.6789),
        NortheastPlace("Serchhip", "Serchhip", "Mizoram", "796181", 23.3411, 92.8503),

        // --- TRIPURA ---
        NortheastPlace("Agartala", "West Tripura", "Tripura", "799001", 23.8315, 91.2868),
        NortheastPlace("Udaipur", "Gomati", "Tripura", "799120", 23.5333, 91.4833),
        NortheastPlace("Dharmanagar", "North Tripura", "Tripura", "799250", 24.3739, 92.1645),
        NortheastPlace("Kailashahar", "Unakoti", "Tripura", "799277", 24.3292, 92.0089),
        NortheastPlace("Jampui Hills", "North Tripura", "Tripura", "799269", 23.9667, 92.2667),

        // --- SIKKIM ---
        NortheastPlace("Gangtok", "East Sikkim", "Sikkim", "737101", 27.3389, 88.6065),
        NortheastPlace("MG Marg, Gangtok", "East Sikkim", "Sikkim", "737101", 27.3314, 88.6138),
        NortheastPlace("Namchi", "South Sikkim", "Sikkim", "737126", 27.1667, 88.3500),
        NortheastPlace("Pelling", "West Sikkim", "Sikkim", "737113", 27.3000, 88.2333),
        NortheastPlace("Mangan", "North Sikkim", "Sikkim", "737116", 27.5000, 88.5333),
        NortheastPlace("Lachung", "North Sikkim", "Sikkim", "737120", 27.6892, 88.7456),

        // --- NORTH BENGAL / FOOTHILLS CORRIDOR ---
        NortheastPlace("Siliguri", "Darjeeling", "West Bengal", "734001", 26.7271, 88.3953),
        NortheastPlace("Darjeeling", "Darjeeling", "West Bengal", "734101", 27.0410, 88.2663),
        NortheastPlace("Kalimpong", "Kalimpong", "West Bengal", "734301", 27.0667, 88.4667),
        NortheastPlace("Alipurduar", "Alipurduar", "West Bengal", "736121", 26.4919, 89.5272),
        NortheastPlace("Cooch Behar", "Cooch Behar", "West Bengal", "736101", 26.3239, 89.4510)
    )

    /**
     * Resolves GPS coordinates strictly offline using the embedded Northeast geospatial dataset.
     * Guaranteed to work with ZERO internet connectivity (hills, mountains, remote roads).
     */
    fun resolveOffline(latitude: Double, longitude: Double): ResolvedAddress? {
        if (latitude == 0.0 && longitude == 0.0) return null

        var nearestPlace: NortheastPlace? = null
        var minDistanceKm = Double.MAX_VALUE

        for (place in places) {
            val dist = haversineKm(latitude, longitude, place.latitude, place.longitude)
            if (dist < minDistanceKm) {
                minDistanceKm = dist
                nearestPlace = place
            }
        }

        if (nearestPlace == null) return null

        // If within 150 km of any known regional place, provide rich offline address
        return if (minDistanceKm <= 150.0) {
            val formatted = if (minDistanceKm <= 1.2) {
                "${nearestPlace.name}, ${nearestPlace.district}, ${nearestPlace.state} ${nearestPlace.pincode}, India"
            } else {
                val dir = getCardinalDirection(nearestPlace.latitude, nearestPlace.longitude, latitude, longitude)
                String.format(
                    Locale.US,
                    "%.1f km %s of %s, %s, %s %s, India",
                    minDistanceKm,
                    dir,
                    nearestPlace.name,
                    nearestPlace.district,
                    nearestPlace.state,
                    nearestPlace.pincode
                )
            }

            ResolvedAddress(
                formattedAddress = formatted,
                city = nearestPlace.name.substringBefore(",").substringBefore("/").trim(),
                state = nearestPlace.state,
                country = "India",
                isFromCache = true
            )
        } else {
            null
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun getCardinalDirection(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): String {
        val dLon = Math.toRadians(toLon - fromLon)
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        var brng = Math.toDegrees(atan2(y, x))
        brng = (brng + 360) % 360

        return when (((brng + 22.5) / 45).toInt() % 8) {
            0 -> "North"
            1 -> "NE"
            2 -> "East"
            3 -> "SE"
            4 -> "South"
            5 -> "SW"
            6 -> "West"
            7 -> "NW"
            else -> "Near"
        }
    }
}
