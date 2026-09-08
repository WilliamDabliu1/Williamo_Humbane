package com.williamo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.williamo.dto.openmeteo.ForecastResponse;
import com.williamo.dto.openmeteo.GeocodingResponse;
import com.williamo.dto.openmeteo.GeocodingResult;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private final RestTemplate restTemplate;
    private final String geocodingUrl;
    private final int geocodingCount;
    private final String geocodingLanguage;
    private final String geocodingFormat;
    private final String forecastUrl;
    private final String forecastCurrent;

    public OpenMeteoClient(
            RestTemplate restTemplate,
            @Value("${openmeteo.geocoding.url}") String geocodingUrl,
            @Value("${openmeteo.geocoding.count}") int geocodingCount,
            @Value("${openmeteo.geocoding.language}") String geocodingLanguage,
            @Value("${openmeteo.geocoding.format}") String geocodingFormat,
            @Value("${openmeteo.forecast.url}") String forecastUrl,
            @Value("${openmeteo.forecast.current}") String forecastCurrent) {
        this.restTemplate = restTemplate;
        this.geocodingUrl = geocodingUrl;
        this.geocodingCount = geocodingCount;
        this.geocodingLanguage = geocodingLanguage;
        this.geocodingFormat = geocodingFormat;
        this.forecastUrl = forecastUrl;
        this.forecastCurrent = forecastCurrent;
    }

    public List<GeocodingResult> searchCities(String name) {
        URI uri = UriComponentsBuilder.fromUriString(geocodingUrl)
                .queryParam("name", name)
                .queryParam("count", geocodingCount)
                .queryParam("language", geocodingLanguage)
                .queryParam("format", geocodingFormat)
                .build()
                .encode()
                .toUri();

        log.info("Calling Open-Meteo Geocoding API {}", uri);
        try {
            ResponseEntity<GeocodingResponse> entity =
                    restTemplate.exchange(uri, HttpMethod.GET, null, GeocodingResponse.class);
            int status = entity.getStatusCode().value();
            log.info("Open-Meteo Geocoding HTTP status {}", status);
            if (!entity.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Open-Meteo Geocoding API returned HTTP " + status);
            }

            GeocodingResponse body = entity.getBody();
            if (body == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Open-Meteo Geocoding API returned an empty body");
            }
            if (body.getResults() == null) {
                return Collections.emptyList();
            }
            return body.getResults();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Failed to call Open-Meteo Geocoding API", ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to query the Open-Meteo Geocoding API",
                    ex);
        }
    }

    public ForecastResponse getCurrentWeather(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUriString(forecastUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", forecastCurrent)
                .queryParam("timezone", "auto")
                .build()
                .encode()
                .toUri();

        log.info("Calling Open-Meteo Weather Forecast API {}", uri);
        try {
            ResponseEntity<ForecastResponse> entity =
                    restTemplate.exchange(uri, HttpMethod.GET, null, ForecastResponse.class);
            int status = entity.getStatusCode().value();
            log.info("Open-Meteo Forecast HTTP status {}", status);
            if (!entity.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Open-Meteo Weather Forecast API returned HTTP " + status);
            }

            ForecastResponse body = entity.getBody();
            if (body == null
                    || Boolean.TRUE.equals(body.getError())
                    || body.getCurrent() == null) {
                String reason = body != null && body.getReason() != null
                        ? body.getReason()
                        : "empty or incomplete forecast";
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Invalid Open-Meteo Forecast response: " + reason);
            }
            return body;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Failed to call Open-Meteo Weather Forecast API", ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to query the Open-Meteo Weather Forecast API",
                    ex);
        }
    }
}
