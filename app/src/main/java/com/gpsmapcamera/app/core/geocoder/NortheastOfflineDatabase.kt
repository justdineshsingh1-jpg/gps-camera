package com.gpsmapcamera.app.core.geocoder

import com.gpsmapcamera.app.core.location.OpenLocationCode
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NortheastPlace(
    val name: String,
    val city: String,
    val district: String,
    val state: String,
    val pincode: String,
    val latitude: Double,
    val longitude: Double
)

object NortheastOfflineDatabase {

    private val places = listOf(
        // =========================================================================
        // ASSAM : Guwahati Metropolitan (Dense Locality Nodes)
        // =========================================================================
        NortheastPlace("Kailashpur Rd, Jyotinagar, Noonmati", "Guwahati", "Kamrup Metropolitan", "Assam", "781021", 26.1926, 91.7928),
        NortheastPlace("Noonmati Sector 1", "Guwahati", "Kamrup Metropolitan", "Assam", "781020", 26.1985, 91.8021),
        NortheastPlace("Jyotinagar, Bamunimaidam", "Guwahati", "Kamrup Metropolitan", "Assam", "781021", 26.1865, 91.7892),
        NortheastPlace("Narengi, Forest Gate", "Guwahati", "Kamrup Metropolitan", "Assam", "781026", 26.1782, 91.8245),
        NortheastPlace("Chandmari, Guwahati Club", "Guwahati", "Kamrup Metropolitan", "Assam", "781003", 26.1892, 91.7766),
        NortheastPlace("Silpukhuri, Nabagraha", "Guwahati", "Kamrup Metropolitan", "Assam", "781003", 26.1881, 91.7645),
        NortheastPlace("Uzanbazar, Latasil", "Guwahati", "Kamrup Metropolitan", "Assam", "781001", 26.1915, 91.7523),
        NortheastPlace("Panbazar, Dighalipukhuri", "Guwahati", "Kamrup Metropolitan", "Assam", "781001", 26.1859, 91.7485),
        NortheastPlace("Paltan Bazaar, Railway Station", "Guwahati", "Kamrup Metropolitan", "Assam", "781008", 26.1802, 91.7512),
        NortheastPlace("Ulubari, GS Road", "Guwahati", "Kamrup Metropolitan", "Assam", "781007", 26.1725, 91.7582),
        NortheastPlace("Bhangagarh, GMCH Hospital", "Guwahati", "Kamrup Metropolitan", "Assam", "781005", 26.1602, 91.7698),
        NortheastPlace("Christian Basti, GS Road", "Guwahati", "Kamrup Metropolitan", "Assam", "781005", 26.1555, 91.7782),
        NortheastPlace("Ganeshguri, Dispur Flyover", "Guwahati", "Kamrup Metropolitan", "Assam", "781005", 26.1492, 91.7865),
        NortheastPlace("Dispur, Assam Secretariat", "Guwahati", "Kamrup Metropolitan", "Assam", "781006", 26.1420, 91.7898),
        NortheastPlace("Sarumotoria, Hengrabari", "Guwahati", "Kamrup Metropolitan", "Assam", "781036", 26.1472, 91.7888),
        NortheastPlace("Supermarket, Last Gate", "Guwahati", "Kamrup Metropolitan", "Assam", "781006", 26.1382, 91.7945),
        NortheastPlace("Six Mile, VIP Road", "Guwahati", "Kamrup Metropolitan", "Assam", "781022", 26.1369, 91.8082),
        NortheastPlace("Khanapara", "Guwahati", "Kamrup Metropolitan", "Assam", "781022", 26.1264, 91.8219),
        NortheastPlace("Beltola", "Guwahati", "Kamrup Metropolitan", "Assam", "781028", 26.1287, 91.7942),
        NortheastPlace("Bhetapara", "Guwahati", "Kamrup Metropolitan", "Assam", "781028", 26.1173, 91.7865),
        NortheastPlace("Hatigaon", "Guwahati", "Kamrup Metropolitan", "Assam", "781038", 26.1345, 91.7891),
        NortheastPlace("Lalmati, Lokhra", "Guwahati", "Kamrup Metropolitan", "Assam", "781029", 26.1154, 91.7521),
        NortheastPlace("ISBT Betkuchi, Garchuk", "Guwahati", "Kamrup Metropolitan", "Assam", "781035", 26.1245, 91.7182),
        NortheastPlace("Maligaon, NF Railway HQ", "Guwahati", "Kamrup Metropolitan", "Assam", "781011", 26.1601, 91.7012),
        NortheastPlace("Kamakhya Temple, Nilachal Hill", "Guwahati", "Kamrup Metropolitan", "Assam", "781010", 26.1664, 91.7056),
        NortheastPlace("Jalukbari, Gauhati University", "Guwahati", "Kamrup Metropolitan", "Assam", "781014", 26.1458, 91.6627),
        NortheastPlace("Borjhar, LGBI Airport", "Guwahati", "Kamrup", "Assam", "781015", 26.1061, 91.5859),
        NortheastPlace("North Guwahati, IIT Guwahati", "Guwahati", "Kamrup", "Assam", "781039", 26.1966, 91.7214),

        // =========================================================================
        // ASSAM : Kamrup Rural & Lower Assam Districts
        // =========================================================================
        NortheastPlace("Hajo", "Hajo", "Kamrup", "Assam", "781102", 26.2486, 91.5269),
        NortheastPlace("Mirza, Palasbari", "Mirza", "Kamrup", "Assam", "781125", 26.0886, 91.5222),
        NortheastPlace("Chaygaon, Boko", "Chaygaon", "Kamrup", "Assam", "781124", 26.0452, 91.2421),
        NortheastPlace("Baihata Chariali", "Baihata", "Kamrup", "Assam", "781381", 26.3475, 91.7136),
        NortheastPlace("Rangia Junction", "Rangia", "Kamrup", "Assam", "781354", 26.4719, 91.6289),
        NortheastPlace("Nalbari", "Nalbari", "Nalbari", "Assam", "781335", 26.4447, 91.4391),
        NortheastPlace("Tihu", "Tihu", "Nalbari", "Assam", "781371", 26.4952, 91.1782),
        NortheastPlace("Barpeta", "Barpeta", "Barpeta", "Assam", "781301", 26.3214, 91.0069),
        NortheastPlace("Barpeta Road", "Barpeta Road", "Barpeta", "Assam", "781315", 26.5000, 90.9667),
        NortheastPlace("Pathsala, Bajali", "Pathsala", "Bajali", "Assam", "781325", 26.4989, 91.1788),
        NortheastPlace("Bongaigaon", "Bongaigaon", "Bongaigaon", "Assam", "783380", 26.5024, 90.5532),
        NortheastPlace("Abhayapuri", "Abhayapuri", "Bongaigaon", "Assam", "783384", 26.3333, 90.6667),
        NortheastPlace("Kokrajhar", "Kokrajhar", "Kokrajhar", "Assam", "783370", 26.4014, 90.2714),
        NortheastPlace("Gossaigaon", "Gossaigaon", "Kokrajhar", "Assam", "783360", 26.4500, 89.9667),
        NortheastPlace("Kajalgaon", "Kajalgaon", "Chirang", "Assam", "783385", 26.5167, 90.5333),
        NortheastPlace("Mushalpur", "Mushalpur", "Baksa", "Assam", "781372", 26.5833, 91.4167),
        NortheastPlace("Tamulpur", "Tamulpur", "Tamulpur", "Assam", "781367", 26.6333, 91.5667),
        NortheastPlace("Goalpara", "Goalpara", "Goalpara", "Assam", "783101", 26.1724, 90.6247),
        NortheastPlace("Dudhnoi", "Dudhnoi", "Goalpara", "Assam", "783124", 25.9833, 90.7333),
        NortheastPlace("Dhubri", "Dhubri", "Dhubri", "Assam", "783301", 26.0207, 89.9744),
        NortheastPlace("Gauripur", "Gauripur", "Dhubri", "Assam", "783331", 26.0833, 89.9667),
        NortheastPlace("Bilasipara", "Bilasipara", "Dhubri", "Assam", "783348", 26.2333, 90.2333),
        NortheastPlace("Hatsingimari", "Hatsingimari", "South Salmara", "Assam", "783135", 25.6833, 89.8833),

        // =========================================================================
        // ASSAM : Central & North Bank Districts
        // =========================================================================
        NortheastPlace("Jorabat", "Jorabat", "Kamrup Metropolitan", "Assam", "781023", 26.1032, 91.8974),
        NortheastPlace("Jagiroad", "Jagiroad", "Morigaon", "Assam", "782410", 26.1215, 92.2152),
        NortheastPlace("Morigaon", "Morigaon", "Morigaon", "Assam", "782105", 26.2519, 92.3421),
        NortheastPlace("Nagaon", "Nagaon", "Nagaon", "Assam", "782001", 26.3466, 92.6840),
        NortheastPlace("Raha", "Raha", "Nagaon", "Assam", "782103", 26.2289, 92.5203),
        NortheastPlace("Hojai", "Hojai", "Hojai", "Assam", "782435", 26.0024, 92.8621),
        NortheastPlace("Lanka", "Lanka", "Hojai", "Assam", "782446", 25.9167, 92.9500),
        NortheastPlace("Kaliabor", "Kaliabor", "Nagaon", "Assam", "782137", 26.5492, 93.0232),
        NortheastPlace("Mangaldai", "Mangaldai", "Darrang", "Assam", "784125", 26.4358, 92.0305),
        NortheastPlace("Sipajhar", "Sipajhar", "Darrang", "Assam", "784145", 26.4167, 91.8833),
        NortheastPlace("Udalguri", "Udalguri", "Udalguri", "Assam", "784509", 26.7456, 92.0967),
        NortheastPlace("Tangla", "Tangla", "Udalguri", "Assam", "784521", 26.6500, 91.9167),
        NortheastPlace("Tezpur", "Tezpur", "Sonitpur", "Assam", "784001", 26.6528, 92.7926),
        NortheastPlace("Dhekiajuli", "Dhekiajuli", "Sonitpur", "Assam", "784110", 26.7000, 92.5000),
        NortheastPlace("Biswanath Chariali", "Biswanath", "Biswanath", "Assam", "784176", 26.7333, 93.1500),
        NortheastPlace("Gohpur", "Gohpur", "Biswanath", "Assam", "784168", 26.8833, 93.6333),
        NortheastPlace("North Lakhimpur", "North Lakhimpur", "Lakhimpur", "Assam", "787001", 27.2344, 94.1038),
        NortheastPlace("Bihpuria", "Bihpuria", "Lakhimpur", "Assam", "784161", 27.0333, 93.9000),
        NortheastPlace("Dhakuakhana", "Dhakuakhana", "Lakhimpur", "Assam", "787055", 27.2500, 94.4500),
        NortheastPlace("Dhemaji", "Dhemaji", "Dhemaji", "Assam", "787057", 27.4816, 94.5772),
        NortheastPlace("Silapathar", "Silapathar", "Dhemaji", "Assam", "787059", 27.5889, 94.7214),
        NortheastPlace("Jonai, Murkongselek", "Jonai", "Dhemaji", "Assam", "787060", 27.8333, 95.1667),

        // =========================================================================
        // ASSAM : Upper Assam Districts
        // =========================================================================
        NortheastPlace("Kohora, Kaziranga National Park", "Kaziranga", "Golaghat", "Assam", "785609", 26.5862, 93.4116),
        NortheastPlace("Bagori, Kaziranga", "Kaziranga", "Nagaon", "Assam", "782136", 26.5684, 93.3082),
        NortheastPlace("Bokakhat", "Bokakhat", "Golaghat", "Assam", "785612", 26.6190, 93.5937),
        NortheastPlace("Golaghat", "Golaghat", "Golaghat", "Assam", "785621", 26.5239, 93.9634),
        NortheastPlace("Dergaon", "Dergaon", "Golaghat", "Assam", "785614", 26.7022, 93.9745),
        NortheastPlace("Sarupathar", "Sarupathar", "Golaghat", "Assam", "785601", 26.1833, 93.8167),
        NortheastPlace("Jorhat", "Jorhat", "Jorhat", "Assam", "785001", 26.7509, 94.2037),
        NortheastPlace("Mariani", "Mariani", "Jorhat", "Assam", "785634", 26.6667, 94.3333),
        NortheastPlace("Teok", "Teok", "Jorhat", "Assam", "785112", 26.8488, 94.4259),
        NortheastPlace("Titabor", "Titabor", "Jorhat", "Assam", "785630", 26.6000, 94.2000),
        NortheastPlace("Majuli, Kamalabari", "Majuli", "Majuli", "Assam", "785104", 26.9602, 94.2127),
        NortheastPlace("Garmur, Majuli", "Majuli", "Majuli", "Assam", "785104", 26.9833, 94.2500),
        NortheastPlace("Sivasagar, Rang Ghar", "Sivasagar", "Sivasagar", "Assam", "785640", 26.9826, 94.6425),
        NortheastPlace("Nazira, ONGC", "Nazira", "Sivasagar", "Assam", "785685", 26.9167, 94.7333),
        NortheastPlace("Amguri", "Amguri", "Sivasagar", "Assam", "785680", 26.8167, 94.5333),
        NortheastPlace("Demow", "Demow", "Sivasagar", "Assam", "785662", 27.1333, 94.7500),
        NortheastPlace("Sonari, Charaideo Maidams", "Sonari", "Charaideo", "Assam", "785690", 27.0167, 95.0333),
        NortheastPlace("Moranhat", "Moranhat", "Charaideo", "Assam", "785670", 27.1884, 94.9287),
        NortheastPlace("Dibrugarh, Chowkidinghee", "Dibrugarh", "Dibrugarh", "Assam", "786001", 27.4728, 94.9120),
        NortheastPlace("Dibrugarh University", "Dibrugarh", "Dibrugarh", "Assam", "786004", 27.4521, 94.8982),
        NortheastPlace("Duliajan, Oil India HQ", "Duliajan", "Dibrugarh", "Assam", "786602", 27.3592, 95.3182),
        NortheastPlace("Naharkatia", "Naharkatia", "Dibrugarh", "Assam", "786610", 27.2833, 95.3333),
        NortheastPlace("Chabua", "Chabua", "Dibrugarh", "Assam", "786184", 27.4833, 95.1833),
        NortheastPlace("Tinsukia", "Tinsukia", "Tinsukia", "Assam", "786125", 27.4922, 95.3468),
        NortheastPlace("Digboi, Oldest Refinery", "Digboi", "Tinsukia", "Assam", "786171", 27.3820, 95.6267),
        NortheastPlace("Margherita", "Margherita", "Tinsukia", "Assam", "786181", 27.2882, 95.6811),
        NortheastPlace("Ledo, Stilwell Road", "Ledo", "Tinsukia", "Assam", "786182", 27.3015, 95.7389),
        NortheastPlace("Doomdooma", "Doomdooma", "Tinsukia", "Assam", "786151", 27.5683, 95.5433),
        NortheastPlace("Sadiya, Dhola-Sadiya Bridge", "Sadiya", "Tinsukia", "Assam", "786158", 27.8333, 95.6667),

        // =========================================================================
        // ASSAM : Hills & Barak Valley Districts
        // =========================================================================
        NortheastPlace("Diphu", "Diphu", "Karbi Anglong", "Assam", "782460", 25.8458, 93.4339),
        NortheastPlace("Bokajan", "Bokajan", "Karbi Anglong", "Assam", "782480", 26.0167, 93.7833),
        NortheastPlace("Hamren", "Hamren", "West Karbi Anglong", "Assam", "782486", 25.8667, 92.5667),
        NortheastPlace("Haflong Hill Station", "Haflong", "Dima Hasao", "Assam", "788819", 25.1764, 93.0181),
        NortheastPlace("Jatinga", "Jatinga", "Dima Hasao", "Assam", "788819", 25.1232, 93.0421),
        NortheastPlace("Umrangso", "Umrangso", "Dima Hasao", "Assam", "788931", 25.5167, 92.7833),
        NortheastPlace("Maibang", "Maibang", "Dima Hasao", "Assam", "788836", 25.3000, 93.1667),
        NortheastPlace("Silchar", "Silchar", "Cachar", "Assam", "788001", 24.8333, 92.7789),
        NortheastPlace("Silchar Medical College", "Silchar", "Cachar", "Assam", "788014", 24.7892, 92.7915),
        NortheastPlace("Karimganj", "Karimganj", "Karimganj", "Assam", "788710", 24.8649, 92.3593),
        NortheastPlace("Badarpur Junction", "Badarpur", "Karimganj", "Assam", "788806", 24.9000, 92.6000),
        NortheastPlace("Hailakandi", "Hailakandi", "Hailakandi", "Assam", "788151", 24.6833, 92.5667),

        // =========================================================================
        // MEGHALAYA : Khasi, Jaintia & Garo Hills
        // =========================================================================
        NortheastPlace("Police Bazar, Shillong", "Shillong", "East Khasi Hills", "Meghalaya", "793001", 25.5788, 91.8831),
        NortheastPlace("Laitumkhrah, Shillong", "Shillong", "East Khasi Hills", "Meghalaya", "793003", 25.5701, 91.8967),
        NortheastPlace("Mawlai, Shillong", "Shillong", "East Khasi Hills", "Meghalaya", "793008", 25.5969, 91.8745),
        NortheastPlace("Laban, Shillong", "Shillong", "East Khasi Hills", "Meghalaya", "793004", 25.5658, 91.8722),
        NortheastPlace("Shillong Peak", "Shillong", "East Khasi Hills", "Meghalaya", "793005", 25.5348, 91.8542),
        NortheastPlace("Elephant Falls", "Shillong", "East Khasi Hills", "Meghalaya", "793009", 25.5369, 91.8239),
        NortheastPlace("Umiam Lake, Barapani", "Barapani", "Ri-Bhoi", "Meghalaya", "793103", 25.6601, 91.9056),
        NortheastPlace("Umsning", "Umsning", "Ri-Bhoi", "Meghalaya", "793105", 25.7500, 91.8833),
        NortheastPlace("Nongpoh", "Nongpoh", "Ri-Bhoi", "Meghalaya", "793102", 25.9038, 91.8797),
        NortheastPlace("Byrnihat", "Byrnihat", "Ri-Bhoi", "Meghalaya", "793101", 26.0592, 91.8654),
        NortheastPlace("Cherrapunji, Sohra", "Cherrapunji", "East Khasi Hills", "Meghalaya", "793108", 25.2702, 91.7323),
        NortheastPlace("Nohkalikai Falls", "Cherrapunji", "East Khasi Hills", "Meghalaya", "793108", 25.2757, 91.6845),
        NortheastPlace("Mawsynram", "Mawsynram", "East Khasi Hills", "Meghalaya", "793113", 25.2974, 91.5839),
        NortheastPlace("Dawki, Umngot River", "Dawki", "West Jaintia Hills", "Meghalaya", "793109", 25.1873, 92.0163),
        NortheastPlace("Mawlynnong", "Mawlynnong", "East Khasi Hills", "Meghalaya", "793110", 25.2017, 91.9167),
        NortheastPlace("Laitlum Canyons", "Laitlum", "East Khasi Hills", "Meghalaya", "793015", 25.4745, 91.9082),
        NortheastPlace("Jowai", "Jowai", "West Jaintia Hills", "Meghalaya", "793150", 25.4489, 92.2036),
        NortheastPlace("Khliehriat", "Khliehriat", "East Jaintia Hills", "Meghalaya", "793200", 25.3524, 92.3684),
        NortheastPlace("Nongstoin", "Nongstoin", "West Khasi Hills", "Meghalaya", "793119", 25.5208, 91.2694),
        NortheastPlace("Mairang", "Mairang", "Eastern West Khasi Hills", "Meghalaya", "793120", 25.5604, 91.6369),
        NortheastPlace("Tura", "Tura", "West Garo Hills", "Meghalaya", "794001", 25.5144, 90.2032),
        NortheastPlace("Williamnagar", "Williamnagar", "East Garo Hills", "Meghalaya", "794111", 25.5978, 90.6172),
        NortheastPlace("Baghmara", "Baghmara", "South Garo Hills", "Meghalaya", "794102", 25.1972, 90.6389),
        NortheastPlace("Ampati", "Ampati", "South West Garo Hills", "Meghalaya", "794115", 25.4667, 89.9333),
        NortheastPlace("Resubelpara", "Resubelpara", "North Garo Hills", "Meghalaya", "794108", 25.9000, 90.6000),

        // =========================================================================
        // ARUNACHAL PRADESH : Valleys, Hills & Frontier Passes
        // =========================================================================
        NortheastPlace("Itanagar, Capital Complex", "Itanagar", "Papum Pare", "Arunachal Pradesh", "791111", 27.0844, 93.6053),
        NortheastPlace("Naharlagun", "Naharlagun", "Papum Pare", "Arunachal Pradesh", "791110", 27.1064, 93.6936),
        NortheastPlace("Nirjuli", "Nirjuli", "Papum Pare", "Arunachal Pradesh", "791109", 27.1333, 93.7500),
        NortheastPlace("Banderdewa", "Banderdewa", "Papum Pare", "Arunachal Pradesh", "791123", 27.1422, 93.8189),
        NortheastPlace("Tawang Monastery", "Tawang", "Tawang", "Arunachal Pradesh", "790104", 27.5861, 91.8594),
        NortheastPlace("Sela Pass", "Sela Pass", "Tawang", "Arunachal Pradesh", "790105", 27.5042, 92.1039),
        NortheastPlace("Dirang", "Dirang", "West Kameng", "Arunachal Pradesh", "790101", 27.3592, 92.2412),
        NortheastPlace("Bomdila", "Bomdila", "West Kameng", "Arunachal Pradesh", "790001", 27.2645, 92.4231),
        NortheastPlace("Bhalukpong", "Bhalukpong", "West Kameng", "Arunachal Pradesh", "790114", 27.0145, 92.6469),
        NortheastPlace("Ziro Valley", "Ziro", "Lower Subansiri", "Arunachal Pradesh", "791120", 27.5962, 93.8295),
        NortheastPlace("Daporijo", "Daporijo", "Upper Subansiri", "Arunachal Pradesh", "791122", 27.9892, 94.2201),
        NortheastPlace("Aalo (Along)", "Aalo", "West Siang", "Arunachal Pradesh", "791001", 28.1692, 94.8028),
        NortheastPlace("Pasighat", "Pasighat", "East Siang", "Arunachal Pradesh", "791102", 28.0665, 95.3268),
        NortheastPlace("Yingkiong", "Yingkiong", "Upper Siang", "Arunachal Pradesh", "791002", 28.6167, 94.9167),
        NortheastPlace("Roing", "Roing", "Lower Dibang Valley", "Arunachal Pradesh", "792110", 28.1408, 95.8364),
        NortheastPlace("Anini", "Anini", "Dibang Valley", "Arunachal Pradesh", "792101", 28.7833, 95.9000),
        NortheastPlace("Tezu", "Tezu", "Lohit", "Arunachal Pradesh", "792001", 27.9192, 96.1664),
        NortheastPlace("Namsai, Golden Pagoda", "Namsai", "Namsai", "Arunachal Pradesh", "792103", 27.6711, 95.8672),
        NortheastPlace("Changlang", "Changlang", "Changlang", "Arunachal Pradesh", "792120", 27.1264, 95.7333),
        NortheastPlace("Miao", "Miao", "Changlang", "Arunachal Pradesh", "792122", 27.4833, 96.2000),
        NortheastPlace("Khonsa", "Khonsa", "Tirap", "Arunachal Pradesh", "792130", 27.0175, 95.5089),
        NortheastPlace("Longding", "Longding", "Longding", "Arunachal Pradesh", "792131", 26.8667, 95.3167),
        NortheastPlace("Seppa", "Seppa", "East Kameng", "Arunachal Pradesh", "790102", 27.3500, 93.0333),
        NortheastPlace("Koloriang", "Koloriang", "Kurung Kumey", "Arunachal Pradesh", "791118", 27.9000, 93.3500),
        NortheastPlace("Hawai", "Hawai", "Anjaw", "Arunachal Pradesh", "792104", 27.8833, 96.8000),

        // =========================================================================
        // NAGALAND : Naga Hills & Valley Corridors
        // =========================================================================
        NortheastPlace("Kohima, Town Centre", "Kohima", "Kohima", "Nagaland", "797001", 25.6751, 94.1086),
        NortheastPlace("Kisama Heritage Village", "Kisama", "Kohima", "Nagaland", "797005", 25.6083, 94.1167),
        NortheastPlace("Dzukou Valley", "Dzukou", "Kohima", "Nagaland", "797002", 25.5601, 94.0672),
        NortheastPlace("Dimapur, Clock Tower", "Dimapur", "Dimapur", "Nagaland", "797112", 25.9095, 93.7266),
        NortheastPlace("Chümoukedima", "Chümoukedima", "Chümoukedima", "Nagaland", "797103", 25.8014, 93.7712),
        NortheastPlace("Mokokchung", "Mokokchung", "Mokokchung", "Nagaland", "798601", 26.3245, 94.5186),
        NortheastPlace("Wokha, Doyang", "Wokha", "Wokha", "Nagaland", "797111", 26.0989, 94.2612),
        NortheastPlace("Zunheboto", "Zunheboto", "Zunheboto", "Nagaland", "798620", 25.9667, 94.5167),
        NortheastPlace("Phek", "Phek", "Phek", "Nagaland", "797107", 25.6833, 94.4833),
        NortheastPlace("Pfütsero", "Pfütsero", "Phek", "Nagaland", "797107", 25.5683, 94.3167),
        NortheastPlace("Mon", "Mon", "Mon", "Nagaland", "798621", 26.7456, 95.0617),
        NortheastPlace("Tuensang", "Tuensang", "Tuensang", "Nagaland", "798612", 26.2833, 94.8333),
        NortheastPlace("Kiphire", "Kiphire", "Kiphire", "Nagaland", "798611", 25.8667, 94.7833),
        NortheastPlace("Longleng", "Longleng", "Longleng", "Nagaland", "798625", 26.4833, 94.8167),
        NortheastPlace("Peren", "Peren", "Peren", "Nagaland", "797101", 25.5167, 93.7333),

        // =========================================================================
        // MANIPUR : Imphal Valley & Hill Districts
        // =========================================================================
        NortheastPlace("Kangla, Imphal", "Imphal", "Imphal West", "Manipur", "795001", 24.8089, 93.9421),
        NortheastPlace("Thangal Bazar, Imphal", "Imphal", "Imphal West", "Manipur", "795001", 24.8170, 93.9368),
        NortheastPlace("Singjamei, Imphal", "Imphal", "Imphal West", "Manipur", "795008", 24.7833, 93.9333),
        NortheastPlace("Porompat, Imphal East", "Imphal", "Imphal East", "Manipur", "795005", 24.8215, 93.9621),
        NortheastPlace("Moirang, Loktak Lake", "Moirang", "Bishnupur", "Manipur", "795133", 24.4989, 93.7712),
        NortheastPlace("Bishnupur", "Bishnupur", "Bishnupur", "Manipur", "795126", 24.6322, 93.7612),
        NortheastPlace("Thoubal", "Thoubal", "Thoubal", "Manipur", "795138", 24.6369, 93.9964),
        NortheastPlace("Kakching", "Kakching", "Kakching", "Manipur", "795103", 24.4833, 93.9833),
        NortheastPlace("Churachandpur (Lamka)", "Churachandpur", "Churachandpur", "Manipur", "795128", 24.3333, 93.6833),
        NortheastPlace("Ukhrul, Shirui Peak", "Ukhrul", "Ukhrul", "Manipur", "795142", 25.1167, 94.3667),
        NortheastPlace("Senapati", "Senapati", "Senapati", "Manipur", "795106", 25.2667, 94.0167),
        NortheastPlace("Kangpokpi", "Kangpokpi", "Kangpokpi", "Manipur", "795129", 25.1500, 93.9667),
        NortheastPlace("Tamenglong", "Tamenglong", "Tamenglong", "Manipur", "795141", 24.9833, 93.5000),
        NortheastPlace("Chandel", "Chandel", "Chandel", "Manipur", "795127", 24.3167, 94.0000),
        NortheastPlace("Moreh Border", "Moreh", "Tengnoupal", "Manipur", "795131", 24.2503, 94.3056),
        NortheastPlace("Jiribam", "Jiribam", "Jiribam", "Manipur", "795116", 24.8000, 93.1167),

        // =========================================================================
        // MIZORAM : Ridges & Border Hills
        // =========================================================================
        NortheastPlace("Bara Bazar, Aizawl", "Aizawl", "Aizawl", "Mizoram", "796001", 23.7271, 92.7176),
        NortheastPlace("Khatla, Aizawl", "Aizawl", "Aizawl", "Mizoram", "796001", 23.7182, 92.7154),
        NortheastPlace("Kulikawn, Aizawl", "Aizawl", "Aizawl", "Mizoram", "796005", 23.6985, 92.7212),
        NortheastPlace("Bawngkawn, Aizawl", "Aizawl", "Aizawl", "Mizoram", "796014", 23.7541, 92.7389),
        NortheastPlace("Lengpui Airport", "Lengpui", "Mamit", "Mizoram", "796422", 23.8408, 92.6247),
        NortheastPlace("Lunglei", "Lunglei", "Lunglei", "Mizoram", "796701", 22.8833, 92.7333),
        NortheastPlace("Champhai", "Champhai", "Champhai", "Mizoram", "796321", 23.4756, 93.3283),
        NortheastPlace("Kolasib", "Kolasib", "Kolasib", "Mizoram", "796081", 24.2244, 92.6789),
        NortheastPlace("Vairengte", "Vairengte", "Kolasib", "Mizoram", "796101", 24.5000, 92.7667),
        NortheastPlace("Serchhip", "Serchhip", "Serchhip", "Mizoram", "796181", 23.3411, 92.8503),
        NortheastPlace("Lawngtlai", "Lawngtlai", "Lawngtlai", "Mizoram", "796891", 22.5333, 92.9000),
        NortheastPlace("Siaha", "Siaha", "Siaha", "Mizoram", "796901", 22.4833, 92.9833),
        NortheastPlace("Mamit", "Mamit", "Mamit", "Mizoram", "796441", 23.9333, 92.4833),

        // =========================================================================
        // TRIPURA : Plains & Hill Ranges
        // =========================================================================
        NortheastPlace("Agartala, Capital Complex", "Agartala", "West Tripura", "Tripura", "799001", 23.8315, 91.2868),
        NortheastPlace("Battala, Agartala", "Agartala", "West Tripura", "Tripura", "799001", 23.8267, 91.2754),
        NortheastPlace("MBB Airport, Agartala", "Agartala", "West Tripura", "Tripura", "799009", 23.8864, 91.2405),
        NortheastPlace("Udaipur, Tripureswari Temple", "Udaipur", "Gomati", "Tripura", "799120", 23.5333, 91.4833),
        NortheastPlace("Dharmanagar", "Dharmanagar", "North Tripura", "Tripura", "799250", 24.3739, 92.1645),
        NortheastPlace("Kailashahar, Unakoti", "Kailashahar", "Unakoti", "Tripura", "799277", 24.3292, 92.0089),
        NortheastPlace("Ambassa", "Ambassa", "Dhalai", "Tripura", "799289", 23.9167, 91.8500),
        NortheastPlace("Khowai", "Khowai", "Khowai", "Tripura", "799201", 24.0667, 91.6000),
        NortheastPlace("Belonia", "Belonia", "South Tripura", "Tripura", "799155", 23.2500, 91.4500),
        NortheastPlace("Sabroom", "Sabroom", "South Tripura", "Tripura", "799145", 23.0000, 91.7000),
        NortheastPlace("Bishalgarh", "Bishalgarh", "Sepahijala", "Tripura", "799102", 23.6833, 91.3167),
        NortheastPlace("Jampui Hills", "Jampui Hills", "North Tripura", "Tripura", "799269", 23.9667, 92.2667),

        // =========================================================================
        // SIKKIM : Himalayan Valleys & Passes
        // =========================================================================
        NortheastPlace("MG Marg, Gangtok", "Gangtok", "East Sikkim", "Sikkim", "737101", 27.3314, 88.6138),
        NortheastPlace("Deorali, Gangtok", "Gangtok", "East Sikkim", "Sikkim", "737102", 27.3182, 88.6085),
        NortheastPlace("Tadong, Gangtok", "Gangtok", "East Sikkim", "Sikkim", "737102", 27.3095, 88.6012),
        NortheastPlace("Rumtek Monastery", "Rumtek", "East Sikkim", "Sikkim", "737135", 27.3000, 88.5500),
        NortheastPlace("Namchi, Char Dham", "Namchi", "South Sikkim", "Sikkim", "737126", 27.1667, 88.3500),
        NortheastPlace("Ravangla, Buddha Park", "Ravangla", "South Sikkim", "Sikkim", "737139", 27.3083, 88.3639),
        NortheastPlace("Jorethang", "Jorethang", "South Sikkim", "Sikkim", "737121", 27.1167, 88.3000),
        NortheastPlace("Pelling", "Pelling", "West Sikkim", "Sikkim", "737113", 27.3000, 88.2333),
        NortheastPlace("Geyzing", "Geyzing", "West Sikkim", "Sikkim", "737111", 27.2833, 88.2500),
        NortheastPlace("Mangan", "Mangan", "North Sikkim", "Sikkim", "737116", 27.5000, 88.5333),
        NortheastPlace("Lachung, Yumthang Valley", "Lachung", "North Sikkim", "Sikkim", "737120", 27.6892, 88.7456),
        NortheastPlace("Lachen", "Lachen", "North Sikkim", "Sikkim", "737120", 27.7167, 88.5500),
        NortheastPlace("Pakyong Airport", "Pakyong", "Pakyong", "Sikkim", "737106", 27.2333, 88.5833),
        NortheastPlace("Rangpo Border", "Rangpo", "Pakyong", "Sikkim", "737132", 27.1764, 88.5312),
        NortheastPlace("Singtam", "Singtam", "East Sikkim", "Sikkim", "737134", 27.2333, 88.5000),

        // =========================================================================
        // NORTH BENGAL / FOOTHILLS GATEWAY
        // =========================================================================
        NortheastPlace("Siliguri, Sevoke Road", "Siliguri", "Darjeeling", "West Bengal", "734001", 26.7271, 88.3953),
        NortheastPlace("Bagdogra Airport", "Bagdogra", "Darjeeling", "West Bengal", "734014", 26.6812, 88.3286),
        NortheastPlace("Darjeeling Mall", "Darjeeling", "Darjeeling", "West Bengal", "734101", 27.0410, 88.2663),
        NortheastPlace("Kalimpong", "Kalimpong", "Kalimpong", "West Bengal", "734301", 27.0667, 88.4667),
        NortheastPlace("Alipurduar", "Alipurduar", "Alipurduar", "West Bengal", "736121", 26.4919, 89.5272),
        NortheastPlace("Cooch Behar", "Cooch Behar", "Cooch Behar", "West Bengal", "736101", 26.3239, 89.4510),

        // =========================================================================
        // DELHI (NCT of Delhi) : Comprehensive Geospatial Grid Across All 11 Districts
        // =========================================================================
        // Central Delhi & Old Delhi
        NortheastPlace("Connaught Place, Rajiv Chowk", "New Delhi", "Central Delhi", "Delhi", "110001", 28.6304, 77.2177),
        NortheastPlace("Karol Bagh, Ajmal Khan Road", "Delhi", "Central Delhi", "Delhi", "110005", 28.6517, 77.1906),
        NortheastPlace("Paharganj, New Delhi Railway Station", "New Delhi", "Central Delhi", "Delhi", "110055", 28.6433, 77.2144),
        NortheastPlace("Daryaganj, Delhi Gate", "Delhi", "Central Delhi", "Delhi", "110002", 28.6425, 77.2415),
        NortheastPlace("Rajendra Nagar", "Delhi", "Central Delhi", "Delhi", "110060", 28.6412, 77.1815),
        NortheastPlace("Patel Nagar", "Delhi", "Central Delhi", "Delhi", "110008", 28.6534, 77.1685),

        // New Delhi
        NortheastPlace("India Gate, Kartavya Path", "New Delhi", "New Delhi", "Delhi", "110001", 28.6129, 77.2295),
        NortheastPlace("Chanakyapuri, Diplomatic Enclave", "New Delhi", "New Delhi", "Delhi", "110021", 28.5956, 77.1855),
        NortheastPlace("Barakhamba Road, Mandi House", "New Delhi", "New Delhi", "Delhi", "110001", 28.6254, 77.2341),
        NortheastPlace("Khan Market", "New Delhi", "New Delhi", "Delhi", "110003", 28.6001, 77.2272),
        NortheastPlace("Lodhi Colony, Lodhi Road", "New Delhi", "New Delhi", "Delhi", "110003", 28.5882, 77.2235),
        NortheastPlace("Rashtrapati Bhavan, Central Vista", "New Delhi", "New Delhi", "Delhi", "110004", 28.6143, 77.1994),
        NortheastPlace("Sarojini Nagar", "New Delhi", "New Delhi", "Delhi", "110023", 28.5752, 77.1985),
        NortheastPlace("AIIMS, Ansari Nagar", "New Delhi", "New Delhi", "Delhi", "110029", 28.5672, 77.2100),
        NortheastPlace("Jor Bagh, Safdarjung", "New Delhi", "New Delhi", "Delhi", "110003", 28.5835, 77.2125),

        // South Delhi
        NortheastPlace("Hauz Khas, Aurobindo Marg", "New Delhi", "South Delhi", "Delhi", "110016", 28.5494, 77.2001),
        NortheastPlace("Saket, Select Citywalk", "New Delhi", "South Delhi", "Delhi", "110017", 28.5245, 77.2185),
        NortheastPlace("Greater Kailash 1, M-Block", "New Delhi", "South Delhi", "Delhi", "110048", 28.5482, 77.2345),
        NortheastPlace("Greater Kailash 2", "New Delhi", "South Delhi", "Delhi", "110048", 28.5345, 77.2412),
        NortheastPlace("Malviya Nagar", "New Delhi", "South Delhi", "Delhi", "110017", 28.5365, 77.2085),
        NortheastPlace("Green Park", "New Delhi", "South Delhi", "Delhi", "110016", 28.5585, 77.2032),
        NortheastPlace("South Extension", "New Delhi", "South Delhi", "Delhi", "110049", 28.5702, 77.2225),
        NortheastPlace("Lajpat Nagar, Central Market", "New Delhi", "South Delhi", "Delhi", "110024", 28.5685, 77.2435),
        NortheastPlace("Mehrauli, Qutub Minar", "New Delhi", "South Delhi", "Delhi", "110030", 28.5245, 77.1855),
        NortheastPlace("Vasant Kunj, Nelson Mandela Marg", "New Delhi", "South Delhi", "Delhi", "110070", 28.5295, 77.1525),
        NortheastPlace("Vasant Vihar", "New Delhi", "South Delhi", "Delhi", "110057", 28.5595, 77.1612),
        NortheastPlace("RK Puram, Sector 1", "New Delhi", "South Delhi", "Delhi", "110022", 28.5685, 77.1785),

        // South East Delhi
        NortheastPlace("Okhla Industrial Area Phase 3", "New Delhi", "South East Delhi", "Delhi", "110020", 28.5412, 77.2715),
        NortheastPlace("Nehru Place, IT Market", "New Delhi", "South East Delhi", "Delhi", "110019", 28.5495, 77.2525),
        NortheastPlace("Kalkaji", "New Delhi", "South East Delhi", "Delhi", "110019", 28.5385, 77.2585),
        NortheastPlace("Ashram Chowk, Mathura Road", "New Delhi", "South East Delhi", "Delhi", "110014", 28.5715, 77.2612),
        NortheastPlace("Jamia Nagar, Okhla", "New Delhi", "South East Delhi", "Delhi", "110025", 28.5612, 77.2885),
        NortheastPlace("Sarita Vihar", "New Delhi", "South East Delhi", "Delhi", "110076", 28.5295, 77.2915),
        NortheastPlace("Badarpur Border", "New Delhi", "South East Delhi", "Delhi", "110044", 28.5025, 77.3015),
        NortheastPlace("Jasola Vihar", "New Delhi", "South East Delhi", "Delhi", "110025", 28.5385, 77.2885),

        // West Delhi
        NortheastPlace("Rajouri Garden, Main Market", "Delhi", "West Delhi", "Delhi", "110027", 28.6485, 77.1215),
        NortheastPlace("Punjabi Bagh", "Delhi", "West Delhi", "Delhi", "110026", 28.6685, 77.1315),
        NortheastPlace("Janakpuri District Centre", "New Delhi", "West Delhi", "Delhi", "110058", 28.6285, 77.0815),
        NortheastPlace("Tilak Nagar", "Delhi", "West Delhi", "Delhi", "110018", 28.6385, 77.0985),
        NortheastPlace("Vikaspuri", "New Delhi", "West Delhi", "Delhi", "110018", 28.6385, 77.0685),
        NortheastPlace("Paschim Vihar", "Delhi", "West Delhi", "Delhi", "110063", 28.6685, 77.0985),
        NortheastPlace("Uttam Nagar", "Delhi", "West Delhi", "Delhi", "110059", 28.6215, 77.0585),

        // South West Delhi
        NortheastPlace("Dwarka Sector 6", "New Delhi", "South West Delhi", "Delhi", "110075", 28.5885, 77.0615),
        NortheastPlace("Dwarka Sector 10, Metro Station", "New Delhi", "South West Delhi", "Delhi", "110075", 28.5815, 77.0585),
        NortheastPlace("Dwarka Sector 12", "New Delhi", "South West Delhi", "Delhi", "110078", 28.5915, 77.0415),
        NortheastPlace("Dwarka Sector 21", "New Delhi", "South West Delhi", "Delhi", "110077", 28.5525, 77.0585),
        NortheastPlace("Palam, Manglapuri", "New Delhi", "South West Delhi", "Delhi", "110045", 28.5885, 77.0885),
        NortheastPlace("Delhi Cantt", "New Delhi", "South West Delhi", "Delhi", "110010", 28.5985, 77.1315),
        NortheastPlace("Mahipalpur, Aerocity", "New Delhi", "South West Delhi", "Delhi", "110037", 28.5485, 77.1215),
        NortheastPlace("IGI Airport Terminal 3", "New Delhi", "South West Delhi", "Delhi", "110037", 28.5562, 77.0855),
        NortheastPlace("IGI Airport Terminal 1", "New Delhi", "South West Delhi", "Delhi", "110037", 28.5685, 77.1185),
        NortheastPlace("Najafgarh", "Delhi", "South West Delhi", "Delhi", "110043", 28.6085, 76.9815),

        // North Delhi
        NortheastPlace("Civil Lines", "Delhi", "North Delhi", "Delhi", "110054", 28.6785, 77.2215),
        NortheastPlace("Kashmere Gate ISBT", "Delhi", "North Delhi", "Delhi", "110006", 28.6685, 77.2315),
        NortheastPlace("Chandni Chowk, Red Fort", "Delhi", "North Delhi", "Delhi", "110006", 28.6562, 77.2385),
        NortheastPlace("Sadar Bazar", "Delhi", "North Delhi", "Delhi", "110006", 28.6545, 77.2115),
        NortheastPlace("Delhi University North Campus", "Delhi", "North Delhi", "Delhi", "110007", 28.6885, 77.2085),
        NortheastPlace("Kamla Nagar", "Delhi", "North Delhi", "Delhi", "110007", 28.6815, 77.2015),
        NortheastPlace("Model Town", "Delhi", "North Delhi", "Delhi", "110009", 28.7015, 77.1915),
        NortheastPlace("Azadpur Mandi", "Delhi", "North Delhi", "Delhi", "110033", 28.7185, 77.1785),
        NortheastPlace("Burari", "Delhi", "North Delhi", "Delhi", "110084", 28.7515, 77.1985),
        NortheastPlace("Alipur", "Delhi", "North Delhi", "Delhi", "110036", 28.7985, 77.1315),
        NortheastPlace("Narela", "Delhi", "North Delhi", "Delhi", "110040", 28.8515, 77.0915),

        // North West Delhi
        NortheastPlace("Rohini Sector 3", "Delhi", "North West Delhi", "Delhi", "110085", 28.7015, 77.1185),
        NortheastPlace("Rohini Sector 7", "Delhi", "North West Delhi", "Delhi", "110085", 28.7085, 77.1215),
        NortheastPlace("Rohini Sector 9", "Delhi", "North West Delhi", "Delhi", "110085", 28.7185, 77.1315),
        NortheastPlace("Rohini Sector 14", "Delhi", "North West Delhi", "Delhi", "110085", 28.7245, 77.1385),
        NortheastPlace("Rohini Sector 18", "Delhi", "North West Delhi", "Delhi", "110089", 28.7415, 77.1315),
        NortheastPlace("Rohini Sector 24", "Delhi", "North West Delhi", "Delhi", "110085", 28.7315, 77.0815),
        NortheastPlace("Pitampura, TV Tower", "Delhi", "North West Delhi", "Delhi", "110034", 28.6985, 77.1415),
        NortheastPlace("Shalimar Bagh", "Delhi", "North West Delhi", "Delhi", "110088", 28.7115, 77.1615),
        NortheastPlace("Prashant Vihar", "Delhi", "North West Delhi", "Delhi", "110085", 28.7115, 77.1315),
        NortheastPlace("Ashok Vihar", "Delhi", "North West Delhi", "Delhi", "110052", 28.6915, 77.1715),
        NortheastPlace("Mangolpuri", "Delhi", "North West Delhi", "Delhi", "110083", 28.6915, 77.0785),
        NortheastPlace("Bawana", "Delhi", "North West Delhi", "Delhi", "110039", 28.7985, 77.0385),

        // East Delhi & North East Delhi
        NortheastPlace("Laxmi Nagar, Vikas Marg", "Delhi", "East Delhi", "Delhi", "110092", 28.6315, 77.2785),
        NortheastPlace("Preet Vihar", "Delhi", "East Delhi", "Delhi", "110092", 28.6415, 77.2915),
        NortheastPlace("Mayur Vihar Phase 1", "Delhi", "East Delhi", "Delhi", "110091", 28.6085, 77.2985),
        NortheastPlace("Mayur Vihar Phase 2", "Delhi", "East Delhi", "Delhi", "110091", 28.6185, 77.3085),
        NortheastPlace("Mayur Vihar Phase 3", "Delhi", "East Delhi", "Delhi", "110096", 28.6015, 77.3315),
        NortheastPlace("Akshardham", "New Delhi", "East Delhi", "Delhi", "110092", 28.6125, 77.2772),
        NortheastPlace("Shakarpur", "Delhi", "East Delhi", "Delhi", "110092", 28.6285, 77.2785),
        NortheastPlace("Patparganj", "Delhi", "East Delhi", "Delhi", "110092", 28.6315, 77.3015),
        NortheastPlace("Anand Vihar ISBT", "Delhi", "East Delhi", "Delhi", "110092", 28.6485, 77.3185),
        NortheastPlace("Shahdara, Metro Station", "Delhi", "Shahdara", "Delhi", "110032", 28.6715, 77.2885),
        NortheastPlace("Dilshad Garden", "Delhi", "Shahdara", "Delhi", "110095", 28.6815, 77.3185),
        NortheastPlace("Seelampur", "Delhi", "North East Delhi", "Delhi", "110053", 28.6685, 77.2685),
        NortheastPlace("Yamuna Vihar", "Delhi", "North East Delhi", "Delhi", "110053", 28.6985, 77.2715),
        NortheastPlace("Bhajanpura", "Delhi", "North East Delhi", "Delhi", "110053", 28.7015, 77.2615),
        NortheastPlace("Karawal Nagar", "Delhi", "North East Delhi", "Delhi", "110094", 28.7285, 77.2785)
    )

    /**
     * Resolves GPS coordinates strictly offline using embedded regional geospatial nodes.
     * Guaranteed to work with ZERO internet connectivity across Assam, Arunachal Pradesh,
     * Meghalaya, Manipur, Mizoram, Tripura, Nagaland, Sikkim, and Delhi.
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

        // If within 150 km of any regional place, provide rich offline address matching reference styling
        return if (minDistanceKm <= 150.0) {
            val shortCode = OpenLocationCode.getShortCode(latitude, longitude)

            val formatted = if (minDistanceKm <= 1.5) {
                val cityPart = if (nearestPlace.name.contains(nearestPlace.city, ignoreCase = true) || nearestPlace.city.equals(nearestPlace.state, ignoreCase = true)) {
                    ""
                } else {
                    ", ${nearestPlace.city}"
                }
                "$shortCode, ${nearestPlace.name}$cityPart, ${nearestPlace.state} ${nearestPlace.pincode}, India"
            } else {
                val dir = getCardinalDirection(nearestPlace.latitude, nearestPlace.longitude, latitude, longitude)
                val cityPart = if (nearestPlace.name.contains(nearestPlace.city, ignoreCase = true) || nearestPlace.city.equals(nearestPlace.state, ignoreCase = true)) {
                    ""
                } else {
                    ", ${nearestPlace.city}"
                }
                String.format(
                    Locale.US,
                    "$shortCode, %.1f km %s of %s$cityPart, %s %s, India",
                    minDistanceKm,
                    dir,
                    nearestPlace.name,
                    nearestPlace.state,
                    nearestPlace.pincode
                )
            }

            ResolvedAddress(
                formattedAddress = formatted,
                city = nearestPlace.city,
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
