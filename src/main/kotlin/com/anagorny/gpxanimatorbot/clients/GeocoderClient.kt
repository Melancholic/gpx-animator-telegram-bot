package com.anagorny.gpxanimatorbot.clients

interface GeocoderClient {

    fun reverse(lon: Double, lat: Double, lang: String = "en"): ReverseGeocodingResult
}

/**
 * The GeoJSON photon.komoot.io answers with, narrowed to the only part we read.
 * Feature properties are free-form (city/state/country/...), so they stay a map.
 */
data class ReverseGeocodingResult(
    val features: List<Feature> = emptyList()
) {
    data class Feature(val properties: Map<String, Any?> = emptyMap())
}
