package com.williamo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.williamo.client.OpenMeteoClient;
import com.williamo.dto.HistoryPageResponse;
import com.williamo.dto.WeatherQueryResponse;
import com.williamo.dto.openmeteo.ForecastCurrent;
import com.williamo.dto.openmeteo.ForecastCurrentUnits;
import com.williamo.dto.openmeteo.ForecastResponse;
import com.williamo.dto.openmeteo.GeocodingResult;
import com.williamo.entity.WeatherQuery;
import com.williamo.repository.WeatherQueryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final OpenMeteoClient openMeteoClient;
    private final WeatherQueryRepository weatherQueryRepository;

    public WeatherService(OpenMeteoClient openMeteoClient, WeatherQueryRepository weatherQueryRepository) {
        this.openMeteoClient = openMeteoClient;
        this.weatherQueryRepository = weatherQueryRepository;
    }

    public WeatherQueryResponse getWeather(String city, Double latitude, Double longitude) {
        log.info("Validating received parameters city={} latitude={} longitude={}", city, latitude, longitude);
        validateParameters(city, latitude, longitude);
        boolean hasCity = hasText(city);
        boolean hasCoordinates = latitude != null && longitude != null;

        GeocodingResult location = null;
        if (hasCity) {
            log.info("Calling Open-Meteo Geocoding API for city={}", city.trim());
            List<GeocodingResult> results = openMeteoClient.searchCities(city.trim());
            location = selectLocation(results, city.trim(), latitude, longitude);
            validateLocation(location);
            log.info("Selected location city={} country={} countryCode={} region={} lat={} lon={} timezone={}",
                    location.getName(),
                    location.getCountry(),
                    location.getCountryCode(),
                    location.getAdmin1(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getTimezone());
        }

        double forecastLatitude;
        double forecastLongitude;
        if (hasCoordinates) {
            forecastLatitude = latitude;
            forecastLongitude = longitude;
        } else {
            forecastLatitude = location.getLatitude();
            forecastLongitude = location.getLongitude();
        }

        log.info("Extracted coordinates latitude={} longitude={}", 
        		forecastLatitude, forecastLongitude);
        ForecastResponse forecast = openMeteoClient.getCurrentWeather(forecastLatitude, forecastLongitude);

        if (location == null) {
            String inferredCity = cityFromTimezone(forecast.getTimezone());
            if (hasText(inferredCity) && !"Unknown".equals(inferredCity)) {
                log.info("Calling Open-Meteo Geocoding API to enrich coordinates city={}", inferredCity);
                List<GeocodingResult> results = openMeteoClient.searchCities(inferredCity);
                if (!results.isEmpty()) {
                    location = selectLocation(results, inferredCity, forecastLatitude, forecastLongitude);
                    validateLocation(location);
                    log.info("Selected location city={} country={} region={}",
                            location.getName(), location.getCountry(), location.getAdmin1());
                }
            }
        }

        WeatherQueryResponse mapped = mapToResponse(location, forecast, forecastLatitude, forecastLongitude);
        WeatherQuery saved = saveQuery(mapped);
        mapped.setId(saved.getId());
        mapped.setConsultedAt(saved.getConsultedAt());
        log.info("Weather query saved id={} city={}", saved.getId(), saved.getCity());
        log.info("Returning weather result to client id={}", mapped.getId());
        return mapped;
    }

    public HistoryPageResponse listHistory(
            String city,
            String country,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100");
        }
        String cityFilter = hasText(city) ? city.trim() : null;
        String countryFilter = hasText(country) ? country.trim() : null;
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startDate cannot be after endDate");
        }
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        log.info("Listing weather history city={} country={} startDate={} endDate={} page={} size={}",
                cityFilter, countryFilter, startDate, endDate, page, size);

        Page<WeatherQuery> records = weatherQueryRepository.searchHistory(
                cityFilter, countryFilter, start, end, PageRequest.of(0, 5));

        List<WeatherQueryResponse> content = new ArrayList<WeatherQueryResponse>();
        for (WeatherQuery record : records.getContent()) {
            content.add(toResponse(record));
        }

        HistoryPageResponse response = new HistoryPageResponse();
        response.setContent(content);
        response.setPage(records.getNumber());
        response.setSize(records.getSize());
        response.setTotalElements(records.getTotalElements());
        response.setTotalPages(records.getTotalPages());
        response.setFirst(records.isFirst());
        response.setLast(records.isLast());
        return response;
    }

    public WeatherQueryResponse getHistory(Long id) {
        validateId(id);
        WeatherQuery record = weatherQueryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de histórico não encontrado: " + id));
        return toResponse(record);
    }

    public void deleteHistory(Long id) {
        validateId(id);
        if (!weatherQueryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de histórico não encontrado: " + id);
        }
        weatherQueryRepository.deleteById(id);
        log.info("\n"
        		+ "Registro de histórico meteorológico excluído id={}", id);
    }

    private void validateParameters(String city, Double latitude, Double longitude) {
        boolean hasCity = hasText(city);
        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;

        if (!hasCity && !hasLatitude && !hasLongitude) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Forneça a cidade ou as coordenadas (latitude and longitude)");
        }
        if (hasLatitude != hasLongitude) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A latitude e a longitude devem ser fornecidas juntas.");
        }
        if (hasLatitude) {
            if (latitude < -90 || latitude > 90) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            if (longitude < -180 || longitude > 180) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }
    }

    private GeocodingResult selectLocation(
            List<GeocodingResult> results,
            String searchTerm,
            Double latitude,
            Double longitude) {
        if (results == null || results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + searchTerm);
        }
        if (latitude != null && longitude != null) {
            return findClosest(results, latitude, longitude);
        }
        return results.get(0);
    }

    private void validateLocation(GeocodingResult location) {
        if (location == null
                || location.getName() == null
                || location.getLatitude() == null
                || location.getLongitude() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Selected location is incomplete");
        }
    }

    private GeocodingResult findClosest(List<GeocodingResult> results, double latitude, double longitude) {
        GeocodingResult closest = results.get(0);
        double closestDistance = distance(closest, latitude, longitude);
        for (int i = 1; i < results.size(); i++) {
            GeocodingResult current = results.get(i);
            double currentDistance = distance(current, latitude, longitude);
            if (currentDistance < closestDistance) {
                closest = current;
                closestDistance = currentDistance;
            }
        }
        return closest;
    }

    private double distance(GeocodingResult result, double latitude, double longitude) {
        if (result.getLatitude() == null || result.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        double dLat = result.getLatitude() - latitude;
        double dLon = result.getLongitude() - longitude;
        return dLat * dLat + dLon * dLon;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID deve ser um número existente");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private WeatherQueryResponse mapToResponse(
            GeocodingResult location,
            ForecastResponse forecast,
            double latitude,
            double longitude) {
        ForecastCurrent current = forecast.getCurrent();
        ForecastCurrentUnits units = forecast.getCurrentUnits();

        WeatherQueryResponse response = new WeatherQueryResponse();
        if (location != null) {
            response.setCity(location.getName());
            response.setCountry(location.getCountry());
            response.setCountryCode(location.getCountryCode());
            response.setRegion(location.getAdmin1());
        } else {
            response.setCity(cityFromTimezone(forecast.getTimezone()));
        }
        response.setLatitude(latitude);
        response.setLongitude(longitude);
        response.setTimezone(forecast.getTimezone() != null
                ? forecast.getTimezone()
                : (location != null ? location.getTimezone() : null));
        response.setTemperature(current.getTemperature2m());
        response.setApparentTemperature(current.getApparentTemperature());
        response.setHumidity(current.getRelativeHumidity2m());
        response.setWindSpeed(current.getWindSpeed10m());
        response.setWeatherCode(current.getWeatherCode());
        response.setWeatherTime(current.getTime());
        if (units != null) {
            response.setTemperatureUnit(units.getTemperature2m());
            response.setHumidityUnit(units.getRelativeHumidity2m());
            response.setWindSpeedUnit(units.getWindSpeed10m());
        }
        return response;
    }

    private WeatherQuery saveQuery(WeatherQueryResponse mapped) {
        WeatherQuery history = new WeatherQuery();
        history.setCity(mapped.getCity());
        history.setCountry(mapped.getCountry());
        history.setCountryCode(mapped.getCountryCode());
        history.setRegion(mapped.getRegion());
        history.setLatitude(mapped.getLatitude());
        history.setLongitude(mapped.getLongitude());
        history.setTimezone(mapped.getTimezone());
        history.setTemperature(mapped.getTemperature());
        history.setTemperatureUnit(mapped.getTemperatureUnit());
        history.setApparentTemperature(mapped.getApparentTemperature());
        history.setHumidity(mapped.getHumidity());
        history.setHumidityUnit(mapped.getHumidityUnit());
        history.setWindSpeed(mapped.getWindSpeed());
        history.setWindSpeedUnit(mapped.getWindSpeedUnit());
        history.setWeatherCode(mapped.getWeatherCode());
        history.setWeatherTime(mapped.getWeatherTime());
        history.setConsultedAt(LocalDateTime.now());
        return weatherQueryRepository.save(history);
    }

    private WeatherQueryResponse toResponse(WeatherQuery record) {
        WeatherQueryResponse response = new WeatherQueryResponse();
        response.setId(record.getId());
        response.setCity(record.getCity());
        response.setCountry(record.getCountry());
        response.setCountryCode(record.getCountryCode());
        response.setRegion(record.getRegion());
        response.setLatitude(record.getLatitude());
        response.setLongitude(record.getLongitude());
        response.setTimezone(record.getTimezone());
        response.setTemperature(record.getTemperature());
        response.setTemperatureUnit(record.getTemperatureUnit());
        response.setApparentTemperature(record.getApparentTemperature());
        response.setHumidity(record.getHumidity());
        response.setHumidityUnit(record.getHumidityUnit());
        response.setWindSpeed(record.getWindSpeed());
        response.setWindSpeedUnit(record.getWindSpeedUnit());
        response.setWeatherCode(record.getWeatherCode());
        response.setWeatherTime(record.getWeatherTime());
        response.setConsultedAt(record.getConsultedAt());
        return response;
    }

    private String cityFromTimezone(String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            return "Unknown";
        }
        int slash = timezone.lastIndexOf('/');
        if (slash >= 0 && slash < timezone.length() - 1) {
            return timezone.substring(slash + 1).replace('_', ' ');
        }
        return timezone;
    }
}
