package com.anagorny.gpxanimatorbot.clients

import org.geojson.FeatureCollection
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Service
@FeignClient(value = "geocoderClient", url = "\${external.geocoder.url}")
interface GeocoderClient {

    @GetMapping("/reverse")
    fun reverse(
        @RequestParam("lon") lon: Double,
        @RequestParam("lat") lat: Double,
        @RequestParam(defaultValue = "en") lang: String = "en"
    ): FeatureCollection
}