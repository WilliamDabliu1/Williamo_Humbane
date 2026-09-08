package com.williamo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.williamo.dto.openmeteo.ForecastResponse;
import com.williamo.dto.openmeteo.GeocodingResponse;
import com.williamo.dto.openmeteo.GeocodingResult;
import com.williamo.exception.OpenMeteoIntegrationException;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);
    private static final String SOURCE_GEOCODING = "open-meteo-geocoding";
    private static final String SOURCE_FORECAST = "open-meteo-forecast";

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
            GeocodingResponse body = entity.getBody();
            log.info("Open-Meteo Geocoding HTTP status {}", status);

            if (!entity.getStatusCode().is2xxSuccessful()) {
                throw integrationError(
                        SOURCE_GEOCODING,
                        status,
                        reasonOf(body != null ? body.getReason() : null, "HTTP " + status));
            }
            if (body == null) {
                throw integrationError(SOURCE_GEOCODING, status, "empty body");
            }
            if (Boolean.TRUE.equals(body.getError())) {
                throw integrationError(
                        SOURCE_GEOCODING,
                        status,
                        reasonOf(body.getReason(), "geocoding rejected the request"));
            }
            if (body.getResults() == null) {
                return Collections.emptyList();
            }
            return body.getResults();
        } catch (OpenMeteoIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw mapTransportError(SOURCE_GEOCODING, ex);
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
            ForecastResponse body = entity.getBody();
            log.info("Open-Meteo Forecast HTTP status {}", status);

            if (!entity.getStatusCode().is2xxSuccessful()) {
                throw integrationError(
                        SOURCE_FORECAST,
                        status,
                        reasonOf(body != null ? body.getReason() : null, "HTTP " + status));
            }
            if (body == null) {
                throw integrationError(SOURCE_FORECAST, status, "empty body");
            }
            if (Boolean.TRUE.equals(body.getError())) {
                throw integrationError(
                        SOURCE_FORECAST,
                        status,
                        reasonOf(body.getReason(), "forecast rejected the request"));
            }
            if (body.getCurrent() == null) {
                throw integrationError(SOURCE_FORECAST, status, "incomplete forecast: current weather is missing");
            }
            return body;
        } catch (OpenMeteoIntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw mapTransportError(SOURCE_FORECAST, ex);
        }
    }

    private OpenMeteoIntegrationException mapTransportError(String source, Exception ex) {
        if (ex instanceof OpenMeteoIntegrationException) {
            return (OpenMeteoIntegrationException) ex;
        }
        if (isTimeout(ex)) {
            log.warn("{} timed out", source, ex);
            return new OpenMeteoIntegrationException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    source,
                    null,
                    source + " timed out",
                    ex);
        }
        if (isUnknownHost(ex)) {
            log.error("{} host could not be resolved", source, ex);
            return new OpenMeteoIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    source,
                    null,
                    source + " is unreachable",
                    ex);
        }
        if (ex instanceof HttpMessageConversionException) {
            log.error("{} returned an invalid payload", source, ex);
            return new OpenMeteoIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    source,
                    null,
                    source + " returned an invalid response",
                    ex);
        }
        if (ex instanceof ResourceAccessException || ex instanceof RestClientException) {
            log.error("{} transport error", source, ex);
            return new OpenMeteoIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    source,
                    null,
                    source + " is unavailable",
                    ex);
        }
        log.error("{} unexpected integration error", source, ex);
        return new OpenMeteoIntegrationException(
                HttpStatus.BAD_GATEWAY,
                source,
                null,
                source + " failed",
                ex);
    }

    private OpenMeteoIntegrationException integrationError(String source, int remoteStatus, String reason) {
        HttpStatus status = mapRemoteStatus(remoteStatus);
        log.warn("{} HTTP {} reason={}", source, remoteStatus, reason);
        return new OpenMeteoIntegrationException(
                status,
                source,
                remoteStatus,
                source + " error: " + reason);
    }

    private HttpStatus mapRemoteStatus(int remoteStatus) {
        if (remoteStatus == 429) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (remoteStatus == 408 || remoteStatus == 504) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (remoteStatus >= 500) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (remoteStatus >= 400) {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private String reasonOf(String reason, String fallback) {
        if (reason == null || reason.trim().isEmpty()) {
            return fallback;
        }
        return reason.trim();
    }

    private boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isUnknownHost(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
